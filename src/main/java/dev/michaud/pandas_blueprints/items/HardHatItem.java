package dev.michaud.pandas_blueprints.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class HardHatItem extends Item implements PolymerItem {

  public static final int DURABILITY_STAT = 25;
  public static final int DEFENSE_STAT = 1;
  public static final int ENCHANTABILITY_STAT = 8;
  public static final float TOUGHNESS_STAT = 1;
  public static final float KNOCKBACK_RESISTANCE_STAT = 0;
  public static final int MAX_DAMAGE = EquipmentType.HELMET.getMaxDamage(DURABILITY_STAT);

  public HardHatItem(Settings settings) {
    super(settings);
  }

  public static AttributeModifiersComponent createAttributeModifiers() {
    final AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
    final AttributeModifierSlot headSlot = AttributeModifierSlot.HEAD;
    final Identifier attributeId = Identifier.ofVanilla("armor.helmet");

    builder.add(EntityAttributes.ARMOR,
        new EntityAttributeModifier(attributeId, DEFENSE_STAT, Operation.ADD_VALUE), headSlot);
    builder.add(EntityAttributes.ARMOR_TOUGHNESS,
        new EntityAttributeModifier(attributeId, TOUGHNESS_STAT, Operation.ADD_VALUE), headSlot);

    //noinspection ConstantValue
    if (KNOCKBACK_RESISTANCE_STAT > 0) {
      builder.add(EntityAttributes.KNOCKBACK_RESISTANCE, new EntityAttributeModifier(
          attributeId, KNOCKBACK_RESISTANCE_STAT, Operation.ADD_VALUE), headSlot);
    }

    return builder.build();
  }

  @Override
  public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
    if (PolymerResourcePackUtils.hasMainPack(context)) {
      return Identifier.of("greenpanda", "hard_hat");
    } else {
      return Identifier.ofVanilla("leather_helmet");
    }
  }

  @Override
  public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
    return Items.LEATHER_HORSE_ARMOR;
  }
}