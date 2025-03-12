package dev.michaud.pandas_blueprints.blocks;

import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;

public interface ScaffoldingBlockMaxDistanceHolder {
  default IntProperty getDistanceProperty() {
    return ScaffoldingBlock.DISTANCE;
  }

  default int getMaxDistance() {
    return 7;
  }
}
