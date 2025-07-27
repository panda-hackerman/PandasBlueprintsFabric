package dev.michaud.pandas_blueprints.tags;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import net.minecraft.block.Block;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

@SuppressWarnings("unused")
public class ModTags {

  public static final TagKey<Item> SCAFFOLDING_ITEM = ofItem("scaffolding");
  public static final TagKey<Item> REPAIRS_HARD_HAT = ofItem("repairs_hard_hat");

  public static final TagKey<Block> SCAFFOLDING_BLOCK = ofBlock("scaffolding");
  public static final TagKey<Block> COPPER_SCAFFOLDING_BLOCK = ofBlock("copper_scaffolding");
  public static final TagKey<Block> CUSTOM_WAXABLE = ofBlock("waxable");
  public static final TagKey<Block> CUSTOM_WAXED = ofBlock("waxed");

  public static final TagKey<DamageType> BYPASSES_HAT_HAT = ofDamageType("bypasses_hard_hat");

  private static TagKey<Item> ofItem(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return TagKey.of(RegistryKeys.ITEM, id);
  }

  private static TagKey<Block> ofBlock(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return TagKey.of(RegistryKeys.BLOCK, id);
  }

  private static TagKey<DamageType> ofDamageType(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return TagKey.of(RegistryKeys.DAMAGE_TYPE, id);
  }

  public static void registerModTags() {
  }

}