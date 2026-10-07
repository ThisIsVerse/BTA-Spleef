package com.verse.spleef.mixin;

import com.verse.spleef.SpleefManager;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityLightning;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityLightning.class, remap = false)
public abstract class EntityLightningMixin {
	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/world/World;setBlockWithNotify(IIII)Z"))
	private boolean btaspleef$suppressCosmeticFire(World world, int x, int y, int z, int id) {
		if (SpleefManager.isCosmeticLightning((EntityLightning)(Object)this)) return false;
		return world.setBlockWithNotify(x, y, z, id);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/entity/Entity;thunderHit(Lnet/minecraft/core/entity/EntityLightning;)V"))
	private void btaspleef$suppressCosmeticDamage(Entity target, EntityLightning bolt) {
		if (!SpleefManager.isCosmeticLightning(bolt)) target.thunderHit(bolt);
	}
}
