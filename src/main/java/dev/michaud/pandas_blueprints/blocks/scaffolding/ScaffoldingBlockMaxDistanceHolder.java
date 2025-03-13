package dev.michaud.pandas_blueprints.blocks.scaffolding;

import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.state.property.IntProperty;

public interface ScaffoldingBlockMaxDistanceHolder {
  default IntProperty getDistanceProperty() {
    return ScaffoldingBlock.DISTANCE;
  }

  default int getMaxDistance() {
    return 7;
  }
}