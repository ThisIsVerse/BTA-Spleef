package com.verse.spleef.mixin;

import com.verse.spleef.SpleefManager;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, remap = false)
public abstract class PlayerDamageMixin {
	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void btaspleef$disableRoundPvp(Entity attacker, int damage, DamageType type, CallbackInfoReturnable<Boolean> cir) {
		Player victim = (Player)(Object)this;
		if (attacker instanceof Player && SpleefManager.sameActiveRound(victim, (Player)attacker)) cir.setReturnValue(false);
	}
}
