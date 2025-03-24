package dev.michaud.pandas_blueprints.blocks.scaffolding;

import com.google.common.primitives.Ints;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.BlockView;

public interface ScaffoldingBlockDistanceHolder {

  /** Get the distance property */
  default IntProperty getDistanceProperty() {
    return ScaffoldingBlock.DISTANCE;
  }

  /** Get the max number of the distance property */
  default int getMaxDistance() {
    return 7;
  }

  /** Get the distance before the scaffolding falls */
  default int getFallDistance() {
    return getMaxDistance();
  }

  default int calculateScaffoldingDistance(BlockView world, BlockPos pos) {

    final BlockPos posDown = pos.down().toImmutable();
    final BlockState stateDownOne = world.getBlockState(posDown);

    int distance = getMaxDistance();

    if (stateDownOne.getBlock() instanceof ScaffoldingBlockDistanceHolder downScaffolding) {
      // Standing on Scaffolding
      distance = stateDownOne.get(downScaffolding.getDistanceProperty());
    } else if (stateDownOne.isSideSolidFullSquare(world, posDown, Direction.UP)) {
      // Standing on solid block
      return 0;
    }

    for (final Direction direction : Type.HORIZONTAL) {

      final BlockState stateInDirection = world.getBlockState(pos.offset(direction));

      if (stateInDirection.getBlock() instanceof ScaffoldingBlockDistanceHolder dirScaffolding) {
        int dist = stateInDirection.get(dirScaffolding.getDistanceProperty()) + 1;
        distance = Math.min(distance, dist);

        if (distance == 1) {
          break;
        }
      }
    }

    return Ints.constrainToRange(distance, 0, getMaxDistance());
  }
}