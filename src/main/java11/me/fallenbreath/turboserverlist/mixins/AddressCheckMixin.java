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

import net.minecraft.client.multiplayer.resolver.AddressCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Collections;

@Mixin(AddressCheck.class)
public interface AddressCheckMixin
{
	@ModifyArg(
			method = "createFromService",
			at = @At(
					value = "INVOKE",
					target = "Lcom/google/common/collect/Streams;stream(Ljava/lang/Iterable;)Ljava/util/stream/Stream;",
					remap = false
			),
			remap = false
	)
	private static Iterable<?> nothingWasLoaded(Iterable<?> services)
	{
		return Collections.emptyList();
	}
}
