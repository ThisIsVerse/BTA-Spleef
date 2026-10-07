package com.verse.spleef.mixin;

import com.verse.spleef.SpleefManager;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerList.class, remap = false)
public abstract class PlayerDisconnectMixin {
	@Inject(method = "playerLoggedOut", at = @At("HEAD"))
	private void btaspleef$onDisconnect(PlayerServer player, CallbackInfo ci) {
		SpleefManager.onPlayerDisconnect(player);
	}
}
