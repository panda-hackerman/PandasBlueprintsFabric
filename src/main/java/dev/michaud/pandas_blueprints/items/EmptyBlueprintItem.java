package dev.michaud.pandas_blueprints.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

/**
 * A blueprint without a schematic
 * @see FilledBlueprintItem
 */
public class EmptyBlueprintItem extends Item implements PolymerItem {

  public EmptyBlueprintItem(Settings settings) {
    super(settings);
  }

  @Override
  public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
    return Items.PAINTING;
  }

  @Override
  public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
    if (PolymerResourcePackUtils.hasMainPack(context)) {
      return Identifier.of("greenpanda", "empty_blueprint");
    } else {
      return Identifier.of("minecraft", "painting");
    }
  }
}