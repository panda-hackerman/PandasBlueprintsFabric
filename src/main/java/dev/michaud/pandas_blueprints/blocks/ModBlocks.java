package dev.michaud.pandas_blueprints.blocks;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.scaffolding.OxidizableScaffoldingBlock;
import java.util.function.Function;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.Oxidizable.OxidationLevel;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

@SuppressWarnings("unused")
public class ModBlocks {

  public static final Block BLUEPRINT_TABLE = register("blueprint_table", BlueprintTableBlock::new,
      AbstractBlock.Settings.create()
          .mapColor(state -> state.get(BlueprintTableBlock.HAS_BLUEPRINT) ? MapColor.BLUE
              : MapColor.YELLOW)
          .instrument(NoteBlockInstrument.BASS)
          .strength(2.5F)
          .sounds(BlockSoundGroup.BAMBOO_WOOD)
          .burnable());
  public static final Block COPPER_SCAFFOLDING = register("copper_scaffolding",
      settings -> new OxidizableScaffoldingBlock(OxidationLevel.UNAFFECTED, settings),
      AbstractBlock.Settings.create()
          .mapColor(Blocks.COPPER_BLOCK.getDefaultMapColor())
          .sounds(BlockSoundGroup.COPPER)
          .noCollision()
          .dynamicBounds()
          .allowsSpawning(Blocks::never)
          .solidBlock(Blocks::never));
  public static final Block EXPOSED_COPPER_SCAFFOLDING = register("exposed_copper_scaffolding",
      settings -> new OxidizableScaffoldingBlock(OxidationLevel.EXPOSED, settings),
      AbstractBlock.Settings.copy(COPPER_SCAFFOLDING)
          .mapColor(Blocks.EXPOSED_COPPER.getDefaultMapColor()));
  public static final Block WEATHERED_COPPER_SCAFFOLDING = register("weathered_copper_scaffolding",
      settings -> new OxidizableScaffoldingBlock(OxidationLevel.WEATHERED, settings),
      AbstractBlock.Settings.copy(COPPER_SCAFFOLDING)
          .mapColor(Blocks.WEATHERED_COPPER.getDefaultMapColor()));
  public static final Block OXIDIZED_COPPER_SCAFFOLDING = register("oxidized_copper_scaffolding",
      settings -> new OxidizableScaffoldingBlock(OxidationLevel.OXIDIZED, settings),
      AbstractBlock.Settings.copy(COPPER_SCAFFOLDING)
          .mapColor(Blocks.OXIDIZED_COPPER.getDefaultMapColor()));
  public static final Block WAXED_COPPER_SCAFFOLDING = register("waxed_copper_scaffolding",
      settings -> new OxidizableScaffoldingBlock(OxidationLevel.UNAFFECTED, settings),
      AbstractBlock.Settings.copy(COPPER_SCAFFOLDING));
  public static final Block WAXED_EXPOSED_COPPER_SCAFFOLDING = register(
      "waxed_exposed_copper_scaffolding",
      settings -> new OxidizableScaffoldingBlock(OxidationLevel.EXPOSED, settings),
      AbstractBlock.Settings.copy(EXPOSED_COPPER_SCAFFOLDING));
  public static final Block WAXED_WEATHERED_COPPER_SCAFFOLDING = register(
      "waxed_weathered_copper_scaffolding",
      settings -> new OxidizableScaffoldingBlock(OxidationLevel.WEATHERED, settings),
      AbstractBlock.Settings.copy(WEATHERED_COPPER_SCAFFOLDING));
  public static final Block WAXED_OXIDIZED_COPPER_SCAFFOLDING = register(
      "waxed_oxidized_copper_scaffolding",
      settings -> new OxidizableScaffoldingBlock(OxidationLevel.OXIDIZED, settings),
      AbstractBlock.Settings.copy(OXIDIZED_COPPER_SCAFFOLDING));

  public static Block register(String name, Function<AbstractBlock.Settings, Block> factory,
      AbstractBlock.Settings settings) {

    final Identifier id = Identifier.of(PandasBlueprints.MOD_ID, name);
    final RegistryKey<Block> registryKey = RegistryKey.of(RegistryKeys.BLOCK, id);
    final Block block = factory.apply(settings.registryKey(registryKey));

    return Registry.register(Registries.BLOCK, registryKey, block);
  }

  public static void registerModBlocks() {

    // Make blocks oxidizable
    OxidizableBlocksRegistry.registerOxidizableBlockPair(COPPER_SCAFFOLDING, EXPOSED_COPPER_SCAFFOLDING);
    OxidizableBlocksRegistry.registerOxidizableBlockPair(EXPOSED_COPPER_SCAFFOLDING, WEATHERED_COPPER_SCAFFOLDING);
    OxidizableBlocksRegistry.registerOxidizableBlockPair(WEATHERED_COPPER_SCAFFOLDING, OXIDIZED_COPPER_SCAFFOLDING);

    // Makes blocks waxable
    OxidizableBlocksRegistry.registerWaxableBlockPair(COPPER_SCAFFOLDING, WAXED_COPPER_SCAFFOLDING);
    OxidizableBlocksRegistry.registerWaxableBlockPair(EXPOSED_COPPER_SCAFFOLDING, WAXED_EXPOSED_COPPER_SCAFFOLDING);
    OxidizableBlocksRegistry.registerWaxableBlockPair(WEATHERED_COPPER_SCAFFOLDING, WAXED_WEATHERED_COPPER_SCAFFOLDING);
    OxidizableBlocksRegistry.registerWaxableBlockPair(OXIDIZED_COPPER_SCAFFOLDING, WAXED_OXIDIZED_COPPER_SCAFFOLDING);
  }

}