package dev.michaud.pandas_blueprints.items.wrench;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.entity.BlueprintTableBlockEntity;
import dev.michaud.pandas_blueprints.sounds.ModSounds;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class CopperWrenchItem extends Item implements PolymerItem {

  public CopperWrenchItem(Settings settings) {
    super(settings);
  }

  @Override
  public ActionResult useOnBlock(ItemUsageContext context) {

    final PlayerEntity player = context.getPlayer();
    final ServerWorld world = (ServerWorld) context.getWorld();
    final BlockPos pos = context.getBlockPos();
    final BlockState state = world.getBlockState(pos);

    if (player == null || !shouldAllowBlockRotation(player, world, pos)) {
      return ActionResult.PASS;
    }

    // Rotate block
    final boolean shift = player.isSneaking();
    final Direction hitFace = context.getSide();

    RotationType rotationType = switch (hitFace) {
      case UP -> RotationType.ofHorizontal(!shift);
      case DOWN -> RotationType.ofHorizontal(shift);
      case NORTH, SOUTH -> RotationType.ofVertical(!shift, true);
      case WEST, EAST -> RotationType.ofVertical(!shift, false);
    };

    BlockState newState = WrenchRotationUtil.rotateBlock(state, rotationType);

    if (state.equals(newState) && rotationType.isVertical()) {
      rotationType = rotationType.toHorizontal();
      newState = WrenchRotationUtil.rotateBlock(state, rotationType);
    }

    if (!state.equals(newState)) {
      world.setBlockState(pos, newState, Block.NOTIFY_ALL_AND_REDRAW);
      world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, Emitter.of(player, newState));
      world.playSound(null, pos, ModSounds.COPPER_WRENCH_USE, SoundCategory.BLOCKS, 1f, 1f);

      // Damage item
      final ItemStack stack = context.getStack();
      final EquipmentSlot slot = context.getHand().getEquipmentSlot();
      stack.damage(1, player, slot);

      return ActionResult.SUCCESS;
    }

    return ActionResult.PASS;
  }

  protected static boolean shouldAllowBlockRotation(@Nullable PlayerEntity player, ServerWorld world, BlockPos pos) {

    final BlockState state = world.getBlockState(pos);
    final BlockEntity entity = world.getBlockEntity(pos);

    if (player != null) {
      if (!player.canModifyBlocks()
          || !player.canModifyAt(world, pos)) {
        return false;
      }
    }

    if (state.isAir()
        || state.getBlock().getHardness() < 0
        || state.getBlock().getHardness() > 100) {
      return false; // Immovable block or air
    }

    // Disallow rotating extended pistons
    if (state.contains(Properties.EXTENDED)) {
      return !state.get(Properties.EXTENDED);
    }

    // Only allow whitelisted block entities
    if (entity != null) {
      if (entity instanceof ChestBlockEntity) {
        return state.get(Properties.CHEST_TYPE) == ChestType.SINGLE;
      }

      return entity instanceof BlueprintTableBlockEntity
          || entity instanceof CrafterBlockEntity
          || entity instanceof DispenserBlockEntity
          || entity instanceof DropperBlockEntity
          || entity instanceof HopperBlockEntity
          || entity instanceof SkullBlockEntity
          || entity instanceof BellBlockEntity
          || entity instanceof CampfireBlockEntity
          || entity instanceof BannerBlockEntity
          || entity instanceof BrewingStandBlockEntity;
    }

    return true;
  }

  @Override
  public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
    return Items.TRIAL_KEY;
  }

  @Override
  public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
    if (PolymerResourcePackUtils.hasMainPack(context)) {
      return Identifier.of(PandasBlueprints.GREENPANDA_ID, "copper_wrench");
    } else {
      return Identifier.ofVanilla("debug_stick");
    }
  }

  public static AttributeModifiersComponent createAttributeModifiers() {
    return AttributeModifiersComponent.builder()
        .add(EntityAttributes.ATTACK_DAMAGE,
            new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, 3,
                Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
        .add(EntityAttributes.ATTACK_SPEED,
            new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID, -3.0,
                Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
        .build();
  }
}