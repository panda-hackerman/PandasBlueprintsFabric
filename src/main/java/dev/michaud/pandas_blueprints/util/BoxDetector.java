package dev.michaud.pandas_blueprints.util;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class BoxDetector {

  /**
   * Attempts to find the largest 3d bounding box that has a perimeter made of valid blocks.
   *
   * @param startPos                The starting position. Can be in the corner or anywhere inside
   *                                the base-level of the structure you want to detect.
   * @param maximumSideLength       The maximum side length of the box.
   * @param validFrameBlockFunction A function that takes a boolean and returns true if it is a
   *                                valid block
   * @return A bounding box, if found, otherwise empty
   */
  public static Optional<BlockBox> detectOutlineOf(BlockPos startPos, int maximumSideLength,
      Function<BlockPos, Boolean> validFrameBlockFunction) {

    Stopwatch stopwatch = Stopwatch.createStarted();

    final Optional<BlockBox> baseSquare = findLargestValidBaseSquare(startPos, maximumSideLength,
        validFrameBlockFunction);

    if (baseSquare.isEmpty()) {
      return Optional.empty();
    }

    final Optional<BlockBox> topSquare = findHighestValidTopSquare(baseSquare.get(),
        maximumSideLength, validFrameBlockFunction);

    Optional<BlockBox> out = topSquare.map(top -> baseSquare.get().encompass(top));

    stopwatch.stop();
    PandasBlueprints.LOGGER.info("Detected an outline in {}ms",
        stopwatch.elapsed(TimeUnit.MILLISECONDS));

    return out;
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
  public static @NotNull @Unmodifiable List<BlockPos> getValidBlocksInDir(BlockPos origin,
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
  public static Optional<BlockBox> getBoxSurrounding(Iterable<BlockPos> positions) {

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
   * @return The minimum sized box containing all the given positions
   */
  public static Optional<BlockBox> getBoxSurrounding(BlockPos... positions) {
    return getBoxSurrounding(Arrays.stream(positions).toList());
  }

  public static Optional<BlockBox> findHighestValidTopSquare(BlockBox bottomBox, int scanDistance,
      Function<BlockPos, Boolean> validFrameBlockFunction) {

    final BlockPos bottomNW = getMaxPos(bottomBox);
    final BlockPos bottomSE = getMinPos(bottomBox);

    final List<BlockPos> upScaffoldingNW = getValidBlocksInDir(getMaxPos(bottomBox),
        Direction.UP, scanDistance, validFrameBlockFunction);
    final List<BlockPos> upScaffoldingSE = getValidBlocksInDir(getMaxPos(bottomBox),
        Direction.UP, scanDistance, validFrameBlockFunction);

    final List<Integer> commonHeights = upScaffoldingNW.stream()
        .map(BlockPos::getY)
        .filter(i -> upScaffoldingSE.stream().anyMatch(b -> b.getY() == i))
        .sorted(Comparator.reverseOrder())
        .toList();

    for (int y : commonHeights) {

      final Optional<BlockBox> box = getBoxSurrounding(bottomNW.withY(y), bottomSE.withY(y));

      if (box.isPresent() && hasValidPerimeter(box.get(), validFrameBlockFunction)) {
        return box;
      }
    }

    return Optional.empty();
  }

  public static Optional<BlockBox> findLargestValidBaseSquare(BlockPos startPos, int scanDistance,
      Function<BlockPos, Boolean> validFrameBlockFunction) {

    final List<List<BlockPos>> blocksByDirection = new ArrayList<>();

    for (Direction direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH,
        Direction.WEST)) {
      List<BlockPos> list = getValidBlocksInDir(startPos, direction, scanDistance,
          validFrameBlockFunction);

      if (!list.isEmpty()) {
        blocksByDirection.add(list);
      }
    }

    if (blocksByDirection.size() < 2) {
      return Optional.empty(); // Need at least 2
    }

    List<BlockBox> possibleBoxes = Lists.cartesianProduct(blocksByDirection).stream()
        .map(BoxDetector::getBoxSurrounding)
        .flatMap(Optional::stream)
        .filter(box -> isValidSize(box, scanDistance))
        .distinct()
        .sorted(Comparator.comparingInt(BoxDetector::getArea).reversed())
        .toList();

    PandasBlueprints.LOGGER.info("Found {} possible bounding boxes...",
        possibleBoxes.size());

    return possibleBoxes.stream()
        .filter(box -> hasValidPerimeter(box, validFrameBlockFunction))
        .findFirst();
  }

  /**
   * @return True if the perimeter of this box contains scaffolding.
   */
  public static boolean hasValidPerimeter(BlockBox box,
      Function<BlockPos, Boolean> validFrameBlockFunction) {

    final BlockPos maxPos = BoxDetector.getMaxPos(box);
    final BlockPos minPos = BoxDetector.getMinPos(box);

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

  public static boolean isValidSize(BlockBox box, int maxSideLength) {
    return box.getBlockCountX() <= maxSideLength
        && box.getBlockCountY() <= maxSideLength
        && box.getBlockCountZ() <= maxSideLength;
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

  public static int getArea(BlockBox box) {
    return box.getBlockCountX() * box.getBlockCountY() * box.getBlockCountZ();
  }

}