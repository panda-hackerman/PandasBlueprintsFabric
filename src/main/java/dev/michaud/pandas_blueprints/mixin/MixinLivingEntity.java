package dev.michaud.pandas_blueprints.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import dev.michaud.pandas_blueprints.components.BlocksOverheadComponent;
import dev.michaud.pandas_blueprints.components.ModComponentTypes;
import dev.michaud.pandas_blueprints.tags.ModTags;
import net.minecraft.entity.Attackable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.waypoint.ServerWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity implements Attackable, ServerWaypoint {

  public MixinLivingEntity(EntityType<?> type, World world) {
    super(type, world);
  }

  /**
   * Makes it so that our custom scaffolding is treated as a climbable block just like real boy
   * scaffolding.
   */
  @Definition(id = "isOf", method = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z")
  @Definition(id = "SCAFFOLDING", field = "Lnet/minecraft/block/Blocks;SCAFFOLDING:Lnet/minecraft/block/Block;")
  @Definition(id = "getBlockStateAtPos", method = "Lnet/minecraft/entity/LivingEntity;getBlockStateAtPos()Lnet/minecraft/block/BlockState;")
  @Expression("this.getBlockStateAtPos().isOf(SCAFFOLDING)")
  @ModifyExpressionValue(method = "applyClimbingSpeed", at = @At("MIXINEXTRAS:EXPRESSION"))
  private boolean getIsBlockStateAtPosScaffolding(boolean original) {
    return original || getBlockStateAtPos().isIn(ModTags.SCAFFOLDING_BLOCK);
  }

  /**
   * Blocks damage from items with a block overhead component
   *
   * @see BlocksOverheadComponent
   */
  @Definition(id = "amount", local = @Local(type = float.class, argsOnly = true))
  @Expression("amount = @(amount - ?)")
  @ModifyExpressionValue(method = "damage", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
  private float subtractBlockedFromAboveAmount(float amount,
      @Local(argsOnly = true) ServerWorld world,
      @Local(argsOnly = true) DamageSource source,
      @Share("blockedAbove") LocalBooleanRef blockedAboveRef) {

    final LivingEntity thisEntity = (LivingEntity) (Object) this;
    final float blocked = BlocksOverheadComponent.getDamageBlockedAmount(thisEntity, world, source,
        amount);

    blockedAboveRef.set(blocked > 0);

    return amount - blocked;
  }

//  @ModifyVariable(method = "damage", at = @At("STORE"), ordinal = 2)
//  private boolean modifyWasntBlockedBool(boolean original, @Local(ordinal = 0) boolean bl, @Local(argsOnly = true) float amount, @Share("blockedAbove") LocalBooleanRef blockedAboveRef) {
//    return !(bl || blockedAboveRef.get()) || amount > 0f;
//  }

  @Definition(id = "getActiveItem", method = "Lnet/minecraft/entity/LivingEntity;getActiveItem()Lnet/minecraft/item/ItemStack;")
  @Definition(id = "BLOCKS_ATTACKS", field = "Lnet/minecraft/component/DataComponentTypes;BLOCKS_ATTACKS:Lnet/minecraft/component/ComponentType;")
  @Definition(id = "get", method = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;")
  @Expression("this.getActiveItem().get(BLOCKS_ATTACKS)")
  @Inject(method = "damage", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
  private void modifyBlockedBool(ServerWorld world, DamageSource source, float amount,
      CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) LocalBooleanRef bl,
      @Share("blockedAbove") LocalBooleanRef blockedAboveRef) {
    bl.set(bl.get() || blockedAboveRef.get());
  }

  /// Play the block sound if the attack was blocked by {@link BlocksOverheadComponent}.
  @WrapWithCondition(method = "damage",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/server/world/ServerWorld;sendEntityDamage(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;)V"))
  private boolean wrapWorldSendEntityDamage(ServerWorld world, Entity entity,
      DamageSource damageSource, @Share("blockedAbove") LocalBooleanRef blockedAboveRef) {

    final LivingEntity thisEntity = (LivingEntity) (Object) this;
    final ItemStack headItem = thisEntity.getEquippedStack(EquipmentSlot.HEAD);
    final BlocksOverheadComponent component = headItem.get(
        ModComponentTypes.BLOCKS_OVERHEAD_ATTACKS);

    if (blockedAboveRef.get() && component != null) {
      component.playBlockSound(world, thisEntity);
      return false; // Don't send damage
    }

    return true; // Do send damage
  }

}