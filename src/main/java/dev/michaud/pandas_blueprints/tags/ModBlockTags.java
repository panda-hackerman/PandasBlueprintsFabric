package dev.michaud.pandas_blueprints.tags;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

@SuppressWarnings("unused")
public class ModBlockTags {

  public static final TagKey<Block> BLUEPRINT_IGNORES = of("blueprint_ignores");
  public static final TagKey<Block> SCAFFOLDING = of("scaffolding");
  public static final TagKey<Block> COPPER_SCAFFOLDING = of("copper_scaffolding");
  public static final TagKey<Block> CUSTOM_WAXABLE = of("waxable");
  public static final TagKey<Block> CUSTOM_WAXED = of("waxed");

  private static TagKey<Block> of(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return TagKey.of(RegistryKeys.BLOCK, id);
  }
}