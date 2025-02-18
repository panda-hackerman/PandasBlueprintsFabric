package dev.michaud.pandas_blueprints.util;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class CustomMathHelper {

  public static int min(int a, int b, int c, int d) {
    return Math.min(Math.min(a, b), Math.min(c, d));
  }

  public static int min(int a, int b, int c) {
    return Math.min(Math.min(a, b), c);
  }

  public static int max(int a, int b, int c, int d) {
    return Math.max(Math.max(a, b), Math.max(c, d));
  }

  public static int max(int a, int b, int c) {
    return Math.max(Math.max(a, b), c);
  }

  public static BlockPos getMaxPos(BlockBox box) {
    return new BlockPos(box.getMaxX(), box.getMaxY(), box.getMaxZ());
  }

  public static BlockPos getMinPos(BlockBox box) {
    return new BlockPos(box.getMinX(), box.getMinY(), box.getMinZ());
  }

  public static Vec3i getSize(BlockBox box) {
    return new Vec3i(box.getBlockCountX(), box.getBlockCountY(), box.getBlockCountZ());
  }

}
