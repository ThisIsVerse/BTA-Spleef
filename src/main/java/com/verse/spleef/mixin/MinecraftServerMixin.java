package com.verse.spleef.mixin;

import com.verse.spleef.SpleefCommands;
import com.verse.spleef.SpleefManager;
import net.minecraft.core.net.command.CommandManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftServer.class, remap = false)
public abstract class MinecraftServerMixin {
	@Inject(method = "initCommands", at = @At("TAIL"))
	private void btaspleef$commands(CallbackInfo ci) {
		CommandManager.registerServerCommand(new SpleefCommands());
	}

	@Inject(method = "run", at = @At("HEAD"))
	private void btaspleef$serverStart(CallbackInfo ci) {
		SpleefManager.serverStart((MinecraftServer)(Object)this);
	}

	@Inject(method = "doTick", at = @At("TAIL"))
	private void btaspleef$tick(CallbackInfo ci) {
		SpleefManager.onServerTick((MinecraftServer)(Object)this);
	}
}
