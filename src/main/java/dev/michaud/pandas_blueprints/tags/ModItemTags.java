package dev.michaud.pandas_blueprints.tags;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

@SuppressWarnings("unused")
public class ModItemTags {

  public static final TagKey<Item> SCAFFOLDING = of("scaffolding");
  public static final TagKey<Item> COPPER_SCAFFOLDING = of("copper_scaffolding");
  public static final TagKey<Item> REPAIRS_HARD_HAT = of("repairs_hard_hat");

  private static TagKey<Item> of(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return TagKey.of(RegistryKeys.ITEM, id);
  }

}