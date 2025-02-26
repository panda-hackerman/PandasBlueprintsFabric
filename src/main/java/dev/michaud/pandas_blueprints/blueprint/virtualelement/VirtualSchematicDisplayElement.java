package dev.michaud.pandas_blueprints.blueprint.virtualelement;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import dev.michaud.pandas_blueprints.blocks.entity.BlueprintTableBlockEntity;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic.BlueprintBlockInfo;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import java.util.function.Supplier;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.entity.decoration.Brightness;
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

public class VirtualSchematicDisplayElement extends ElementHolder {

  public static final int GLOW_COLOR = 829160;
  public static final int WRONG_GLOW_COLOR = 13901856;

  private final BlueprintSchematic schematic;
  private final BlueprintTableBlockEntity blockEntity;
  private final ManualAttachment attachment;

  private final BiMap<BlueprintBlockInfo, BlockDisplayElement> blockDisplays;

  private Direction facing = Direction.NORTH;

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

    final Builder<BlueprintBlockInfo, BlockDisplayElement> builder = ImmutableBiMap
        .builderWithExpectedSize(size.getX() * size.getY() * size.getX());

    for (final BlueprintBlockInfo block : schematic.getAll()) {

      final BlockState state = block.state();

      if (state.isAir()) {
        continue;
      }

      if (state.contains(Properties.BED_PART)
          && state.get(Properties.BED_PART) == BedPart.FOOT) {
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

  @Override
  public void tick() {

    final World world = blockEntity.getWorld();
    final BlockPos origin = blockEntity.getPos();

    if (world == null || getWatchingPlayers().isEmpty()) {
      return;
    }

    blockDisplays.forEach((info, element) -> {

      final BlockState state = getBlockStateFacing(facing, info.state());
      final Vec3i offset = getBlockPositionFacing(facing, info.pos());
      final BlockPos pos = origin.add(offset);
      final BlockState stateAtPos = world.getBlockState(pos);

      if (stateAtPos.isAir()) {
        // State is air, so display normally
        element.setBlockState(state);
        element.setGlowColorOverride(GLOW_COLOR);
        element.setInvisible(false);
      } else if (stateAtPos.equals(state)) {
        // State is not air, and it's the correct block, so we don't need to display anything
        element.setBlockState(Blocks.AIR.getDefaultState());
        element.setInvisible(true);
      } else {
        // State is not air, but it's the wrong block, so show that it's wrong.
        element.setBlockState(Blocks.RED_STAINED_GLASS.getDefaultState());
        element.setInvisible(true);
        element.setGlowColorOverride(WRONG_GLOW_COLOR);
      }
    });

    super.tick();
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

  protected BlueprintBlockInfo getInfoFromElement(BlockDisplayElement entity) {
    return getBlockDisplays().inverse().get(entity);
  }

  protected BlockDisplayElement getElementFromInfo(BlueprintBlockInfo info) {
    return getBlockDisplays().get(info);
  }

  public Direction getFacing() {
    return facing;
  }

  public void setFacing(Direction facing) {

    if (this.facing != facing) {
      blockDisplays.forEach((info, element) -> {
        Vec3i offset = getBlockPositionFacing(facing, info.pos());
        element.setOffset(Vec3d.of(offset));
      });
    }

    this.facing = facing;
  }

  /** @return The bounding box of this schematic in the world */
  public BlockBox getBoundingBox() {
    final Vec3i size = schematic.getSize();
    final BlockPos origin = blockEntity.getPos();
    final BlockPos corner = origin.add(getBlockPositionFacing(facing, size));

    return BlockBox.create(origin, corner);
  }

  /**
   * Get rotation a block entity with this state should have.
   * Normally, this is 0, but some block entities don't render properly.
   * @param state The state to check
   * @return The yaw and pitch
   */
  public static Vec2f getSpecialBlockEntityRotation(BlockState state) {

    float yaw = 0;
    float pitch = 0;

    final Block block = state.getBlock();

    if (block instanceof PolymerBlock) {
      return Vec2f.ZERO; // Polymer should handle this
    }

    // Horizontal facing
    if ((block instanceof BedBlock || block instanceof ChestBlock || block instanceof WallSkullBlock
        || block instanceof WallSignBlock || block instanceof WallHangingSignBlock
        || block instanceof WallBannerBlock)
        && state.contains(Properties.HORIZONTAL_FACING)) {

      Direction facing = state.get(Properties.HORIZONTAL_FACING);
      yaw = switch (facing) {
        case NORTH -> 180;
        case EAST -> 270;
        case SOUTH -> 0;
        case WEST -> 90;
        default -> throw new IllegalStateException("Unexpected value: " + facing);
      };
    }

    // 1-16 Rotation
    if ((block instanceof SkullBlock || block instanceof SignBlock || block instanceof HangingSignBlock
        || block instanceof BannerBlock)
        && state.contains(Properties.ROTATION)) {
      yaw = (float) (state.get(Properties.ROTATION) * 22.5) - 180;
    }

    // Horizontal + Vertical facing
    if ((block instanceof ShulkerBoxBlock)
        && state.contains(Properties.FACING)) {
      switch (state.get(Properties.FACING)) {
        case DOWN -> yaw = 180; // TODO: Quaternions :'(
        case UP -> yaw = 0;
        case NORTH -> pitch = -90;
        case SOUTH -> pitch = 90;
        case WEST -> {
          pitch = 90;
          yaw = 90;
        }
        case EAST -> {
          pitch = 90;
          yaw = -90;
        }
      }
    }

    return new Vec2f(yaw, pitch);
  }

  public static @NotNull Vec3i getBlockPositionFacing(Direction facing, Vec3i originalPosition) {
    final int x = originalPosition.getX();
    final int y = originalPosition.getY();
    final int z = originalPosition.getZ();

    return switch (facing) {
      case NORTH -> new Vec3i(x, y, z);          // No rotation
      case EAST -> new Vec3i(-z, y, x);   // 90
      case SOUTH -> new Vec3i(-x, y, -z); // 180
      case WEST -> new Vec3i(z, y, -x);   // -90
      default -> throw new IllegalArgumentException("Unexpected value: " + facing);
    };
  }

  public static @NotNull BlockState getBlockStateFacing(Direction facing, BlockState originalState) {
    return switch (facing) {
      case NORTH -> originalState;
      case EAST -> originalState.rotate(BlockRotation.CLOCKWISE_90);
      case SOUTH -> originalState.rotate(BlockRotation.CLOCKWISE_180);
      case WEST -> originalState.rotate(BlockRotation.COUNTERCLOCKWISE_90);
      default -> throw new IllegalArgumentException("Unexpected value: " + facing);
    };
  }

}