package dev.michaud.pandas_blueprints.blueprint.virtualelement;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.entity.BlueprintTableBlockEntity;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic.BlueprintBlockInfo;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class VirtualSchematicDisplayElement extends ElementHolder {

  public static final int GLOW_COLOR = 829160;
  public static final int WRONG_GLOW_COLOR = 13901856;

  private final BlueprintSchematic schematic;
  private final BlueprintTableBlockEntity blockEntity;
  private final ManualAttachment attachment;

  private final BiMap<BlueprintBlockInfo, BlockDisplayElement> blockDisplays;

  public VirtualSchematicDisplayElement(@NotNull BlueprintSchematic schematic,
      @NotNull BlueprintTableBlockEntity blockEntity) {

    this.schematic = schematic;
    this.blockEntity = blockEntity;
    this.blockDisplays = createBlockDisplays();

    final ServerWorld world = (ServerWorld) blockEntity.getWorld();
    final Supplier<Vec3d> posSupplier = () -> Vec3d.of(blockEntity.getPos());
    this.attachment = new ManualAttachment(this, world, posSupplier);
  }

  protected BiMap<BlueprintBlockInfo, BlockDisplayElement> createBlockDisplays() {

    final Vec3d origin = Vec3d.of(blockEntity.getPos());
    final Vec3i size = schematic.getSize();
    final int numberOfBlocks = size.getX() * size.getY() * size.getX();

    final ImmutableBiMap.Builder<BlueprintBlockInfo, BlockDisplayElement> builder = ImmutableBiMap.builderWithExpectedSize(
        numberOfBlocks);

    int i = 0;

    for (BlueprintBlockInfo block : schematic.getAll()) {

      if (block.state().isAir()) {
        continue;
      }

      final BlockDisplayElement element = new BlockDisplayElement(block.state());

      element.setInitialPosition(origin);
      element.setOffset(Vec3d.of(block.pos()));
      element.setGlowing(true);
      element.setGlowColorOverride(GLOW_COLOR); // Blue color
      element.setBrightness(new Brightness(15, 15));
      element.setShadowRadius(0);

      addElementWithoutUpdates(element);
      builder.put(block, element);
    }

    return builder.build();
  }

  public BlueprintSchematic getSchematic() {
    return schematic;
  }

  public BlueprintTableBlockEntity getBlockEntity() {
    return blockEntity;
  }

  @Override
  public @NotNull ManualAttachment getAttachment() {
    return attachment;
  }

  public BiMap<BlueprintBlockInfo, BlockDisplayElement> getBlockDisplays() {
    return blockDisplays;
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  public void tick() {

    final World world = blockEntity.getWorld();
    final BlockPos origin = blockEntity.getPos();

    if (world == null) {
      return;
    }

    for (BlueprintBlockInfo info : blockDisplays.keySet()) {

      BlockPos pos = origin.add(info.pos());
      BlockState stateAtPos = world.getBlockState(pos);
      BlockDisplayElement element = blockDisplays.get(info);

      if (stateAtPos.isAir()) {
        // State is air, so display normally
        element.setBlockState(info.state());
        element.setGlowColorOverride(GLOW_COLOR);
        element.setInvisible(false);
      } else if (stateAtPos.equals(info.state())) {
        // State is not air, and it's the correct block, so we don't need to display anything
        element.setBlockState(Blocks.AIR.getDefaultState());
        element.setInvisible(true);
      } else {
        // State is not air, but it's the wrong block, so show that it's wrong.
        element.setBlockState(Blocks.RED_STAINED_GLASS.getDefaultState());
        element.setInvisible(true);
        element.setGlowColorOverride(WRONG_GLOW_COLOR);
      }
    }

    super.tick();
  }
}