package dev.michaud.pandas_blueprints.blueprint.virtualelement;

import com.google.common.collect.ImmutableList;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic.BlueprintBlockInfo;
import dev.michaud.pandas_blueprints.blueprint.virtualelement.BlueprintHighlight.BlockStateMatch;
import dev.michaud.pandas_blueprints.util.RotationHelper;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import java.util.List;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
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
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class BlueprintBlockDisplay extends BlockDisplayElement {

  /**
   * Classes that don't work normally when using block displays
   */
  private static final List<Class<? extends BlockEntityProvider>> BROKEN_BLOCK_DISPLAY_CLASSES =
      ImmutableList.copyOf(List.of(
          BedBlock.class, ChestBlock.class, WallSkullBlock.class, WallSignBlock.class,
          WallHangingSignBlock.class, WallBannerBlock.class, SkullBlock.class, SignBlock.class,
          HangingSignBlock.class, BannerBlock.class, ShulkerBoxBlock.class));

  public final boolean useCustomTransformation;
  public final BlueprintBlockInfo blockInfo;

  private BlockStateMatch match = BlockStateMatch.NO_BLOCK_MATCH;
  private BlockRotation rotation = BlockRotation.NONE; // Rotation around the table pos

  public BlueprintBlockDisplay(@NotNull BlueprintBlockInfo blockInfo) {
    super(blockInfo.state());

    this.blockInfo = blockInfo;
    this.useCustomTransformation = doesBlockEntityIgnoreRotation(blockInfo.state().getBlock());

    setBrightness(new Brightness(15, 15));
    setShadowRadius(0);
  }

  @Override
  public void tick() {

    if (getBlockState() == null) {
      PandasBlueprints.LOGGER.warn("Block display state set to null! That shouldn't happen...");
      setBlockState(blockInfo.state()); // Default
      return;
    }

    final BlockState state = getBlueprintBlockState();

    if (match == BlockStateMatch.AIR) {
      setRender(state);
    } else {
      setStopRendering();
    }

    super.tick();
  }

  private void setRender(BlockState state) {
    setInvisible(false);
    setBlockState(state);

    if (useCustomTransformation) {
      setTransformation(RotationHelper.getSpecialBlockEntityRotation(state));
    }
  }

  private void setStopRendering() {
    setInvisible(true);
    setBlockState(Blocks.AIR.getDefaultState());
  }

  @Override
  public Vec3d getOffset() {
    return Vec3d.of(getBlockOffset());
  }

  public BlockPos getBlockOffset() {
    return blockInfo.pos().rotate(rotation);
  }

  public BlockState getBlueprintBlockState() {
    return blockInfo.state().rotate(rotation);
  }

  public BlockStateMatch getMatch() {
    return match;
  }

  public BlockRotation getRotation() {
    return rotation;
  }

  public void setBlockMatch(BlockStateMatch match) {
    this.match = match;
  }

  public void setRotation(BlockRotation newRotation) {
    if (rotation != newRotation) {
      rotation = newRotation;
    }
  }

  /**
   * Check if the given block has a block entity that ignores the block state rotation when
   * displayed using a BlockDisplayEntity (such as chests, skulls, etc.)
   *
   * @param block The block to check
   * @return True if the block is a block entity and the display is broken
   * @implNote The block classes affected are listed in
   * {@link BlueprintBlockDisplay#BROKEN_BLOCK_DISPLAY_CLASSES}; if the block isn't a Polymer
   * block and is an instance of one of the classes in the list, we treat it as broken. Though it's
   * highly likely to vary by version, this is actually intended behavior, so it may not ever be
   * "fixed".
   * @see <a href="https://report.bugs.mojang.com/servicedesk/customer/portal/2/MC-259954">
   * MC-259954</a>
   * @see <a href="https://report.bugs.mojang.com/servicedesk/customer/portal/2/MC-259990">
   * MC-259990</a>
   */
  private static boolean doesBlockEntityIgnoreRotation(Block block) {
    if (block instanceof PolymerBlock) {
      return false; // Polymer should handle this...
    }

    if (!(block instanceof BlockEntityProvider)) {
      return false;
    }

    return BROKEN_BLOCK_DISPLAY_CLASSES.stream()
        .anyMatch(clazz -> clazz.isInstance(block));
  }

}