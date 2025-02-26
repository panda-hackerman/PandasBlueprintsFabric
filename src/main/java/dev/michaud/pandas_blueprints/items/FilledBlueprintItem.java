package dev.michaud.pandas_blueprints.items;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.components.ModComponentTypes;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import java.text.Normalizer.Form;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

/**
 * A blueprint that has an associated schematic
 * @see EmptyBlueprintItem
 */
public class FilledBlueprintItem extends Item implements PolymerItem {

  public FilledBlueprintItem(Settings settings) {
    super(settings);
  }

  public static ItemStack createBlueprint(Identifier id, PlayerEntity author) {

    ItemStack itemStack = new ItemStack(ModItems.FILLED_BLUEPRINT);
    itemStack.set(ModComponentTypes.BLUEPRINT_ID, id);

    return itemStack;
  }

  @Override
  public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
    return Items.MAP;
  }

  @Override
  public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip,
      TooltipType type) {

    final Identifier id = stack.get(ModComponentTypes.BLUEPRINT_ID);

    if (id != null) {
      if (id.getNamespace().equals(PandasBlueprints.GREENPANDA_ID)) {
        tooltip.add(Text.translatable("filled_blueprint.id", Text.of(id.getPath()))
            .formatted(Formatting.GRAY));
      } else {
        tooltip.add(Text.translatable("filled_blueprint.id", Text.of(id))
            .formatted(Formatting.GRAY));
      }
    } else {
      tooltip.add(Text.translatable("filled_blueprint.unknown")
          .formatted(Formatting.GRAY));
    }
  }

  @Override
  public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
    if (PolymerResourcePackUtils.hasMainPack(context)) {
      return Identifier.of(PandasBlueprints.GREENPANDA_ID, "filled_blueprint");
    } else {
      return Identifier.ofVanilla("painting");
    }
  }
}