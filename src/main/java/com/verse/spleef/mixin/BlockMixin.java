package com.verse.spleef.mixin;

import com.verse.spleef.SpleefManager;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePosc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Block.class, remap = false)
public abstract class BlockMixin {
	@Inject(method = "dropWithCause", at = @At("HEAD"), cancellable = true)
	private void btaspleef$suppressRoundDrop(World world, EnumDropCause cause, TilePosc pos, int data, TileEntity tileEntity, Player player, CallbackInfo ci) {
		if (SpleefManager.isActiveRoundParticipant(player)) ci.cancel();
	}

	@Inject(method = "onHarvest", at = @At("HEAD"), cancellable = true)
	private void btaspleef$suppressHarvestDrop(World world, Player player, TilePosc pos, int data, TileEntity tileEntity, CallbackInfo ci) {
		if (SpleefManager.isActiveRoundParticipant(player)) ci.cancel();
	}

	@Inject(method = "onDestroyedByExplosion", at = @At("HEAD"), cancellable = true)
	private void btaspleef$protectArenaFromExplosions(World world, TilePosc pos, CallbackInfo ci) {
		if (SpleefManager.isInsideAnyArena(world, pos.x(), pos.y(), pos.z())) ci.cancel();
	}
}
