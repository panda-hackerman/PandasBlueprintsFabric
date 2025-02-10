package dev.michaud.pandas_blueprints.blocks;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import java.util.function.Function;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

  public static final Block BLUEPRINT_TABLE = register("blueprint_table", BlueprintTableBlock::new,
      AbstractBlock.Settings.create()
          .mapColor(state -> state.get(BlueprintTableBlock.HAS_BLUEPRINT) ? MapColor.BLUE : MapColor.YELLOW)
          .instrument(NoteBlockInstrument.BASS)
          .strength(2.5F)
          .sounds(BlockSoundGroup.BAMBOO_WOOD)
          .burnable());

  public static Block register(String name, Function<AbstractBlock.Settings, Block> factory,
      AbstractBlock.Settings settings) {

    final Identifier id = Identifier.of(PandasBlueprints.MOD_ID, name);
    final RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, id);

    return Blocks.register(key, factory, settings);
  }

  public static void registerModBlocks() {
  }

}