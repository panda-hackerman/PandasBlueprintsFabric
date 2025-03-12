package dev.michaud.pandas_blueprints.mixin;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.ScaffoldingBlockMaxDistanceHolder;
import dev.michaud.pandas_blueprints.tags.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScaffoldingBlock.class)
public abstract class MixinScaffoldingBlock extends Block implements Waterloggable,
    ScaffoldingBlockMaxDistanceHolder {

  @Shadow
  public static @Final BooleanProperty WATERLOGGED;

  @Shadow
  public static @Final BooleanProperty BOTTOM;

  public MixinScaffoldingBlock(Settings settings) {
    super(settings);
  }

  @Shadow
  protected abstract boolean shouldBeBottom(BlockView world, BlockPos pos, int distance);

  @Shadow
  public static int calculateDistance(BlockView world, BlockPos pos) {
    final BlockPos downOnePos = pos.down();
    final BlockState downOneState = world.getBlockState(downOnePos);

    if (!(downOneState.getBlock() instanceof ScaffoldingBlockMaxDistanceHolder scaffolding)) {
      return -1;
    }

    int distance = scaffolding.getMaxDistance();

    if (downOneState.getBlock() instanceof ScaffoldingBlockMaxDistanceHolder downScaffolding) {
      distance = downOneState.get(downScaffolding.getDistanceProperty());
    } else if (downOneState.isSideSolidFullSquare(world, downOnePos, Direction.UP)) {
      return 0;
    }

    for (final Direction direction : Direction.Type.HORIZONTAL) {
      final BlockPos nextToPos = pos.offset(direction);
      final BlockState nextToState = world.getBlockState(nextToPos);

      if (nextToState.getBlock() instanceof ScaffoldingBlockMaxDistanceHolder nextToScaffolding) {
        final int nextToDistance = nextToState.get(nextToScaffolding.getDistanceProperty());
        distance = Math.min(distance, nextToDistance + 1);
        if (distance == 1) {
          break; // Min distance already achieved !
        }
      }
    }

    return distance;
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

  @Override
  public boolean canReplace(BlockState state, ItemPlacementContext context) {
    return context.getStack().isIn(ModTags.SCAFFOLDING_ITEM);
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

  @Override
  public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
    BlockPos blockPos = context.getBlockPos();
    World world = context.getWorld();

    int distance = calculateDistance(world, blockPos);

    return getDefaultState()
        .with(WATERLOGGED, world.getFluidState(blockPos).getFluid() == Fluids.WATER)
        .with(getDistanceProperty(), distance)
        .with(BOTTOM, shouldBeBottom(world, blockPos, distance));
  }

  @Override
  public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    final int distance = calculateDistance(world, pos);
    final BlockState newState = state.with(getDistanceProperty(), distance)
        .with(BOTTOM, shouldBeBottom(world, pos, distance));

    if (distance >= getMaxDistance()) {
      if (state.get(getDistanceProperty()) == getMaxDistance()) {
        FallingBlockEntity.spawnFromBlock(world, pos, newState);
      } else {
        world.breakBlock(pos, true);
      }
    } else if (state != newState) {
      world.setBlockState(pos, newState, Block.NOTIFY_ALL);
    }
  }

  @Inject(method = "shouldBeBottom", at = @At("HEAD"), cancellable = true)
  private void shouldBeBottom(BlockView world, BlockPos pos, int distance,
      CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(
        distance > 0 && !world.getBlockState(pos.down()).isIn(ModTags.SCAFFOLDING_BLOCK));
    cir.cancel();
  }

}
