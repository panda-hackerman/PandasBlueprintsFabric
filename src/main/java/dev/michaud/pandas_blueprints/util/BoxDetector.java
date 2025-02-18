package dev.michaud.pandas_blueprints.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class BoxDetector {

  public static Optional<BlockBox> detectOutlineOf(BlockPos startPos, int scanDistance,
      Function<BlockPos, Boolean> validFrameBlockFunction) {

    final Map<Direction, List<BlockPos>> blocksByDirection = ImmutableMap.of(
        Direction.NORTH, getBlocksOfTypeInDirection(startPos, Direction.NORTH, scanDistance,
            validFrameBlockFunction),
        Direction.EAST,
        getBlocksOfTypeInDirection(startPos, Direction.EAST, scanDistance, validFrameBlockFunction),
        Direction.SOUTH, getBlocksOfTypeInDirection(startPos, Direction.SOUTH, scanDistance,
            validFrameBlockFunction),
        Direction.WEST, getBlocksOfTypeInDirection(startPos, Direction.WEST, scanDistance,
            validFrameBlockFunction));

    final List<BlockPos> baseSides = blocksByDirection.values().stream()
        .filter(blockPos -> !blockPos.isEmpty())
        .map(List::getFirst)
        .toList();

    if (baseSides.size() < 2) {
      return Optional.empty();
    }

    final Optional<BlockBox> baseSquare = getBoxSurrounding(baseSides);

    if (baseSquare.isEmpty() || !hasValidPerimeter(baseSquare.get(), validFrameBlockFunction)) {
      return Optional.empty();
    }

    final Optional<BlockBox> topSquare = getHighestValidTopSquare(baseSquare.get(), scanDistance * 2,
        validFrameBlockFunction);

    return topSquare.map(top -> baseSquare.get().encompass(top));
  }

  public static Optional<BlockBox> getHighestValidTopSquare(BlockBox bottomBox, int scanDistance,
      Function<BlockPos, Boolean> validFrameBlockFunction) {

    final BlockPos cornerNW = getMaxPos(bottomBox);
    final BlockPos cornerSE = getMinPos(bottomBox);

    final List<BlockPos> scaffoldingNW = getBlocksOfTypeInDirection(getMaxPos(bottomBox),
        Direction.UP, scanDistance, validFrameBlockFunction);
    final List<BlockPos> scaffoldingSE = getBlocksOfTypeInDirection(getMaxPos(bottomBox),
        Direction.UP, scanDistance, validFrameBlockFunction);

    final List<Integer> commonHeights = scaffoldingNW.stream()
        .map(BlockPos::getY)
        .filter(i -> scaffoldingSE.stream().anyMatch(b -> b.getY() == i))
        .sorted(Comparator.reverseOrder())
        .toList();

    for (int y : commonHeights) {

      PandasBlueprints.LOGGER.info("Looking for top at y = {}", y);

      BlockPos nw = cornerNW.withY(y);
      BlockPos se = cornerSE.withY(y);

      Optional<BlockBox> box = getBoxSurrounding(List.of(nw, se));

      if (box.isPresent() && hasValidPerimeter(box.get(), validFrameBlockFunction)) {
        return box;
      }
    }

    return Optional.empty();
  }

  /**
   * Gets a list of blocks in some direction that match the specified function.
   *
   * @param origin                  The starting position
   * @param direction               The direction to check
   * @param maxDistance             The maximum distance
   * @param validFrameBlockFunction A function that takes a block position and returns a true if it
   *                                is a valid block
   * @return A list of blocks (might be empty)
   */
  public static @NotNull @Unmodifiable List<BlockPos> getBlocksOfTypeInDirection(BlockPos origin,
      Direction direction, int maxDistance, Function<BlockPos, Boolean> validFrameBlockFunction) {

    List<BlockPos> out = new ArrayList<>();
    final Vec3i vector = direction.getVector();

    for (int i = 1; i <= maxDistance; i++) {
      final BlockPos pos = origin.add(vector.multiply(i));
      if (validFrameBlockFunction.apply(pos)) {
        out.add(pos);
      }
    }

    // Sort farthest first
    out.sort(Comparator.comparingDouble((BlockPos e) -> e.getSquaredDistance(origin)).reversed());

    return ImmutableList.copyOf(out);
  }

  /**
   * @return The minimum sized box containing all the given positions.
   */
  protected static Optional<BlockBox> getBoxSurrounding(Iterable<BlockPos> positions) {

    if (positions == null || !positions.iterator().hasNext()) {
      return Optional.empty();
    }

    Iterator<BlockPos> iterator = positions.iterator();
    BlockPos firstPos = iterator.next();

    int y = firstPos.getY();

    int minX = firstPos.getX();
    int maxX = firstPos.getX();
    int minZ = firstPos.getZ();
    int maxZ = firstPos.getZ();

    while (iterator.hasNext()) {
      BlockPos pos = iterator.next();

      int x = pos.getX();
      int z = pos.getZ();

      minX = Math.min(x, minX);
      maxX = Math.max(x, maxX);

      minZ = Math.min(z, minZ);
      maxZ = Math.max(z, maxZ);
    }

    return Optional.of(new BlockBox(minX, y, minZ, maxX, y, maxZ));
  }

  /**
   * @return True if the perimeter of this box contains scaffolding.
   */
  protected static boolean hasValidPerimeter(BlockBox box,
      Function<BlockPos, Boolean> validFrameBlockFunction) {

    final BlockPos maxPos = BoxDetector.getMaxPos(box);
    final BlockPos minPos = BoxDetector.getMinPos(box);

    PandasBlueprints.LOGGER.info("Checking from bounds {} to {}", maxPos, minPos);

    for (int i = 0; i < box.getBlockCountX(); i++) {
      BlockPos east = minPos.east(i);
      BlockPos west = maxPos.west(i);

      if (!validFrameBlockFunction.apply(east) || !validFrameBlockFunction.apply(west)) {
        return false;
      }
    }

    for (int i = 0; i < box.getBlockCountZ(); i++) {
      BlockPos south = minPos.south(i);
      BlockPos north = maxPos.north(i);

      if (!validFrameBlockFunction.apply(north) || !validFrameBlockFunction.apply(south)) {
        return false;
      }
    }

    return true;
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