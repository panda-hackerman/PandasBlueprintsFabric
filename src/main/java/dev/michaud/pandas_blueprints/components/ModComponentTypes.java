package dev.michaud.pandas_blueprints.components;

import com.mojang.serialization.Codec;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.fabricmc.fabric.api.item.v1.ComponentTooltipAppenderRegistry;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModComponentTypes {

  public static final ComponentType<BlueprintIdComponent> BLUEPRINT_ID = register("blueprint_id",
      BlueprintIdComponent.CODEC);

  public static <T> ComponentType<T> register(String name, Codec<T> codec) {
    final Identifier id = Identifier.of(PandasBlueprints.MOD_ID, name);
    final ComponentType.Builder<T> builder = ComponentType.<T>builder().codec(codec);

    return Registry.register(Registries.DATA_COMPONENT_TYPE, id, builder.build());
  }

  public static void registerModComponents() {
    PolymerComponent.registerDataComponent(BLUEPRINT_ID);
    ComponentTooltipAppenderRegistry.addFirst(BLUEPRINT_ID);
  }

}