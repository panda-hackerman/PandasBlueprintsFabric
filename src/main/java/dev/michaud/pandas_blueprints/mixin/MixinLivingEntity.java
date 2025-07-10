package dev.michaud.pandas_blueprints.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.michaud.pandas_blueprints.tags.ModTags;
import net.minecraft.entity.Attackable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.minecraft.world.waypoint.ServerWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes it so that our custom scaffolding is treated as a climbable block just like real boy
 * scaffolding.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity implements Attackable, ServerWaypoint {

  public MixinLivingEntity(EntityType<?> type, World world) {
    super(type, world);
  }

  @Definition(id = "isOf", method = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z")
  @Definition(id = "SCAFFOLDING", field = "Lnet/minecraft/block/Blocks;SCAFFOLDING:Lnet/minecraft/block/Block;")
  @Definition(id = "getBlockStateAtPos", method = "Lnet/minecraft/entity/LivingEntity;getBlockStateAtPos()Lnet/minecraft/block/BlockState;")
  @Expression("this.getBlockStateAtPos().isOf(SCAFFOLDING)")
  @ModifyExpressionValue(method = "applyClimbingSpeed", at = @At("MIXINEXTRAS:EXPRESSION"))
  private boolean getIsBlockStateAtPosScaffolding(boolean original) {
    return original || getBlockStateAtPos().isIn(ModTags.SCAFFOLDING_BLOCK);
  }
}