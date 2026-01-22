/*
 * This file is part of the Turbo Server List project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
 *
 * Turbo Server List is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Turbo Server List is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Turbo Server List.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.fallenbreath.turboserverlist;

import com.google.common.collect.Lists;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class TurboServerListMixinPlugin implements IMixinConfigPlugin
{
	public static final Logger LOGGER = LogManager.getLogger();

	private ModifyMode modifyMode = ModifyMode.UNSET;

	@Override
	public void onLoad(String mixinPackage)
	{
		ModeChecker modeChecker;
		if (classExists("net.fabricmc.loader.api.FabricLoader"))
		{
			modeChecker = new FabricModeChecker();
		}
		else if (classExists("net.minecraftforge.fml.loading.FMLLoader"))
		{
			modeChecker = new ForgeModeChecker();
		}
		else if (classExists("net.neoforged.fml.loading.FMLLoader"))
		{
			modeChecker = new NeoForgeModeChecker();
		}
		else
		{
			throw new RuntimeException("Failed to detect current mod loader");
		}
		this.modifyMode = modeChecker.check();
	}

	@Override
	public String getRefMapperConfig()
	{
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		if (mixinClassName.endsWith(".MojangBlockListSupplierMixin"))
		{
			return this.modifyMode == ModifyMode.SUPPLIER_SERVICE;
		}
		if (mixinClassName.endsWith(".BlockedServersMixin"))
		{
			return this.modifyMode == ModifyMode.BLOCKED_SERVERS_CLINIT;
		}
		LOGGER.warn("Unexpected Mixin class {}", mixinClassName);
		return false;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
	{
	}

	@Override
	public List<String> getMixins()
	{
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
	{
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
	{
	}

	private static boolean classExists(String className)
	{
		try
		{
			Class.forName(className);
			return true;
		}
		catch (ClassNotFoundException e)
		{
			return false;
		}
	}

	private enum ModifyMode
	{
		UNSET, BLOCKED_SERVERS_CLINIT, SUPPLIER_SERVICE
	}

	private interface ModeChecker
	{
		ModifyMode check();
	}

	private static class FabricModeChecker implements ModeChecker
	{
		@Override
		public ModifyMode check()
		{
			ModContainer mc = FabricLoader.getInstance().getModContainer("minecraft").
					orElseThrow(() -> new RuntimeException("Failed to get minecraft mod container"));
			Version mcVersion = mc.getMetadata().getVersion();
			ModifyMode modifyMode = doesVersionSatisfyPredicateImpl(mcVersion, ">=1.17.0-alpha.21.19.a")
					? ModifyMode.SUPPLIER_SERVICE
					: ModifyMode.BLOCKED_SERVERS_CLINIT;
			LOGGER.info("Turbo Server List: fabric, mc version: {}, modify mode: {}", mcVersion.getFriendlyString(), modifyMode);
			return modifyMode;
		}

		/**
		 * From <a href="https://github.com/Fallen-Breath/conditional-mixin/blob/d3c76bb3d37b05fd2a648ad58ebe4dd7f12a7932/fabric/src/main/java/me/fallenbreath/conditionalmixin/api/util/fabric/VersionCheckerImpl.java#L23C1-L49C3">conditional-mixin</a>
		 */
		private static boolean doesVersionSatisfyPredicateImpl(Version version, String versionPredicate)
		{
			try
			{
				// fabric loader >=0.12
				return net.fabricmc.loader.api.metadata.version.VersionPredicate.parse(versionPredicate).test(version);
			}
			catch (NoClassDefFoundError e)
			{
				// fabric loader >=0.10.4 <0.12
				try
				{
					Class<?> clazz = Class.forName("net.fabricmc.loader.util.version.VersionPredicateParser");
					Method matches = clazz.getMethod("matches", Version.class, String.class);
					return (boolean)matches.invoke(null, version, versionPredicate);
				}
				catch (Exception ex)
				{
					LOGGER.error("Failed to invoke VersionPredicateParser#matches", ex);
				}
			}
			catch (Exception e)
			{
				LOGGER.error("Failed to parse version or version predicate {} {}: {}", version.getFriendlyString(), versionPredicate, e);
			}
			return false;
		}
	}

	private static abstract class ForgeLikeModeChecker implements ModeChecker
	{
		@FunctionalInterface
		protected interface McVersionSupplier
		{
			String tryGet() throws Exception;
		}

		protected abstract List<McVersionSupplier> createMcVersionSuppliers();
		protected abstract String platformName();

		@Override
		public ModifyMode check()
		{
			String mcVersionString = null;
			StringBuilder exceptionMsgs = new StringBuilder();
			for (McVersionSupplier supplier : createMcVersionSuppliers())
			{
				try
				{
					mcVersionString = supplier.tryGet();
				}
				catch (Exception e)
				{
					if (exceptionMsgs.length() > 0)
					{
						exceptionMsgs.append("; ");
					}
					exceptionMsgs.append(e);
				}
			}
			if (mcVersionString == null)
			{
				throw new RuntimeException("Failed to get mc version: " + exceptionMsgs);
			}
			try
			{
				ArtifactVersion mcVersion = new DefaultArtifactVersion(mcVersionString);
				ModifyMode modifyMode = VersionRange.createFromVersionSpec("[1.17, )").containsVersion(mcVersion)
						? ModifyMode.SUPPLIER_SERVICE
						: ModifyMode.BLOCKED_SERVERS_CLINIT;
				LOGGER.info("Turbo Server List: {}, mc version: {}, modify mode: {}", this.platformName(), mcVersion, modifyMode);
				return modifyMode;
			}
			catch (InvalidVersionSpecificationException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	private static class ForgeModeChecker extends ForgeLikeModeChecker
	{
		@Override
		protected String platformName()
		{
			return "forge";
		}

		@Override
		protected List<McVersionSupplier> createMcVersionSuppliers()
		{
			List<McVersionSupplier> suppliers = Lists.newArrayList();
			suppliers.add(() -> {
				// FMLLoader#mcVersion
				Class<?> clazz = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
				Field field = clazz.getField("mcVersion");
				field.setAccessible(true);
				return (String)field.get(null);
			});
			suppliers.add(() -> {
				// FMLLoader.versionInfo().mcVersion()
				Class<?> clazz = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
				Object versionInfo = clazz.getMethod("versionInfo").invoke(null);
				return (String)versionInfo.getClass().getMethod("mcVersion").invoke(versionInfo);
			});
			suppliers.add(() -> {
				// FMLLoader.getCurrent().getVersionInfo().mcVersion()
				Class<?> clazz = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
				Object loader = clazz.getMethod("getCurrent").invoke(null);
				Object versionInfo = clazz.getMethod("getVersionInfo").invoke(loader);
				return (String)versionInfo.getClass().getMethod("mcVersion").invoke(versionInfo);
			});
			suppliers.add(() -> {
				// MCPVersion.getMCVersion()
				Class<?> clazz = Class.forName("net.minecraftforge.versions.mcp.MCPVersion");
				return (String)clazz.getMethod("getMCVersion").invoke(null);
			});
			return suppliers;
		}
	}

	private static class NeoForgeModeChecker extends ForgeLikeModeChecker
	{
		@Override
		protected String platformName()
		{
			return "neoforge";
		}

		@Override
		protected List<McVersionSupplier> createMcVersionSuppliers()
		{
			List<McVersionSupplier> suppliers = Lists.newArrayList();
			suppliers.add(() -> {
				// FMLLoader.versionInfo().mcVersion()
				Class<?> clazz = Class.forName("net.neoforged.fml.loading.FMLLoader");
				Object versionInfo = clazz.getMethod("versionInfo").invoke(null);
				return (String)versionInfo.getClass().getMethod("mcVersion").invoke(versionInfo);
			});
			suppliers.add(() -> {
				// FMLLoader.getCurrent().getVersionInfo().mcVersion()
				Class<?> clazz = Class.forName("net.neoforged.fml.loading.FMLLoader");
				Object loader = clazz.getMethod("getCurrent").invoke(null);
				Object versionInfo = clazz.getMethod("getVersionInfo").invoke(loader);
				return (String)versionInfo.getClass().getMethod("mcVersion").invoke(versionInfo);
			});
			return suppliers;
		}
	}
}
