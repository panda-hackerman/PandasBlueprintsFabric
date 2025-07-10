package dev.michaud.pandas_blueprints.blueprint.virtualelement;

import com.google.common.collect.ImmutableSet;
import dev.michaud.pandas_blueprints.blocks.entity.BlueprintTableBlockEntity;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic.BlueprintBlockInfo;
import dev.michaud.pandas_blueprints.util.RotationHelper;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.WallHangingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
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

  private final Set<BlueprintBlockDisplayElement> blockDisplays; //Immutable

  private BlockRotation rotation = BlockRotation.NONE;

  public VirtualSchematicDisplayElement(@NotNull BlueprintSchematic schematic,
      @NotNull BlueprintTableBlockEntity blockEntity) {
    this.schematic = schematic;
    this.blockEntity = blockEntity;
    this.blockDisplays = createBlockDisplays();

    final ServerWorld world = (ServerWorld) blockEntity.getWorld();
    final Supplier<Vec3d> posSupplier = () -> Vec3d.of(blockEntity.getPos());
    this.attachment = new ManualAttachment(this, world, posSupplier);
  }

  @Override
  public void tick() {
    final World world = blockEntity.getWorld();
    final BlockPos worldOrigin = blockEntity.getPos();

    if (world == null || getWatchingPlayers().isEmpty()) {
      return;
    }

    blockDisplays.forEach(element -> {
      element.setRotation(rotation);
      element.setWorldState(world.getBlockState(element.getWorldPos(worldOrigin)));
    });

    super.tick();
  }

  protected Set<BlueprintBlockDisplayElement> createBlockDisplays() {

    final ImmutableSet.Builder<BlueprintBlockDisplayElement> builder = ImmutableSet.builder();

    for (final BlueprintBlockInfo info : schematic.getAll()) {

      final BlockState state = info.state();

      if (state.isAir()) {
        continue;
      }

      if (state.contains(Properties.BED_PART)
          && state.get(Properties.BED_PART) == BedPart.FOOT) {
        continue;
      }

      final BlueprintBlockDisplayElement element = new BlueprintBlockDisplayElement(info);

      if (addElementWithoutUpdates(element)) {
        builder.add(element);
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
    final BlockPos origin = blockEntity.getPos();
    final BlockPos corner = origin.add(RotationHelper.rotate(size, rotation));

    return BlockBox.create(origin, corner);
  }

}