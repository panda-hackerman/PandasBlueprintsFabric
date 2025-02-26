package dev.michaud.pandas_blueprints.recipe;

import dev.michaud.pandas_blueprints.components.ModComponentTypes;
import dev.michaud.pandas_blueprints.items.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.world.World;

public class BlueprintCloningRecipe extends SpecialCraftingRecipe {

  public BlueprintCloningRecipe(CraftingRecipeCategory category) {
    super(category);
  }

  @Override
  public boolean matches(CraftingRecipeInput input, World world) {
    if (input.getStackCount() < 2) {
      return false;
    }

    boolean filledBlueprint = false;
    boolean emptyBlueprint = false;

    for (final ItemStack stack : input.getStacks()) {

      if (stack.isEmpty()) {
        continue;
      }

      if (stack.contains(ModComponentTypes.BLUEPRINT_ID)) {
        if (filledBlueprint) {
          return false; // Two filled blueprints, invalid
        }

        filledBlueprint = true;
      } else {
        if (!stack.isOf(ModItems.EMPTY_BLUEPRINT)) {
          return false; // Item that isn't a blueprint, invalid
        }

        emptyBlueprint = true;
      }
    }

    return filledBlueprint && emptyBlueprint;
  }

  @Override
  public ItemStack craft(CraftingRecipeInput input, WrapperLookup registries) {

    int numberOfCopies = 0;
    ItemStack map = ItemStack.EMPTY;

    for (final ItemStack stack : input.getStacks()) {
      if (stack.isEmpty()) {
        continue;
      }

      if (stack.contains(ModComponentTypes.BLUEPRINT_ID)) {
        if (!map.isEmpty()) {
          return ItemStack.EMPTY; // Two filled blueprints, invalid
        }

        map = stack;
      } else {
        if (!stack.isOf(ModItems.EMPTY_BLUEPRINT)) {
          return ItemStack.EMPTY; // Item that isn't a blueprint, invalid
        }

        numberOfCopies++;
      }
    }

    if (map.isEmpty() || numberOfCopies < 1) {
      return ItemStack.EMPTY;
    } else {
      return map.copyWithCount(numberOfCopies + 1);
    }
  }

  @Override
  public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
    return ModRecipeSerializers.BLUEPRINT_CLONING;
  }
}