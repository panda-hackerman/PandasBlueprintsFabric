package dev.michaud.pandas_blueprints.components;

import com.mojang.serialization.Codec;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import java.util.function.Consumer;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public record BlueprintIdComponent(Identifier id) implements TooltipAppender {

  public static final BlueprintIdComponent EMPTY = new BlueprintIdComponent(null);

  public static final Codec<BlueprintIdComponent> CODEC = Identifier.CODEC.xmap(
      BlueprintIdComponent::new,
      BlueprintIdComponent::id
  );

  @Override
  public void appendTooltip(TooltipContext context, Consumer<Text> textConsumer, TooltipType type,
      ComponentsAccess components) {
    if (id != null) {
      if (id.getNamespace().equals(PandasBlueprints.GREENPANDA_ID)) {
        textConsumer.accept(Text.translatable("filled_blueprint.id", Text.literal(id.getPath()))
            .formatted(Formatting.GRAY));
      } else {
        textConsumer.accept(Text.translatable("filled_blueprint.id", Text.literal(id.toString()))
            .formatted(Formatting.GRAY));
      }
    } else {
      textConsumer.accept(Text.translatable("filled_blueprint.unknown")
          .formatted(Formatting.GRAY));
    }
  }

  /**
   * Get an item's Blueprint id (if it exists).
   *
   * @param stack The item stack to check
   * @return The id, or null if the stack doesn't have a blueprint id component.
   */
  @Contract("null -> null")
  public static @Nullable Identifier getIdOrNull(ItemStack stack) {

    if (stack == null || stack.isEmpty()) {
      return null;
    }

    final BlueprintIdComponent component = stack.get(ModComponentTypes.BLUEPRINT_ID);
    return component != null ? component.id() : null;
  }
}