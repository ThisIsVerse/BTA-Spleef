package com.verse.spleef.mixin;

import com.verse.spleef.SpleefManager;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
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
public abstract class SnowDropMixin {
	@Inject(method = "dropWithCause", at = @At("HEAD"), cancellable = true)
	private void btaspleef$suppressSnowballDrop(World world, EnumDropCause cause, TilePosc pos, int data, TileEntity tileEntity, Player player, CallbackInfo ci) {
		if ((Block<?>)(Object)this == Blocks.LAYER_SNOW && SpleefManager.isActiveRoundParticipant(player)) ci.cancel();
	}
}
