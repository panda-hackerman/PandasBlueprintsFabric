package dev.michaud.pandas_blueprints.util;

import com.google.common.collect.ImmutableMap;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class MaterialListUtil {

  @NotNull
  public static ImmutableMap<ItemStack, Integer> getMaterialsList(BlueprintSchematic blueprint) {
    ImmutableMap.Builder<ItemStack, Integer> materials = ImmutableMap.builder();

    for (Block block : blueprint.getAllBlocks()) {
      final ItemStack item = block.asItem().getDefaultStack();
      final int count = blueprint.getCount(block);

      if (!item.isEmpty() && !block.getDefaultState().isAir()) {
        materials.put(item, count);
      }
    }

    // Custom overrides
    if (blueprint.getCount(Blocks.NETHER_PORTAL) > 0
        || blueprint.getCount(Blocks.FIRE) > 0) {
      materials.put(new ItemStack(Items.FLINT_AND_STEEL), 1);
    }

    return materials.build();
  }

}