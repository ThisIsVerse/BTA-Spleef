package com.verse.spleef.mixin;

import com.verse.spleef.SpleefManager;
import net.minecraft.core.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, remap = false)
public abstract class PlayerTickMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void btaspleef$checkFail(CallbackInfo ci) { SpleefManager.onPlayerTick((Player)(Object)this); }
}
