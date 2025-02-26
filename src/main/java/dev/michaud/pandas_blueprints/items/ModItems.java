package dev.michaud.pandas_blueprints.items;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.BlueprintTableBlock.BlueprintTableItem;
import dev.michaud.pandas_blueprints.blocks.ModBlocks;
import dev.michaud.pandas_blueprints.items.wrench.CopperWrenchItem;
import dev.michaud.pandas_blueprints.items.wrench.WrenchDispenseBehavior;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ModItems {

  public static final Item EMPTY_BLUEPRINT = register("empty_blueprint", EmptyBlueprintItem::new,
      new Item.Settings());
  public static final Item FILLED_BLUEPRINT = register("filled_blueprint", FilledBlueprintItem::new,
      new Item.Settings());
  public static final Item BLUEPRINT_TABLE = register(ModBlocks.BLUEPRINT_TABLE, BlueprintTableItem::new,
      new Item.Settings());
  public static final Item COPPER_WRENCH = register("copper_wrench", CopperWrenchItem::new,
      new Item.Settings().repairable(Items.COPPER_INGOT).enchantable(10).maxCount(1).maxDamage(384)
          .attributeModifiers(CopperWrenchItem.createAttributeModifiers()));

  /**
   * Register an item
   */
  public static Item register(@NotNull String name, @NotNull Function<Item.Settings, Item> factory,
      @NotNull Item.Settings settings) {

    final Identifier id = Identifier.of(PandasBlueprints.MOD_ID, name);
    final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, id);

    settings.registryKey(registryKey);
    return Items.register(registryKey, factory, settings);
  }

  /**
   * Register a block item
   */
  public static Item register(Block block, BiFunction<Block, Settings, Item> factory,
      Item.Settings settings) {
    return Items.register(block, factory, settings);
  }

  public static void registerModItems() {
    DispenserBlock.registerBehavior(COPPER_WRENCH, new WrenchDispenseBehavior());
  }
}