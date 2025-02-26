package dev.michaud.pandas_blueprints.items.wrench;

import dev.michaud.pandas_blueprints.sounds.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.FallibleItemDispenserBehavior;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;

public class WrenchDispenseBehavior extends FallibleItemDispenserBehavior {

  @Override
  protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {

    final ServerWorld world = pointer.world();
    final Direction facing = pointer.state().get(DispenserBlock.FACING);
    final BlockPos dispenserPos = pointer.pos();
    final BlockPos facingPos = dispenserPos.offset(facing);

    final Direction.Axis axis = facing.getAxis();
    final boolean clockwise = facing.getDirection() == AxisDirection.POSITIVE;

    setSuccess(tryRotateEntity(world, facingPos, clockwise) || tryRotateBlock(world, facingPos, axis, clockwise));

    if (isSuccess()) {
      stack.damage(1, world, null, i -> {
      });
    }

    return stack;
  }

  private boolean tryRotateBlock(ServerWorld world, BlockPos facingPos, Direction.Axis axis, boolean clockwise) {

    final BlockState facingState = world.getBlockState(facingPos);

    if (CopperWrenchItem.shouldAllowBlockRotation(null, world, facingPos)) {
      final RotationType rotationType = RotationType.of(axis, clockwise);
      final BlockState newFacingState = WrenchRotationUtil.rotateBlock(facingState, rotationType);

      if (!facingState.equals(newFacingState)) {
        world.setBlockState(facingPos, newFacingState, Block.NOTIFY_ALL_AND_REDRAW);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, facingPos, Emitter.of(newFacingState));
        world.playSound(null, facingPos, ModSounds.COPPER_WRENCH_USE, SoundCategory.BLOCKS, 1f, 1f);
        return true;
      }
    }

    return false;
  }

  private boolean tryRotateEntity(ServerWorld world, BlockPos facingPos, boolean clockwise) {

    final Box box = new Box(facingPos);
    final ArmorStandEntity armorStand = world.getEntitiesByClass(ArmorStandEntity.class, box,
            e -> !e.isMarker())
        .stream()
        .findAny()
        .orElse(null);

    if (armorStand != null) {
      WrenchRotationUtil.rotateEntity(armorStand, clockwise);
      world.playSound(null, facingPos, ModSounds.COPPER_WRENCH_USE, SoundCategory.BLOCKS, 1f, 1f);
      return true;
    } else {
      return false;
    }
  }
}