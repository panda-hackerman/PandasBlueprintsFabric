package dev.michaud.pandas_blueprints.blocks.scaffolding;

import com.mojang.serialization.MapCodec;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class OxidizableScaffoldingBlock extends ScaffoldingBlock implements PolymerTexturedBlock,
    ScaffoldingBlockMaxDistanceHolder {

  public static final int MAX_DISTANCE = 16;

  public static final IntProperty DISTANCE = IntProperty.of("distance", 0, MAX_DISTANCE);

  public static final MapCodec<ScaffoldingBlock> CODEC = createCodec(OxidizableScaffoldingBlock::new);

  public static final PolymerBlockModel BLOCK_MODEL_TOP = PolymerBlockModel.of(
      Identifier.of(PandasBlueprints.GREENPANDA_ID, "block/copper_scaffolding_stable"));
  public static final PolymerBlockModel BLOCK_MODEL_BOTTOM = PolymerBlockModel.of(
      Identifier.of(PandasBlueprints.GREENPANDA_ID, "block/copper_scaffolding_unstable"));

  public static final BlockState POLYMER_BLOCK_STATE_TOP = PolymerBlockResourceUtils.requestBlock(
      BlockModelType.VINES_BLOCK, BLOCK_MODEL_TOP);
  public static final BlockState POLYMER_BLOCK_STATE_BOTTOM = PolymerBlockResourceUtils.requestBlock(
      BlockModelType.VINES_BLOCK, BLOCK_MODEL_BOTTOM);
  public static final BlockState POLYMER_BLOCK_STATE_TOP_WATERLOGGED = PolymerBlockResourceUtils.requestBlock(
      BlockModelType.TRANSPARENT_BLOCK_WATERLOGGED, BLOCK_MODEL_TOP);
  public static final BlockState POLYMER_BLOCK_STATE_BOTTOM_WATERLOGGED = PolymerBlockResourceUtils.requestBlock(
      BlockModelType.TRANSPARENT_BLOCK_WATERLOGGED, BLOCK_MODEL_BOTTOM);

  public OxidizableScaffoldingBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState()
        .with(DISTANCE, getMaxDistance())
        .with(WATERLOGGED, false)
        .with(BOTTOM, false));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(DISTANCE, WATERLOGGED, BOTTOM);
  }

  @Override
  public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
    if (state.get(WATERLOGGED)) {
      return state.get(BOTTOM) ? POLYMER_BLOCK_STATE_BOTTOM_WATERLOGGED : POLYMER_BLOCK_STATE_TOP_WATERLOGGED;
    } else {
      return state.get(BOTTOM) ? POLYMER_BLOCK_STATE_BOTTOM : POLYMER_BLOCK_STATE_TOP;
    }
  }

  @Override
  public MapCodec<ScaffoldingBlock> getCodec() {
    return CODEC;
  }

  @Override
  public IntProperty getDistanceProperty() {
    return DISTANCE;
  }

  @Override
  public int getMaxDistance() {
    return MAX_DISTANCE;
  }

  public static class OxidizableScaffoldingBlockItem extends PolymerBlockItem {

    public OxidizableScaffoldingBlockItem(Block block, Settings settings) {
      super(block, settings);
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
      if (PolymerResourcePackUtils.hasMainPack(context)) {
        return Identifier.of(PandasBlueprints.GREENPANDA_ID, "copper_scaffolding");
      } else {
        return Identifier.ofVanilla("scaffolding");
      }
    }

    @Override
    public @Nullable ItemPlacementContext getPlacementContext(ItemPlacementContext context) {
      return ScaffoldingItemPlacementContextUtil.getItemPlacementContext(context, MAX_DISTANCE);
    }

    @Override
    protected boolean checkStatePlacement() {
      return false;
    }
  }

}