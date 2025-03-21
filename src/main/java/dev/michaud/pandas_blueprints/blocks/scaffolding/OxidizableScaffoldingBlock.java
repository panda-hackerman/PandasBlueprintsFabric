package dev.michaud.pandas_blueprints.blocks.scaffolding;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class OxidizableScaffoldingBlock extends ScaffoldingBlock implements PolymerTexturedBlock,
    Oxidizable, ScaffoldingBlockMaxDistanceHolder {

  public static final int MAX_DISTANCE = 16;
  public static final IntProperty DISTANCE = IntProperty.of("distance", 0, MAX_DISTANCE);

  final OxidationLevel oxidationLevel;

  final PolymerBlockModel BLOCK_MODEL_TOP;
  final PolymerBlockModel BLOCK_MODEL_BOTTOM;

  public final BlockState POLYMER_BLOCK_STATE_TOP;
  public final BlockState POLYMER_BLOCK_STATE_BOTTOM;
  public final BlockState POLYMER_BLOCK_STATE_TOP_WATERLOGGED;
  public final BlockState POLYMER_BLOCK_STATE_BOTTOM_WATERLOGGED;

  public OxidizableScaffoldingBlock(OxidationLevel oxidationLevel, Settings settings) {
    super(settings);
    this.oxidationLevel = oxidationLevel;

    setDefaultState(getDefaultState()
        .with(DISTANCE, getMaxDistance())
        .with(WATERLOGGED, false)
        .with(BOTTOM, false));

    final String blockModelPath = "block/" + modelFromOxidation(oxidationLevel);

    BLOCK_MODEL_TOP = PolymerBlockModel.of(
        Identifier.of(PandasBlueprints.GREENPANDA_ID, blockModelPath + "_stable")
    );
    BLOCK_MODEL_BOTTOM = PolymerBlockModel.of(
        Identifier.of(PandasBlueprints.GREENPANDA_ID, blockModelPath + "_unstable")
    );

    POLYMER_BLOCK_STATE_TOP = PolymerBlockResourceUtils.requestBlock(
        BlockModelType.TOP_SCAFFOLDING, BLOCK_MODEL_TOP);
    POLYMER_BLOCK_STATE_BOTTOM = PolymerBlockResourceUtils.requestBlock(
        BlockModelType.BOTTOM_SCAFFOLDING, BLOCK_MODEL_BOTTOM);
    POLYMER_BLOCK_STATE_TOP_WATERLOGGED = PolymerBlockResourceUtils.requestBlock(
        BlockModelType.TOP_SCAFFOLDING_WATERLOGGED, BLOCK_MODEL_TOP);
    POLYMER_BLOCK_STATE_BOTTOM_WATERLOGGED = PolymerBlockResourceUtils.requestBlock(
        BlockModelType.BOTTOM_SCAFFOLDING_WATERLOGGED, BLOCK_MODEL_BOTTOM);
  }

  @Override
  protected void appendProperties(Builder<Block, BlockState> builder) {
    builder.add(DISTANCE, WATERLOGGED, BOTTOM);
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    super.scheduledTick(state, world, pos, random);
  }

  @Override
  public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
    if (state.get(WATERLOGGED)) {
      return state.get(BOTTOM) ? POLYMER_BLOCK_STATE_BOTTOM_WATERLOGGED
          : POLYMER_BLOCK_STATE_TOP_WATERLOGGED;
    } else {
      return state.get(BOTTOM) ? POLYMER_BLOCK_STATE_BOTTOM : POLYMER_BLOCK_STATE_TOP;
    }
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
  public OxidationLevel getDegradationLevel() {
    return oxidationLevel;
  }

  public static String modelFromOxidation(OxidationLevel level) {
    if (level == OxidationLevel.UNAFFECTED) {
      return "copper_scaffolding";
    } else {
      return level.asString() + "_copper_scaffolding";
    }
  }

  public static class OxidizableScaffoldingBlockItem extends PolymerBlockItem {

    final String itemModelName;

    public OxidizableScaffoldingBlockItem(Block block, Settings settings) {
      super(block, settings);

      OxidationLevel level = ((Oxidizable) block).getDegradationLevel();
      itemModelName = modelFromOxidation(level);
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
      if (PolymerResourcePackUtils.hasMainPack(context)) {
        return Identifier.of(PandasBlueprints.GREENPANDA_ID, itemModelName);
      } else {
        return Identifier.ofVanilla("scaffolding");
      }
    }

    @Override
    public @Nullable ItemPlacementContext getPlacementContext(ItemPlacementContext context) {
      return ScaffoldingItemPlacementContextUtil.getItemPlacementContext(context,
          (ScaffoldingBlockMaxDistanceHolder) getBlock());
    }

    @Override
    protected boolean checkStatePlacement() {
      return false;
    }
  }

}