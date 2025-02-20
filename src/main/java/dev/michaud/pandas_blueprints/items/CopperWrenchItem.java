package dev.michaud.pandas_blueprints.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class CopperWrenchItem extends Item implements PolymerItem {

  public CopperWrenchItem(Settings settings) {
    super(settings);
  }

  @Override
  public ActionResult useOnBlock(ItemUsageContext context) {

    final PlayerEntity player = context.getPlayer();
    final World world = context.getWorld();
    final BlockPos pos = context.getBlockPos();

    if (player == null
        || !player.canModifyBlocks()
        || !player.canInteractWithBlockAt(pos, 0)) {
      return ActionResult.PASS;
    }

    final BlockState state = world.getBlockState(pos);
    final BlockEntity entity = world.getBlockEntity(pos);

    if (state.isAir() || entity != null
        || state.getBlock().getHardness() < 0
        || state.getBlock().getHardness() > 100) {
      return ActionResult.PASS; // Invalid block
    }

    // Rotate block
    final boolean shift = player.isSneaking();
    final BlockRotation direction =
        shift ? BlockRotation.COUNTERCLOCKWISE_90 : BlockRotation.CLOCKWISE_90;
    final BlockState newState = state.rotate(direction);

    if (state.equals(newState)) {
      return ActionResult.PASS; // State didn't change
    }

    world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
    world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, newState));

    // Damage item
    final ItemStack stack = context.getStack();
    final EquipmentSlot slot = LivingEntity.getSlotForHand(context.getHand());
    stack.damage(1, player, slot);

    return ActionResult.SUCCESS;
  }

  @Override
  public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
    return Items.TRIAL_KEY;
  }

  @Override
  public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
    if (PolymerResourcePackUtils.hasMainPack(context)) {
      return Identifier.of("greenpanda", "copper_wrench");
    } else {
      return Identifier.of("minecraft", "debug_stick");
    }
  }

  static AttributeModifiersComponent createAttributeModifiers() {
    return AttributeModifiersComponent.builder()
        .add(EntityAttributes.ATTACK_DAMAGE,
            new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, 3,
                EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
        .add(EntityAttributes.ATTACK_SPEED,
            new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID, -3.0,
                EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
        .build();
  }
}