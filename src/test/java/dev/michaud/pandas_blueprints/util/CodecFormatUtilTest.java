package dev.michaud.pandas_blueprints.util;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CodecFormatUtilTest {

  public static final List<String> TEST_BLOCKSTATE_STRINGS = ImmutableList.of(
      "minecraft:blackstone",
      "minecraft:cornflower", "minecraft:tall_grass[half=upper]", "minecraft:moss_block",
      "minecraft:oak_leaves[distance=3,persistent=false,waterlogged=false]",
      "minecraft:oak_leaves[distance=2,persistent=false,waterlogged=false]",
      "minecraft:oak_leaves[distance=4,persistent=false,waterlogged=false]",
      "minecraft:redstone_block", "minecraft:moss_carpet",
      "minecraft:oak_leaves[distance=1,persistent=false,waterlogged=false]",
      "minecraft:trapped_chest[facing=east,type=single,waterlogged=false]", "minecraft:magma_block",
      "minecraft:oak_log[axis=y]", "minecraft:short_grass", "minecraft:dirt",
      "minecraft:mushroom_stem[down=true,east=true,north=true,south=true,up=false,west=true]",
      "minecraft:chest[facing=east,type=single,waterlogged=false]",
      "minecraft:mushroom_stem[down=false,east=true,north=true,south=true,up=false,west=true]",
      "minecraft:red_sand",
      "minecraft:mushroom_stem[down=false,east=true,north=true,south=true,up=true,west=true]",
      "minecraft:flowering_azalea", "minecraft:dandelion",
      "minecraft:ender_chest[facing=east,waterlogged=false]", "minecraft:tall_grass[half=lower]",
      "minecraft:azalea",
      "minecraft:pointed_dripstone[thickness=tip,vertical_direction=up,waterlogged=false]");

  @BeforeAll
  static void beforeAll() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void testBlockStateStringPerformance() {

    ImmutableList.Builder<BlockState> builder = ImmutableList
        .builderWithExpectedSize(TEST_BLOCKSTATE_STRINGS.size());

    Stopwatch stopwatch = Stopwatch.createStarted();

    for (String blockStateString : TEST_BLOCKSTATE_STRINGS) {
      BlockState state = CodecFormatUtil.stringToBlockState(blockStateString);
      builder.add(state);
    }

    stopwatch.stop();
    long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

    PandasBlueprints.LOGGER.info("Converted {} strings to block states in {}ms",
        TEST_BLOCKSTATE_STRINGS.size(), elapsed);

    List<BlockState> states = builder.build();
    ImmutableList.Builder<String> stringBuilder = ImmutableList
        .builderWithExpectedSize(states.size());

    stopwatch = Stopwatch.createStarted();

    for (BlockState state : states) {
      String str = CodecFormatUtil.blockStateToString(state);
      stringBuilder.add(str);
    }

    stopwatch.stop();
    elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

    PandasBlueprints.LOGGER.info("Converted {} block states to strings in {}ms",
        states.size(), elapsed);

  }
}