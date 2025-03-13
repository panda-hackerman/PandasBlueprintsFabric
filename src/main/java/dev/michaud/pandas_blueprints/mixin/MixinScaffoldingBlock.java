package dev.michaud.pandas_blueprints.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.michaud.pandas_blueprints.blocks.scaffolding.ScaffoldingBlockMaxDistanceHolder;
import dev.michaud.pandas_blueprints.tags.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScaffoldingBlock.class)
public abstract class MixinScaffoldingBlock extends Block implements Waterloggable,
    ScaffoldingBlockMaxDistanceHolder {

  public MixinScaffoldingBlock(Settings settings) {
    super(settings);
  }

  @Redirect(method = "<init>",
      at = @At(
          value = "INVOKE",
          ordinal = 0,
          target = "Lnet/minecraft/block/BlockState;with(Lnet/minecraft/state/property/Property;Ljava/lang/Comparable;)Ljava/lang/Object;"))
  private Object setDefaultStateRedirect(BlockState instance, Property<?> property,
      Comparable<?> comparable) {

    if (!property.getName().equals("distance")) {
      throw new IllegalStateException("Mixing in to the wrong property...");
    }

    return instance.with(getDistanceProperty(), getMaxDistance());
  }

  @Inject(method = "canReplace", at = @At("RETURN"), cancellable = true)
  private void canReplace(BlockState state, ItemPlacementContext context,
      CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(context.getStack().isIn(ModTags.SCAFFOLDING_ITEM));
    cir.cancel();
  }

  @Redirect(method = "getCollisionShape",
      at = @At(
          value = "INVOKE",
          ordinal = 0,
          target = "Lnet/minecraft/block/BlockState;get(Lnet/minecraft/state/property/Property;)Ljava/lang/Comparable;"))
  public Comparable<?> getCollisionShapeRedirect(BlockState instance, Property<?> property) {

    if (!property.getName().equals("distance")) {
      throw new IllegalStateException("Mixing in to the wrong property...");
    }

    return instance.get(getDistanceProperty());
  }

  @Redirect(method = "getPlacementState",
      at = @At(
          value = "INVOKE",
          ordinal = 1,
          target = "Lnet/minecraft/block/BlockState;with(Lnet/minecraft/state/property/Property;Ljava/lang/Comparable;)Ljava/lang/Object;"))
  private Object getPlacementState(BlockState instance, Property<?> property,
      Comparable<?> comparable, @Local int distance) {

    if (!property.getName().equals("distance")) {
      throw new IllegalStateException("Mixing in to the wrong property...");
    }

    return instance.with(getDistanceProperty(), distance);
  }

  @Redirect(method = "scheduledTick",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/block/BlockState;get(Lnet/minecraft/state/property/Property;)Ljava/lang/Comparable;"))
  private Comparable<?> scheduledTickGet(BlockState instance, Property<?> property) {
    if (property.getName().equals("distance")) {
      return instance.get(getDistanceProperty());
    }

    return instance.get(property);
  }

  @Redirect(method = "scheduledTick",
      at = @At(
          value = "INVOKE",
          ordinal = 0,
          target = "Lnet/minecraft/block/BlockState;with(Lnet/minecraft/state/property/Property;Ljava/lang/Comparable;)Ljava/lang/Object;"))
  private Object scheduledTickWith(BlockState instance, Property<?> property, Comparable<?> comparable, @Local int distance) {
    if (!property.getName().equals("distance")) {
      throw new IllegalStateException("Mixing in to the wrong property...");
    }

    return instance.with(getDistanceProperty(), distance);
  }

  @ModifyConstant(method = "scheduledTick", constant = @Constant(intValue = 7))
  private int scheduledTickMax(int constant) {
    return getMaxDistance();
  }

  @Inject(method = "shouldBeBottom", at = @At("HEAD"), cancellable = true)
  private void shouldBeBottom(BlockView world, BlockPos pos, int distance,
      CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(
        distance > 0 && !world.getBlockState(pos.down()).isIn(ModTags.SCAFFOLDING_BLOCK));
    cir.cancel();
  }

  @ModifyConstant(method = "canPlaceAt", constant = @Constant(intValue = 7))
  private int canPlaceAt(int constant) {
    return getMaxDistance();
  }
}