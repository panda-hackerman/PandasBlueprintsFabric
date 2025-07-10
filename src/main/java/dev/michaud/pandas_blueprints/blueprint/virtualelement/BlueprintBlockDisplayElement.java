package dev.michaud.pandas_blueprints.blueprint.virtualelement;

import com.google.common.collect.ImmutableList;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic.BlueprintBlockInfo;
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
import org.joml.Matrix4f;

public class BlueprintBlockDisplayElement extends BlockDisplayElement {

  public enum BlockStateMatch {
    /** One or both of the blocks are air */
    AIR,
    /** The block and state are the same */
    EXACT_MATCH,
    /** The blocks are the same, but the states are different */
    BLOCK_MATCH_ONLY,
    /** The blocks (and presumably states) are different */
    NO_BLOCK_MATCH
  }

  /**
   * Classes that don't work normally when using block displays
   */
  private static final List<Class<? extends BlockEntityProvider>> BROKEN_BLOCK_DISPLAY_CLASSES = ImmutableList.copyOf(
      List.of(BedBlock.class, ChestBlock.class, WallSkullBlock.class, WallSignBlock.class,
          WallHangingSignBlock.class, WallBannerBlock.class, SkullBlock.class, SignBlock.class,
          HangingSignBlock.class, BannerBlock.class, ShulkerBoxBlock.class));

  public static final int NORMAL_GLOW_COLOR = 0x0CA6E8; /* Blue */
  public static final int BLOCK_MISMATCH_GLOW_COLOR = 0xD42020; /* Red */
  public static final int STATE_MISMATCH_GLOW_COLOR = 0xff7b00; /* Orange */

  private final boolean useCustomTransformation;
  private final BlueprintBlockInfo blockInfo;
  private BlockRotation rotation; // Rotation around the table pos
  private BlockState worldState; // What block state this element is drawing over

  public BlueprintBlockDisplayElement(@NotNull BlueprintBlockInfo blockInfo) {
    super(blockInfo.state());

    this.blockInfo = blockInfo;
    this.rotation = BlockRotation.NONE;
    this.useCustomTransformation = doesBlockEntityIgnoreRotation(blockInfo.state().getBlock());

    setGlowing(true);
    setGlowColorOverride(NORMAL_GLOW_COLOR);
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

    final BlockState blockState = blockInfo.state().rotate(rotation);

    switch (getBlockStateMatch(blockState, worldState)) {
      case AIR -> setRenderNormally(blockState);
      case EXACT_MATCH -> setStopRendering();
      case BLOCK_MATCH_ONLY -> setRenderStateMismatch();
      case NO_BLOCK_MATCH -> setRenderFullMismatch();
    }

    super.tick();
  }

  public void setRenderNormally(BlockState state) {
    setInvisible(false);
    setBlockState(state);
    setGlowColorOverride(NORMAL_GLOW_COLOR);

    if (useCustomTransformation) {
      setTransformation(RotationHelper.getSpecialBlockEntityRotation(state));
    }
  }

  public void setStopRendering() {
    setInvisible(true);
    setBlockState(Blocks.AIR.getDefaultState());
    setTransformation(new Matrix4f());
  }

  public void setRenderStateMismatch() {
    setInvisible(false);
    setBlockState(Blocks.ORANGE_STAINED_GLASS.getDefaultState());
    setGlowColorOverride(STATE_MISMATCH_GLOW_COLOR);
    setTransformation(new Matrix4f());
  }

  public void setRenderFullMismatch() {
    setInvisible(false);
    setBlockState(Blocks.RED_STAINED_GLASS.getDefaultState());
    setGlowColorOverride(BLOCK_MISMATCH_GLOW_COLOR);
    setTransformation(new Matrix4f());
  }

  @Override
  public Vec3d getOffset() {
    return Vec3d.of(getBlockOffset());
  }

  public BlockPos getBlockOffset() {
    return blockInfo.pos().rotate(rotation);
  }

  public BlockPos getWorldPos(BlockPos origin) {
    return origin.add(getBlockOffset());
  }

  public void setRotation(BlockRotation newRotation) {
    if (rotation != newRotation) {
      rotation = newRotation;
    }
  }

  public void setWorldState(BlockState state) {
    if (worldState == null || !worldState.equals(state)) {
      worldState = state;
    }
  }

  public static BlockStateMatch getBlockStateMatch(BlockState virtualBlock, BlockState realBlock) {

    if (realBlock.isAir()) {
      return BlockStateMatch.AIR;
    }

    if (realBlock.equals(virtualBlock)) {
      return BlockStateMatch.EXACT_MATCH;
    }

    if (realBlock.getBlock().equals(virtualBlock.getBlock())) {
      return BlockStateMatch.BLOCK_MATCH_ONLY;
    }

    return BlockStateMatch.NO_BLOCK_MATCH;
  }

  /**
   * Check if the given block has a block entity that ignores the block state rotation when
   * displayed using a BlockDisplayEntity (such as chests, skulls, etc.)
   *
   * @param block The block to check
   * @return True if the block is a block entity and the display is broken
   * @implNote The block classes affected are listed in
   * {@link BlueprintBlockDisplayElement#BROKEN_BLOCK_DISPLAY_CLASSES}; if the block isn't a Polymer
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