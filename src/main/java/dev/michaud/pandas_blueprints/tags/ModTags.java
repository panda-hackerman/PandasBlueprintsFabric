package dev.michaud.pandas_blueprints.tags;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {

  public static final TagKey<Item> SCAFFOLDING_ITEM = ofItem("scaffolding");
  public static final TagKey<Block> SCAFFOLDING_BLOCK = ofBlock("scaffolding");
  public static final TagKey<Block> COPPER_SCAFFOLDING_BLOCK = ofBlock("copper_scaffolding");

  private static TagKey<Item> ofItem(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return TagKey.of(RegistryKeys.ITEM, id);
  }

  private static TagKey<Block> ofBlock(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return TagKey.of(RegistryKeys.BLOCK, id);
  }

  public static void registerModTags() {
  }

}