package com.verse.spleef.mixin;

import com.verse.spleef.SpleefManager;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.server.world.ServerPlayerController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayerController.class, remap = false)
public abstract class BlockBreakMixin {
	@Shadow private Player player;

	@Inject(method = "startMining", at = @At("HEAD"))
	private void btaspleef$onBlockHit(int x, int y, int z, Side side, CallbackInfo ci) {
		SpleefManager.onBlockHit(player, new net.minecraft.core.world.pos.TilePos(x, y, z));
	}

	@Inject(method = "mineBlock", at = @At("HEAD"), cancellable = true)
	private void btaspleef$protectArena(int x, int y, int z, Side side, CallbackInfoReturnable<Boolean> cir) {
		if (SpleefManager.preventArenaBlockBreak(player, x, y, z)) cir.setReturnValue(false);
	}

	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void btaspleef$protectArenaOnDestroy(int x, int y, int z, Side side, CallbackInfoReturnable<Boolean> cir) {
		if (SpleefManager.preventArenaBlockBreak(player, x, y, z)) cir.setReturnValue(false);
	}

	@Inject(method = "useOrPlaceItemStackOnTile", at = @At("HEAD"), cancellable = true)
	private void btaspleef$blockPlacementDuringRound(Player placer, World world, ItemStack stack, int x, int y, int z, Side side, double hitX, double hitY, CallbackInfoReturnable<Boolean> cir) {
		if (SpleefManager.isActiveRoundParticipant(placer)) cir.setReturnValue(false);
	}

	@Inject(method = "placeItemStackOnTile", at = @At("HEAD"), cancellable = true)
	private void btaspleef$blockPlacementDuringRoundCarried(Player placer, World world, ItemStack stack, int x, int y, int z, Side side, double hitX, double hitY, CallbackInfoReturnable<Boolean> cir) {
		if (SpleefManager.isActiveRoundParticipant(placer)) cir.setReturnValue(false);
	}
}
