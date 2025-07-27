package dev.michaud.pandas_blueprints.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.scaffolding.ScaffoldingBlockDistanceHolder;
import dev.michaud.pandas_blueprints.tags.ModBlockTags;
import dev.michaud.pandas_blueprints.tags.ModItemTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScaffoldingBlock.class)
public abstract class MixinScaffoldingBlock extends Block implements Waterloggable,
    ScaffoldingBlockDistanceHolder {

  public MixinScaffoldingBlock(Settings settings) {
    super(settings);
  }

  // -- CONSTRUCTOR -- //
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

  // -- CAN REPLACE -- //
  @Inject(method = "canReplace", at = @At("RETURN"), cancellable = true)
  private void canReplace(BlockState state, ItemPlacementContext context,
      CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(context.getStack().isIn(ModItemTags.SCAFFOLDING));
    cir.cancel();
  }

  // -- OUTLINE SHAPE -- //
  @Redirect(method = "getOutlineShape", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;asItem()Lnet/minecraft/item/Item;"))
  private Item getOutlineShapeAsItemRedirect(Block instance) {
    return Items.SCAFFOLDING;
  }

  @WrapOperation(method = "getOutlineShape", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/ShapeContext;isHolding(Lnet/minecraft/item/Item;)Z"))
  private boolean getOutlineShapeIsHoldingRedirect(ShapeContext instance, Item item,
      Operation<Boolean> original) {

    if (instance instanceof EntityShapeContext context
        && context.getEntity() instanceof PlayerEntity player) {
      return player.getMainHandStack().isIn(ModItemTags.SCAFFOLDING);
    }

    return original.call(instance, item);
  }

  // -- COLLISION SHAPE -- //
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

  // -- GET PLACEMENT STATE -- //
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

  // -- SHOULD BE BOTTOM -- //
  @Inject(method = "shouldBeBottom", at = @At("HEAD"), cancellable = true)
  private void shouldBeBottom(BlockView world, BlockPos pos, int distance,
      CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(
        distance > 0 && !world.getBlockState(pos.down()).isIn(ModBlockTags.SCAFFOLDING));
    cir.cancel();
  }

  // -- CAN PLACE AT -- //
  @ModifyConstant(method = "canPlaceAt", constant = @Constant(intValue = 7))
  private int canPlaceAt(int constant) {
    return getMaxDistance();
  }

  // -- CALCULATE DISTANCE CALLS -- //

  /**
   * @author NrdyPnda
   * @reason We need to replace this method with
   * {@link ScaffoldingBlockDistanceHolder#calculateScaffoldingDistance(BlockView, BlockPos)}.
   * However, that method relies on the max distance and distance properties in
   * {@link ScaffoldingBlockDistanceHolder}, which is specific to the class, which we can't access
   * here because this method is static. So instead, we just redirect whenever the method is called.
   * As a backup, we can default to calling the custom method with the max distance and distance
   * property in the normal {@link ScaffoldingBlock} class.
   */
  @Overwrite
  public static int calculateDistance(BlockView world, BlockPos pos) {
    PandasBlueprints.LOGGER.warn(
        "Didn't successfully redirect \"Lnet/minecraft/block/ScaffoldingBlock;calculateDistance"
            + "(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)I\"!! "
            + "This is probably due to a new Minecraft update, or another mod. Try updating to the "
            + "latest Panda's Blueprints version: "
            + "https://github.com/panda-hackerman/PandasBlueprintsFabric");
    return ((ScaffoldingBlockDistanceHolder) Blocks.SCAFFOLDING).calculateScaffoldingDistance(world,
        pos);
  }

  @Redirect(method = "getPlacementState",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/block/ScaffoldingBlock;calculateDistance(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)I"))
  private int redirectGetPlacementState(BlockView world, BlockPos pos) {
    return calculateScaffoldingDistance(world, pos);
  }

  @Redirect(method = "scheduledTick",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/block/ScaffoldingBlock;calculateDistance(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)I"))
  private int redirectScheduledTick(BlockView world, BlockPos pos) {
    return calculateScaffoldingDistance(world, pos);
  }

  @Redirect(method = "canPlaceAt",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/block/ScaffoldingBlock;calculateDistance(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)I"))
  private int redirectCanPlaceAt(BlockView world, BlockPos pos) {
    return calculateScaffoldingDistance(world, pos);
  }

}