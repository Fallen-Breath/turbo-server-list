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

package me.fallenbreath.turboserverlist.mixins;

import com.mojang.patchy.BlockedServers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@SuppressWarnings("MixinAnnotationTarget")
@Mixin(BlockedServers.class)
public abstract class BlockedServersMixin
{
	@Inject(
			method = "<clinit>",
			at = @At(
					value = "INVOKE",
					target = "Ljava/net/URL;openConnection()Ljava/net/URLConnection;",
					remap = false
			),
			remap = false
	)
	private static void dontEvenThinkAboutIt(CallbackInfo ci) throws IOException
	{
		throw new IOException("nope");
	}
}
