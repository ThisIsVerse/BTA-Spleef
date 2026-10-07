package com.verse.spleef.mixin;

import com.verse.spleef.SpleefManager;
import net.minecraft.core.block.Block;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePosc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Block.class, remap = false)
public abstract class BlockStrengthMixin {
	@Inject(method = "getStrength", at = @At("HEAD"), cancellable = true)
	private void btaspleef$instamineStrength(World world, TilePosc tilePos, Side side, Player player, CallbackInfoReturnable<Float> cir) {
		if (SpleefManager.isInstamineBlock(player, tilePos.x(), tilePos.y(), tilePos.z())) cir.setReturnValue(100f);
	}
}
