package dev.michaud.pandas_blueprints.util;

import net.minecraft.block.BannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RotationHelper {

  private static final Vector3f WALL_SKULL_OFFSET = new Vector3f(0, 0.25f, -0.25f);
  private static final Vector3f ZERO_VECTOR = new Vector3f(0, 0, 0);

  /**
   * Rotates a vector around the origin.
   *
   * @param rotation The amount to rotate.
   * @return The rotated vector.
   * @see BlockPos#rotate(BlockRotation)
   */
  public static @NotNull Vec3i rotate(@NotNull Vec3i pos, @NotNull BlockRotation rotation) {
    final int x = pos.getX();
    final int y = pos.getY();
    final int z = pos.getZ();

    return switch (rotation) {
      case NONE -> pos;
      case CLOCKWISE_90 -> new Vec3i(-z, y, x);
      case CLOCKWISE_180 -> new Vec3i(-x, y, -z);
      case COUNTERCLOCKWISE_90 -> new Vec3i(z, y, -x);
    };
  }

  /**
   * Turns a cardinal direction into a horizontal rotation, where:
   * <ul>
   *   <li>{@link Direction#NORTH} is no rotation</li>
   *   <li>{@link Direction#EAST} is 90 degrees clockwise</li>
   *   <li>{@link Direction#SOUTH} is 180 degrees, and</li>
   *   <li>{@link Direction#WEST} is 90 degrees counter-clockwise.</li>
   * </ul>
   * The inverse of {@link RotationHelper#blockRotationToDirection(BlockRotation)}.
   *
   * @param direction The direction to convert
   * @return The relevant block rotation
   */
  @Contract(pure = true)
  public static BlockRotation directionToBlockRotation(@NotNull Direction direction) {
    return switch (direction) {
      case NORTH -> BlockRotation.NONE;
      case EAST -> BlockRotation.CLOCKWISE_90;
      case SOUTH -> BlockRotation.CLOCKWISE_180;
      case WEST -> BlockRotation.COUNTERCLOCKWISE_90;
      default -> throw new IllegalArgumentException("Unexpected value: " + direction);
    };
  }

  /**
   * Turns a horizontal rotation into a cardinal direction, where:
   * <ul>
   *   <li>{@link BlockRotation#NONE} is North</li>
   *   <li>{@link BlockRotation#CLOCKWISE_90} is East</li>
   *   <li>{@link BlockRotation#CLOCKWISE_180} is South, and</li>
   *   <li>{@link BlockRotation#COUNTERCLOCKWISE_90} is West.</li>
   * </ul>
   * The inverse of {@link RotationHelper#directionToBlockRotation(Direction)}.
   *
   * @param rotation The rotation to convert
   * @return The relevant direction
   */
  @Contract(pure = true)
  public static Direction blockRotationToDirection(@NotNull BlockRotation rotation) {
    return switch (rotation) {
      case NONE -> Direction.NORTH;
      case CLOCKWISE_90 -> Direction.EAST;
      case CLOCKWISE_180 -> Direction.SOUTH;
      case COUNTERCLOCKWISE_90 -> Direction.WEST;
    };
  }

  /**
   * Turn a Minecraft yaw rotation into a rotation Quaternion.
   *
   * @param yaw The yaw in degrees
   * @return An equivalent rotation quaternion
   * @implNote Flips the handedness of the rotation, since Java OpenGL Math Library uses a
   * right-handed coordinate system and Minecraft uses a left-handed one. Here, this is equivalent
   * to flipping the sign of the yaw value.
   */
  public static Quaternionf yawRotationQuaternion(float yaw) {
    float yawRad = (float) Math.toRadians(yaw * -1);
    return new Quaternionf().rotateY(yawRad);
  }

  /**
   * Turn a Minecraft pitch and yaw value into a rotation Quaternion.
   *
   * @param pitch The pitch in degrees
   * @param yaw   The yaw in degrees
   * @return An equivalent rotation quaternion
   * @implNote Flips the handedness of the rotation, since Java OpenGL Math Library uses a
   * right-handed coordinate system and Minecraft uses a left-handed one.
   */
  public static Quaternionf pitchYawRotationQuaternion(float pitch, float yaw) {
    float pitchRad = (float) Math.toRadians(pitch);
    float yawRad = (float) Math.toRadians(yaw * -1);

    return new Quaternionf()
        .rotateY(yawRad)
        .rotateX(pitchRad);
  }

  /**
   * Turn a Minecraft pitch, yaw and roll value into a rotation Quaternion.
   *
   * @param pitch The pitch in degrees
   * @param yaw   The yaw in degrees
   * @param roll  The roll in degrees.
   * @return An equivalent rotation quaternion.
   * @implNote Flips the handedness of the rotation, since Java OpenGL Math Library uses a
   * right-handed coordinate system and Minecraft uses a left-handed one.
   */
  public static Quaternionf pitchYawRollRotationQuaternion(float pitch, float yaw, float roll) {

    float pitchRad = (float) Math.toRadians(pitch);
    float yawRad = (float) Math.toRadians(yaw * -1);
    float rollRad = (float) Math.toRadians(roll * 1);

    return new Quaternionf()
        .rotateY(yawRad)
        .rotateX(pitchRad)
        .rotateZ(rollRad);
  }

  /**
   * Calculates the rotation to give a {@link BlockDisplayEntity} so that it will appear to be
   * rotated in accordance with the given block state. Normally just setting the block state would
   * be enough, but some block entities don't render properly :(
   * <p>
   * For example, entities displaying chests always seem to face south, no matter what block state
   * they're given. Giving the block entity this transformation makes the entity visually match the
   * state.
   *
   * @param state The state to check
   * @return A transformation with the correct rotation
   */
  public static AffineTransformation getSpecialBlockEntityRotation(@NotNull BlockState state) {

    final Block block = state.getBlock();

    // Rotation
    final Quaternionf rot;

    if (state.contains(Properties.ROTATION)) {
      final int rotation = state.get(Properties.ROTATION);
      final float degrees;

      if (block instanceof BannerBlock) {
        degrees = (rotation * 22.5f); // Banners are flipped the other way, for some reason.
      } else {
        degrees = (rotation * 22.5f) - 180f;
      }

      rot = yawRotationQuaternion(degrees);
    } else if (state.contains(Properties.HORIZONTAL_FACING)) {
      final Direction facing = state.get(Properties.HORIZONTAL_FACING);
      rot = yawRotationQuaternion(Direction.getHorizontalDegreesOrThrow(facing));
    } else if (state.contains(Properties.FACING)) {
      final Direction facing = state.get(Properties.FACING);
      rot = facing.getRotationQuaternion();
    } else {
      rot = new Quaternionf();
    }

    // Offset
    final Vector3f offset;

    if (state.getBlock() instanceof WallSkullBlock) {
      offset = WALL_SKULL_OFFSET;
    } else {
      offset = ZERO_VECTOR;
    }

    return rotationAboutBlockCenter(rot, offset);
  }

  /**
   * Rotate around the center of a block
   *
   * @param rotation The rotation to apply
   * @param offset   An offset, applied before the rotation
   * @return A transformation with the correct rotation and translation
   */
  public static @NotNull AffineTransformation rotationAboutBlockCenter(Quaternionf rotation,
      Vector3f offset) {

    final float tx = offset.x - 0.5f;
    final float ty = offset.y - 0.5f;
    final float tz = offset.z - 0.5f;

    final Matrix4f matrix = new Matrix4f()
        .translation(0.5f, 0.5f, 0.5f)
        .rotate(rotation)
        .translate(tx, ty, tz);

    return new AffineTransformation(matrix);
  }

}