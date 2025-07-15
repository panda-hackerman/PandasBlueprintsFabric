package dev.michaud.pandas_blueprints.blocks.scaffolding;

import dev.michaud.pandas_blueprints.blocks.BlockWithCustomSounds;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class OxidizableScaffoldingBlock extends ScaffoldingBlock implements PolymerTexturedBlock,
    Oxidizable, Waterloggable, ScaffoldingBlockDistanceHolder, BlockWithCustomSounds {

  public static final int MAX_DISTANCE = 16;
  public static final IntProperty DISTANCE = IntProperty.of("distance", 0, MAX_DISTANCE);
  final OxidationLevel oxidationLevel;

  public OxidizableScaffoldingBlock(OxidationLevel oxidationLevel, Settings settings) {
    super(settings);
    this.oxidationLevel = oxidationLevel;

    setDefaultState(getDefaultState()
        .with(getDistanceProperty(), getMaxDistance())
        .with(WATERLOGGED, false)
        .with(BOTTOM, false));
  }

  @Override
  protected void appendProperties(Builder<Block, BlockState> builder) {
    builder.add(getDistanceProperty(), WATERLOGGED, BOTTOM);
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {

    final int distance = calculateScaffoldingDistance(world, pos);
    final BlockState blockState = state
        .with(getDistanceProperty(), distance)
        .with(BOTTOM, shouldBeBottom(world, pos, distance));

    if (blockState.get(getDistanceProperty()) >= getFallDistance()) {
      var e = FallingBlockEntity.spawnFromBlock(world, pos, blockState);
      e.dropItem = true;
    } else if (state != blockState) {
      world.setBlockState(pos, blockState, Block.NOTIFY_ALL);
    }
  }

  @Override
  protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    tickDegradation(state, world, pos, random);
  }

  @Override
  protected boolean hasRandomTicks(BlockState state) {
    return Oxidizable.getIncreasedOxidationBlock(state.getBlock()).isPresent();
  }

  @Override
  public OxidationLevel getDegradationLevel() {
    return oxidationLevel;
  }

  @Override
  public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
    return OxidizableScaffoldingBlockModels.getPolymerBlockState(oxidationLevel, state.get(BOTTOM),
        state.get(WATERLOGGED));
  }

  @Override
  public IntProperty getDistanceProperty() {
    return DISTANCE;
  }

  @Override
  public int getMaxDistance() {
    return MAX_DISTANCE;
  }

  @Override
  public int getFallDistance() {
    return switch (oxidationLevel) {
      case UNAFFECTED -> MAX_DISTANCE;
      case EXPOSED -> MAX_DISTANCE - 4;
      case WEATHERED -> MAX_DISTANCE - 8;
      case OXIDIZED -> MAX_DISTANCE - 12;
    };
  }

  @Override
  public Set<BlockState> getAllClientBlockStates() {
    return OxidizableScaffoldingBlockModels.ALL_STATES;
  }

  public static class OxidizableScaffoldingBlockItem extends PolymerBlockItem {

    public OxidizableScaffoldingBlockItem(Block block, Settings settings) {
      super(block, settings, Items.SCAFFOLDING);
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
      if (PolymerResourcePackUtils.hasMainPack(context)) {
        return OxidizableScaffoldingBlockModels.getPolymerItemModel(getBlock());
      } else {
        return Identifier.ofVanilla("scaffolding");
      }
    }

    @Override
    public @Nullable ItemPlacementContext getPlacementContext(ItemPlacementContext context) {
      return ScaffoldingItemPlacementContextUtil.getItemPlacementContext(context,
          (ScaffoldingBlockDistanceHolder) getBlock());
    }

    @Override
    protected boolean checkStatePlacement() {
      return false;
    }
  }

}