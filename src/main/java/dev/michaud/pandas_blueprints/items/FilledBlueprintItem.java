package dev.michaud.pandas_blueprints.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class FilledBlueprintItem extends Item implements PolymerItem {

  public FilledBlueprintItem(Settings settings) {
    super(settings);
  }

  @Override
  public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
    return Items.MAP;
  }

  @Override
  public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
    if (PolymerResourcePackUtils.hasMainPack(context)) {
      return Identifier.of("greenpanda", "filled_blueprint");
    } else {
      return Identifier.of("minecraft", "painting");
    }
  }
}