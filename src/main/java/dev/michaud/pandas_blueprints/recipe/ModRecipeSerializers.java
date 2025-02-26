package dev.michaud.pandas_blueprints.recipe;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipeSerializers {

  public static final RecipeSerializer<BlueprintCloningRecipe> BLUEPRINT_CLONING = register(
      "crafting_special_blueprintcloning",
      new SpecialRecipeSerializer<>(BlueprintCloningRecipe::new));

  static <S extends RecipeSerializer<T>, T extends Recipe<?>> S register(String name, S serializer) {
    final Identifier id = Identifier.of(PandasBlueprints.MOD_ID, name);
    return Registry.register(Registries.RECIPE_SERIALIZER, id, serializer);
  }

  public static void registerModRecipeSerializers() {
  }
}