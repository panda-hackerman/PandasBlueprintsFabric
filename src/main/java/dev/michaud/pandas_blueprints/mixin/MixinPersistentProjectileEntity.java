package dev.michaud.pandas_blueprints.mixin;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class MixinPersistentProjectileEntity extends ProjectileEntity {

  public MixinPersistentProjectileEntity(
      EntityType<? extends ProjectileEntity> entityType,
      World world) {
    super(entityType, world);
  }

  @Inject(method = "onEntityHit",
      at = @At(
          value = "INVOKE",
          shift = Shift.AFTER,
          target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
  private void inject(EntityHitResult entityHitResult, CallbackInfo ci) {

    if (!(getWorld() instanceof ServerWorld world)) {
      return;
    }

    PandasBlueprints.LOGGER.info("Deflected!!!");

    world.getChunkManager().sendToOtherNearbyPlayers(this, new EntityVelocityUpdateS2CPacket(this));

  }

}