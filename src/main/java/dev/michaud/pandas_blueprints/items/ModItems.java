package dev.michaud.pandas_blueprints.items;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.BlueprintTableBlock.BlueprintTableItem;
import dev.michaud.pandas_blueprints.blocks.ModBlocks;
import dev.michaud.pandas_blueprints.blocks.scaffolding.OxidizableScaffoldingBlock.OxidizableScaffoldingBlockItem;
import dev.michaud.pandas_blueprints.items.wrench.CopperWrenchItem;
import dev.michaud.pandas_blueprints.items.wrench.WrenchDispenseBehavior;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ModItems {

  public static final Item EMPTY_BLUEPRINT = register("empty_blueprint",
      EmptyBlueprintItem::new, new Item.Settings());
  public static final Item FILLED_BLUEPRINT = register("filled_blueprint",
      FilledBlueprintItem::new, new Item.Settings());
  public static final Item BLUEPRINT_TABLE = register(ModBlocks.BLUEPRINT_TABLE,
      BlueprintTableItem::new, new Item.Settings());
  public static final Item COPPER_WRENCH = register("copper_wrench", CopperWrenchItem::new,
      new Item.Settings().repairable(Items.COPPER_INGOT).enchantable(10).maxCount(1).maxDamage(384)
          .attributeModifiers(CopperWrenchItem.createAttributeModifiers()));

  // -- Scaffolding
  public static final Item COPPER_SCAFFOLDING = register(
      ModBlocks.COPPER_SCAFFOLDING, OxidizableScaffoldingBlockItem::new,
      new Item.Settings());
  public static final Item EXPOSED_COPPER_SCAFFOLDING = register(
      ModBlocks.EXPOSED_COPPER_SCAFFOLDING, OxidizableScaffoldingBlockItem::new,
      new Item.Settings());
  public static final Item WEATHERED_COPPER_SCAFFOLDING = register(
      ModBlocks.WEATHERED_COPPER_SCAFFOLDING, OxidizableScaffoldingBlockItem::new,
      new Item.Settings());
  public static final Item OXIDIZED_COPPER_SCAFFOLDING = register(
      ModBlocks.OXIDIZED_COPPER_SCAFFOLDING, OxidizableScaffoldingBlockItem::new,
      new Item.Settings());

  // -- Waxed scaffolding
  public static final Item WAXED_COPPER_SCAFFOLDING = register(
      ModBlocks.WAXED_COPPER_SCAFFOLDING, OxidizableScaffoldingBlockItem::new,
      new Item.Settings());
  public static final Item WAXED_EXPOSED_COPPER_SCAFFOLDING = register(
      ModBlocks.WAXED_EXPOSED_COPPER_SCAFFOLDING, OxidizableScaffoldingBlockItem::new,
      new Item.Settings());
  public static final Item WAXED_WEATHERED_COPPER_SCAFFOLDING = register(
      ModBlocks.WAXED_WEATHERED_COPPER_SCAFFOLDING, OxidizableScaffoldingBlockItem::new,
      new Item.Settings());
  public static final Item WAXED_OXIDIZED_COPPER_SCAFFOLDING = register(
      ModBlocks.WAXED_OXIDIZED_COPPER_SCAFFOLDING, OxidizableScaffoldingBlockItem::new,
      new Item.Settings());

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

    // Add to creative menu
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(group -> {
      group.addAfter(Items.BRUSH, COPPER_WRENCH);
      group.addBefore(Items.MAP, EMPTY_BLUEPRINT);
    });

    ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(group -> {
      group.addAfter(Items.CRAFTER, BLUEPRINT_TABLE);
    });

    ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(group -> {
      group.addAfter(Items.LECTERN, BLUEPRINT_TABLE);

      group.addAfter(Items.SCAFFOLDING,
          COPPER_SCAFFOLDING,
          EXPOSED_COPPER_SCAFFOLDING,
          WEATHERED_COPPER_SCAFFOLDING,
          OXIDIZED_COPPER_SCAFFOLDING,
          WAXED_COPPER_SCAFFOLDING,
          WAXED_EXPOSED_COPPER_SCAFFOLDING,
          WAXED_WEATHERED_COPPER_SCAFFOLDING,
          WAXED_OXIDIZED_COPPER_SCAFFOLDING);
    });
  }
}