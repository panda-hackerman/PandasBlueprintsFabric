package dev.michaud.pandas_blueprints.blueprint.virtualelement;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.entity.BlueprintTableBlockEntity;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic.BlueprintBlockInfo;
import dev.michaud.pandas_blueprints.blueprint.virtualelement.BlueprintHighlight.BlockStateMatch;
import dev.michaud.pandas_blueprints.util.RotationHelper;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * A "virtual" (packet-only) element holder that simulates block displays based on a Blueprint.
 */
public class VirtualSchematicDisplayElement extends ElementHolder {

  private final BlueprintSchematic schematic;
  private final BlueprintTableBlockEntity blockEntity;
  private final ManualAttachment attachment;

  private final Set<BlueprintBlockDisplay> blockDisplays;
  private final Map<BlueprintBlockDisplay, BlueprintHighlight> blockHighlights;

  private BlockRotation rotation = BlockRotation.NONE;
//  private BlockBox cachedBoundingBox = null;

  public VirtualSchematicDisplayElement(@NotNull BlueprintSchematic schematic,
      @NotNull BlueprintTableBlockEntity blockEntity) {
    this.schematic = schematic;
    this.blockEntity = blockEntity;
    this.blockDisplays = createBlockDisplays();
    this.blockHighlights = createBlockHighlights();

    final ServerWorld world = (ServerWorld) blockEntity.getWorld();
    final Supplier<Vec3d> posSupplier = () -> Vec3d.of(blockEntity.getPos());

    this.attachment = new ManualAttachment(this, world, posSupplier);
  }

  @Override
  public void tick() {
    final World world = blockEntity.getWorld();
    final BlockPos pos = blockEntity.getPos();

    if (world == null || getWatchingPlayers().isEmpty()) {
      return;
    }

    blockDisplays.forEach(blockDisplay -> {

      blockDisplay.setRotation(rotation);

      BlockPos worldPos = pos.add(blockDisplay.getBlockOffset());
      BlockState worldState = world.getBlockState(worldPos);
      BlockState blueprintState = blockDisplay.getBlueprintBlockState();

      BlockStateMatch match = BlockStateMatch.from(blueprintState, worldState);

      blockDisplay.setBlockMatch(match);
    });

    super.tick();
  }

  protected Set<BlueprintBlockDisplay> createBlockDisplays() {

    final ImmutableSet.Builder<BlueprintBlockDisplay> builder = ImmutableSet.builder();

    for (final BlueprintBlockInfo info : schematic) {

      final BlockState state = info.state();

      if (state.isAir()) {
        continue;
      }

      if (state.contains(Properties.BED_PART)
          && state.get(Properties.BED_PART) == BedPart.FOOT) {
        continue;
      }

      final BlueprintBlockDisplay element = new BlueprintBlockDisplay(info, schematic.getOffset());
      element.setRotation(rotation);

      if (addElementWithoutUpdates(element)) {
        builder.add(element);
      }
    }

    return builder.build();
  }

  protected Map<BlueprintBlockDisplay, BlueprintHighlight> createBlockHighlights() {

    final ImmutableMap.Builder<BlueprintBlockDisplay, BlueprintHighlight> builder = ImmutableMap
        .builderWithExpectedSize(blockDisplays.size());

    for (final BlueprintBlockDisplay display : blockDisplays) {
      final BlueprintHighlight element = new BlueprintHighlight(display);

      if (addElementWithoutUpdates(element)) {
        builder.put(display, element);
      }
    }

    return builder.build();
  }

  // Getters and setters
  public void setRotation(BlockRotation rotation) {
    this.rotation = rotation;
  }

  @Override
  public @NotNull ManualAttachment getAttachment() {
    return attachment;
  }

  // Util
  /**
   * @return The bounding box of this schematic in the world
   */
  public BlockBox getBoundingBox() {

    final Vec3i size = schematic.getSize();
    final BlockPos offset = schematic.getOffset();
    final BlockPos origin = blockEntity.getPos();

    final BlockPos minPos = origin.add(RotationHelper.rotate(offset, rotation));
    final BlockPos maxPos = minPos.add(RotationHelper.rotate(size, rotation));

    return BlockBox.create(minPos, maxPos);
  }

}