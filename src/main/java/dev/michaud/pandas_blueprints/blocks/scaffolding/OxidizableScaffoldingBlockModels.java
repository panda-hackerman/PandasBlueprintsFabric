package dev.michaud.pandas_blueprints.blocks.scaffolding;

import com.google.common.collect.ImmutableMap;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable.OxidationLevel;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ConstantConditions")
public class OxidizableScaffoldingBlockModels {

  public static final ImmutableMap<OxidationLevel, PolymerBlockModel> TOP_MODELS = ImmutableMap.of(
      OxidationLevel.UNAFFECTED, pathToBlockModel("block/copper_scaffolding_stable"),
      OxidationLevel.EXPOSED, pathToBlockModel("block/exposed_copper_scaffolding_stable"),
      OxidationLevel.WEATHERED, pathToBlockModel("block/weathered_copper_scaffolding_stable"),
      OxidationLevel.OXIDIZED, pathToBlockModel("block/oxidized_copper_scaffolding_stable")
  );
  public static final ImmutableMap<OxidationLevel, PolymerBlockModel> BOTTOM_MODELS = ImmutableMap.of(
      OxidationLevel.UNAFFECTED, pathToBlockModel("block/copper_scaffolding_unstable"),
      OxidationLevel.EXPOSED, pathToBlockModel("block/exposed_copper_scaffolding_unstable"),
      OxidationLevel.WEATHERED, pathToBlockModel("block/weathered_copper_scaffolding_unstable"),
      OxidationLevel.OXIDIZED, pathToBlockModel("block/oxidized_copper_scaffolding_unstable")
  );

  public static final ImmutableMap<OxidationLevel, BlockState> POLYMER_BLOCK_STATES_TOP;
  public static final ImmutableMap<OxidationLevel, BlockState> POLYMER_BLOCK_STATES_BOTTOM;
  public static final ImmutableMap<OxidationLevel, BlockState> POLYMER_BLOCK_STATES_TOP_WATERLOGGED;
  public static final ImmutableMap<OxidationLevel, BlockState> POLYMER_BLOCK_STATES_BOTTOM_WATERLOGGED;

  static {
    ImmutableMap.Builder<OxidationLevel, BlockState> topBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<OxidationLevel, BlockState> bottomBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<OxidationLevel, BlockState> topWaterloggedBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<OxidationLevel, BlockState> bottomWaterloggedBuilder = ImmutableMap.builder();

    TOP_MODELS.forEach((level, model) -> {
      topBuilder.put(level, PolymerBlockResourceUtils
              .requestBlock(BlockModelType.TOP_SCAFFOLDING, model));
      topWaterloggedBuilder.put(level, PolymerBlockResourceUtils
          .requestBlock(BlockModelType.TOP_SCAFFOLDING_WATERLOGGED, model));
    });

    BOTTOM_MODELS.forEach((level, model) -> {
      bottomBuilder.put(level, PolymerBlockResourceUtils
          .requestBlock(BlockModelType.BOTTOM_SCAFFOLDING, model));
      bottomWaterloggedBuilder.put(level, PolymerBlockResourceUtils
          .requestBlock(BlockModelType.BOTTOM_SCAFFOLDING_WATERLOGGED, model));
    });

    POLYMER_BLOCK_STATES_TOP = topBuilder.build();
    POLYMER_BLOCK_STATES_BOTTOM = bottomBuilder.build();
    POLYMER_BLOCK_STATES_TOP_WATERLOGGED = topWaterloggedBuilder.build();
    POLYMER_BLOCK_STATES_BOTTOM_WATERLOGGED = bottomWaterloggedBuilder.build();
  }

  public static void registerScaffoldingBlockModels() {
  }

  public static BlockState getPolymerBlockState(OxidationLevel oxidationLevel, boolean bottom,
      boolean waterlogged) {
    if (!bottom && !waterlogged) {
      return POLYMER_BLOCK_STATES_TOP.get(oxidationLevel);
    } else if (bottom && !waterlogged) {
      return POLYMER_BLOCK_STATES_BOTTOM.get(oxidationLevel);
    } else if (!bottom && waterlogged) {
      return POLYMER_BLOCK_STATES_TOP_WATERLOGGED.get(oxidationLevel);
    } else {
      return POLYMER_BLOCK_STATES_BOTTOM_WATERLOGGED.get(oxidationLevel);
    }
  }

  private static @NotNull PolymerBlockModel pathToBlockModel(String path) {
    return PolymerBlockModel.of(Identifier.of(PandasBlueprints.GREENPANDA_ID, path));
  }

}