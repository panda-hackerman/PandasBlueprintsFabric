package dev.michaud.pandas_blueprints.mixin;

import dev.michaud.pandas_blueprints.items.wrench.CopperWrenchItem;
import dev.michaud.pandas_blueprints.items.wrench.WrenchRotationUtil;
import dev.michaud.pandas_blueprints.sounds.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allow armor stands to be rotated by a wrench.
 * @see CopperWrenchItem#useOnBlock(ItemUsageContext)
 */
@Mixin(ArmorStandEntity.class)
public abstract class MixinArmorStandEntity extends LivingEntity {

  @Shadow
  public abstract ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand);

  protected MixinArmorStandEntity(EntityType<? extends LivingEntity> entityType, World world) {
    super(entityType, world);
  }

  @Inject(method = "interactAt", at = @At("RETURN"), cancellable = true)
  private void inject(PlayerEntity player, Vec3d hitPos, Hand hand,
      CallbackInfoReturnable<ActionResult> cir) {

    final ItemStack itemStack = player.getStackInHand(hand);
    final ArmorStandEntity thisArmorStand = (ArmorStandEntity) (Object) this;
    final ActionResult returnValue = cir.getReturnValue();

    if (thisArmorStand.isMarker()) {
      return;
    }

    if ((returnValue instanceof ActionResult.Pass || returnValue instanceof ActionResult.Fail)
        && itemStack.getItem() instanceof CopperWrenchItem) {

      final boolean shift = player.isSneaking();
      final World world = getWorld();
      final BlockPos pos = getBlockPos();

      // Rotate
      WrenchRotationUtil.rotateEntity(thisArmorStand, !shift);
      world.playSound(null, pos, ModSounds.COPPER_WRENCH_USE, SoundCategory.BLOCKS, 1f, 1f);

      // Damage Item
      final EquipmentSlot slot = LivingEntity.getSlotForHand(hand);
      itemStack.damage(1, player, slot);

      cir.setReturnValue(ActionResult.SUCCESS);
    }
  }
}