package com.verse.spleef.mixin;

import com.verse.spleef.SpleefManager;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, remap = false)
public abstract class PlayerUseItemMixin {
	@Inject(method = "dropCurrentItem", at = @At("HEAD"), cancellable = true)
	private void btaspleef$keepRoundShovel(boolean dropStack, CallbackInfo ci) {
		Player player=(Player)(Object)this;
		ItemStack held=player.getCurrentEquippedItem();
		if (SpleefManager.isRoundParticipant(player) && held != null && held.getItem()==net.minecraft.core.item.Items.TOOL_SHOVEL_DIAMOND) ci.cancel();
	}

	@Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
	private void btaspleef$keepDroppedShovel(ItemStack stack, boolean randomThrow, CallbackInfo ci) {
		Player player=(Player)(Object)this;
		if (SpleefManager.isRoundParticipant(player) && stack != null && stack.getItem()==net.minecraft.core.item.Items.TOOL_SHOVEL_DIAMOND) ci.cancel();
	}
}
