package dev.michaud.pandas_blueprints.blueprint;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A schematic that is saved from a blueprint
 */
public class BlueprintSchematic {

  public static final int NBT_VERSION = 1;
  private final List<BlueprintBlockInfo> blockInfoList;

  protected BlueprintSchematic(List<BlueprintBlockInfo> blockInfoList) {
    this.blockInfoList = blockInfoList;
  }

  /**
   * Creates a new schematic with the blocks in the specified area
   */
  @Contract(value = "_, _, _-> new", pure = true)
  public static @NotNull BlueprintSchematic create(@NotNull World world,
      @NotNull BlockPos startingPos, @NotNull Vec3i size) {

    if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1) {
      throw new IllegalArgumentException(
          "The bounding box must be at least 1 block in every direction");
    }

    final ImmutableList.Builder<BlueprintBlockInfo> builder = ImmutableList.builder();
    final BlockPos cornerPos = startingPos.add(size);

    for (BlockPos pos : BlockPos.iterate(startingPos, cornerPos)) {
      final BlockState state = world.getBlockState(pos);
      final BlockEntity blockEntity = world.getBlockEntity(pos);
      final BlueprintBlockInfo blockInfo = BlueprintBlockInfo.of(world, pos, state, blockEntity);

      builder.add(blockInfo);
    }

    return new BlueprintSchematic(builder.build());
  }

  public List<BlueprintBlockInfo> getBlockInfoList() {
    return blockInfoList;
  }

  public NbtCompound writeNbt(NbtCompound nbt) {

    if (blockInfoList.isEmpty()) {
      //Empty
      return nbt;
    }



  }

  public record BlueprintBlockInfo(@NotNull BlockPos pos, @NotNull BlockState state,
                                   @Nullable NbtCompound nbt) {

    @Contract(value = "_, _, _, _ -> new", pure = true)
    public static @NotNull BlueprintBlockInfo of(@NotNull World world, @NotNull BlockPos pos,
        @NotNull BlockState state, @Nullable BlockEntity entity) {
      final DynamicRegistryManager registryManager = world.getRegistryManager();
      final NbtCompound nbt = (entity == null) ? null : entity.createNbtWithId(registryManager);

      return new BlueprintBlockInfo(pos, state, nbt);
    }
  }

  public static final class BlueprintBlockInfoList {

    private final List<BlueprintBlockInfo> blockInfoList;
    private final Map<Block, List<BlueprintBlockInfo>> blockToInfos = new HashMap<>();

    BlueprintBlockInfoList(List<BlueprintBlockInfo> blockInfoList) {
      this.blockInfoList = blockInfoList;
    }

    public List<BlueprintBlockInfo> getAll() {
      return blockInfoList;
    }

    public List<BlueprintBlockInfo> getAllOf(Block block) {
      return blockToInfos.computeIfAbsent(block, b -> blockInfoList.stream().filter(info -> info.state().isOf(b)).toList());
    }

  }

}