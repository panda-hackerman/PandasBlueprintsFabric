package dev.michaud.pandas_blueprints.items.wrench;

import com.fasterxml.jackson.databind.annotation.JsonAppend.Prop;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.Orientation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class WrenchRotationUtil {

  public static final BiMap<Direction, Direction> verticalRotationX = ImmutableBiMap.of(
      Direction.UP, Direction.SOUTH,
      Direction.SOUTH, Direction.DOWN,
      Direction.DOWN, Direction.NORTH,
      Direction.NORTH, Direction.UP);
  public static final BiMap<Direction, Direction> verticalRotationZ = ImmutableBiMap.of(
      Direction.UP, Direction.EAST,
      Direction.EAST, Direction.DOWN,
      Direction.DOWN, Direction.WEST,
      Direction.WEST, Direction.UP);
  public static final BiMap<Axis, Axis> verticalAxisRotationX = ImmutableBiMap.of(
      Axis.Y, Axis.Z,
      Axis.Z, Axis.Y);
  public static final BiMap<Axis, Axis> verticalAxisRotationZ = ImmutableBiMap.of(
      Axis.Y, Axis.X,
      Axis.X, Axis.Y);
  public static final BiMap<Orientation, Orientation> verticalOrientationRotationX = ImmutableBiMap.of(
      Orientation.UP_SOUTH, Orientation.UP_NORTH,
      Orientation.UP_NORTH, Orientation.SOUTH_UP,
      Orientation.SOUTH_UP, Orientation.DOWN_NORTH,
      Orientation.DOWN_NORTH, Orientation.DOWN_SOUTH,
      Orientation.DOWN_SOUTH, Orientation.NORTH_UP,
      Orientation.NORTH_UP, Orientation.UP_SOUTH);
  public static final BiMap<Orientation, Orientation> verticalOrientationRotationZ = ImmutableBiMap.of(
      Orientation.UP_EAST, Orientation.UP_WEST,
      Orientation.UP_WEST, Orientation.EAST_UP,
      Orientation.EAST_UP, Orientation.DOWN_WEST,
      Orientation.DOWN_WEST, Orientation.DOWN_EAST,
      Orientation.DOWN_EAST, Orientation.WEST_UP,
      Orientation.WEST_UP, Orientation.UP_EAST);
  public static final BiMap<Direction, Direction> verticalHopperRotationX = ImmutableBiMap.of(
      Direction.DOWN, Direction.SOUTH,
      Direction.SOUTH, Direction.NORTH,
      Direction.NORTH, Direction.DOWN);
  public static final BiMap<Direction, Direction> verticalHopperRotationZ = ImmutableBiMap.of(
      Direction.DOWN, Direction.EAST,
      Direction.EAST, Direction.WEST,
      Direction.WEST, Direction.DOWN);


  public static BlockState rotateBlock(BlockState state, RotationType rotation) {
    if (rotation.isHorizontal()) {
      return rotateHorizontal(state, rotation.isClockwise());
    } else {
      return rotateVertical(state, rotation.isClockwise(), rotation.getAcrossAxis() == Axis.X);
    }
  }

  public static BlockState rotateHorizontal(@NotNull BlockState state, boolean clockwise) {

    if (state.contains(Properties.ROTATION)) {
      int rotation = state.get(Properties.ROTATION);
      rotation += clockwise ? 1 : -1;

      if (rotation < 0) {
        rotation = 15;
      } else if (rotation > 15) {
        rotation = 0;
      }

      return state.with(Properties.ROTATION, rotation);
    }

    return state.rotate(clockwise ? BlockRotation.CLOCKWISE_90 : BlockRotation.COUNTERCLOCKWISE_90);
  }

  @Contract(value = "_,_,_->new", pure = true)
  public static @NotNull BlockState rotateVertical(@NotNull BlockState state, boolean clockwise,
      boolean xAxis) {

    final RotationType rotationType = RotationType.ofVertical(clockwise, xAxis);

    if (state.contains(Properties.ORIENTATION)) {
      final Orientation orientation = state.get(Properties.ORIENTATION);
      final Orientation newOrientation = switch (rotationType) {
        case VERTICAL_CLOCKWISE_X -> verticalOrientationRotationX.get(orientation);
        case VERTICAL_CLOCKWISE_Z -> verticalOrientationRotationZ.get(orientation);
        case VERTICAL_COUNTERCLOCKWISE_X -> verticalOrientationRotationX.inverse().get(orientation);
        case VERTICAL_COUNTERCLOCKWISE_Z -> verticalOrientationRotationZ.inverse().get(orientation);
        default -> throw new IllegalStateException("Unexpected value: " + rotationType);
      };

      if (newOrientation != null) {
        return state.with(Properties.ORIENTATION, newOrientation);
      }
    }

    if (state.contains(Properties.HOPPER_FACING)) {
      final Direction direction = state.get(Properties.HOPPER_FACING);
      final Direction newDirection = switch (rotationType) {
        case VERTICAL_CLOCKWISE_X -> verticalHopperRotationX.get(direction);
        case VERTICAL_CLOCKWISE_Z -> verticalHopperRotationZ.get(direction);
        case VERTICAL_COUNTERCLOCKWISE_X -> verticalHopperRotationX.inverse().get(direction);
        case VERTICAL_COUNTERCLOCKWISE_Z -> verticalHopperRotationZ.inverse().get(direction);
        default -> throw new IllegalStateException("Unexpected value: " + rotationType);
      };

      if (newDirection != null) {
        return state.with(Properties.HOPPER_FACING, newDirection);
      }
    }

    if (state.contains(Properties.FACING)) {
      final Direction direction = state.get(Properties.FACING);
      final Direction newDirection = switch (rotationType) {
        case VERTICAL_CLOCKWISE_X -> verticalRotationX.get(direction);
        case VERTICAL_CLOCKWISE_Z -> verticalRotationZ.get(direction);
        case VERTICAL_COUNTERCLOCKWISE_X -> verticalRotationX.inverse().get(direction);
        case VERTICAL_COUNTERCLOCKWISE_Z -> verticalRotationZ.inverse().get(direction);
        default -> throw new IllegalStateException("Unexpected value: " + rotationType);
      };

      if (newDirection != null) {
        return state.with(Properties.FACING, newDirection);
      }
    }

    if (state.contains(Properties.AXIS)) {
      final Axis axis = state.get(Properties.AXIS);
      final Axis newAxis = switch (rotationType) {
        case VERTICAL_CLOCKWISE_X -> verticalAxisRotationX.get(axis);
        case VERTICAL_CLOCKWISE_Z -> verticalAxisRotationZ.get(axis);
        case VERTICAL_COUNTERCLOCKWISE_X -> verticalAxisRotationX.inverse().get(axis);
        case VERTICAL_COUNTERCLOCKWISE_Z -> verticalAxisRotationZ.inverse().get(axis);
        default -> throw new IllegalStateException("Unexpected value: " + rotationType);
      };

      if (newAxis != null) {
        return state.with(Properties.AXIS, newAxis);
      }
    }

    return state;
  }

  public static void rotateEntity(LivingEntity entity, boolean clockwise) {

    float yaw = entity.getYaw();
    yaw += (clockwise ? 45 : -45);

    entity.refreshPositionAndAngles(entity.getEntityPos(), yaw, entity.getPitch());
  }
}