package com.verse.spleef.mixin;

import com.verse.spleef.SpleefManager;
import net.minecraft.core.enums.MobCategory;
import net.minecraft.core.world.World;
import net.minecraft.core.world.SpawnerMobs;
import net.minecraft.core.world.pos.TilePosc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpawnerMobs.class, remap = false)
public abstract class MobSpawnMixin {
	@Inject(method = "canCreatureTypeSpawnAtLocation", at = @At("HEAD"), cancellable = true)
	private static void btaspleef$blockArenaSpawns(MobCategory mobCategory, World world, TilePosc tilePos, CallbackInfoReturnable<Boolean> cir) {
		if (SpleefManager.isInsideAnyArena(world, tilePos.x(), tilePos.y(), tilePos.z())) cir.setReturnValue(false);
	}
}
