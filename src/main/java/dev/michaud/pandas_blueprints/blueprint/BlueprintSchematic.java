package dev.michaud.pandas_blueprints.blueprint;

import com.google.common.collect.ImmutableList;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.util.BoxDetector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.InvalidNbtException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.math.BlockBox;
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
  private final Map<Block, List<BlueprintBlockInfo>> blockToInfos = new HashMap<>();
  private final Vec3i size;

  protected BlueprintSchematic(List<BlueprintBlockInfo> blockInfoList, Vec3i size) {
    this.blockInfoList = blockInfoList;
    this.size = size;
  }

  /**
   * Creates a new schematic
   * @param world The world
   * @param box The bounding box to save
   * @param tablePos The position of the blueprint table
   * @return A new schematic that stores the blocks in the given box
   */
  @Contract(value = "_, _, _-> new", pure = true)
  public static @NotNull BlueprintSchematic create(@NotNull World world, @NotNull BlockBox box, @NotNull BlockPos tablePos) {

    final ImmutableList.Builder<BlueprintBlockInfo> builder = ImmutableList.builder();

    final BlockPos maxCorner = BoxDetector.getMaxPos(box);
    final BlockPos minCorner = BoxDetector.getMinPos(box);

    for (BlockPos pos : BlockPos.iterate(minCorner, maxCorner)) {

      if (pos.equals(tablePos)) {
        continue;
      }

      final BlockPos offsetPos = pos.subtract(tablePos).toImmutable();

      final BlockState state = world.getBlockState(pos);
      final BlockEntity blockEntity = world.getBlockEntity(pos);
      final BlueprintBlockInfo blockInfo = BlueprintBlockInfo.of(world, offsetPos, state, blockEntity);

      builder.add(blockInfo);
    }

    return new BlueprintSchematic(builder.build(), BoxDetector.getSize(box));
  }

  public List<BlueprintBlockInfo> getAll() {
    return blockInfoList;
  }

  public List<BlueprintBlockInfo> getAllOf(Block block) {
    return blockToInfos.computeIfAbsent(block,
        b -> blockInfoList.stream().filter(info -> info.state().isOf(b)).toList());
  }

  public Vec3i getSize() {
    return size;
  }

  /**
   * Write this schematic to nbt
   *
   * @param nbt The nbt to write to.
   * @return The given {@code nbt} after writing this schematic as nbt to it
   */
  @Contract("_ -> param1")
  public NbtCompound writeNbt(@NotNull NbtCompound nbt) {

    final Palette palette = new Palette(); /* Updated in blocks loop */

    // BUILD BLOCKS NBT
    final NbtList blocks = new NbtList();

    for (BlueprintBlockInfo blockInfo : blockInfoList) {

      final NbtCompound block = new NbtCompound();

      final int[] infoPos = new int[]{blockInfo.pos.getX(), blockInfo.pos.getY(), blockInfo.pos.getZ()};
      final int infoStateId = palette.getIdOrCreate(blockInfo.state);
      final @Nullable NbtCompound infoNbt = blockInfo.nbt;

      block.putIntArray("pos", infoPos);
      block.putInt("state", infoStateId);

      if (infoNbt != null) {
        block.put("nbt", infoNbt);
      }

      blocks.add(block);
    }

    // SET PALETTE NBT
    final NbtList paletteStates = new NbtList();

    for (BlockState state : palette) {
      paletteStates.add(NbtHelper.fromBlockState(state));
    }

    nbt.put("blocks", blocks);
    nbt.put("palette", paletteStates);
    nbt.putIntArray("size", new int[]{size.getX(), size.getY(), size.getZ()});
    nbt.putInt("DataVersion", NBT_VERSION);

    return nbt;
  }

  /**
   * Create a schematic from nbt
   * @param blockLookup Block lookup to use
   * @param nbt Nbt to read from
   * @return A new blueprint schematic
   */
  @Contract("_, _ -> new")
  public static @NotNull BlueprintSchematic readNbt(RegistryEntryLookup<Block> blockLookup,
      @NotNull NbtCompound nbt) {

    final int version = nbt.getInt("DataVersion");

    if (version == 0) {
      throw new InvalidNbtException("Unknown data version: " + version);
    }

    if (version > NBT_VERSION) {
      PandasBlueprints.LOGGER.warn("Trying to deserialize future version...");
    }

    // Generate palette
    final Palette palette = new Palette();
    final NbtList paletteStates = nbt.getList("palette", NbtElement.COMPOUND_TYPE);

    for (int i = 0; i < paletteStates.size(); i++) {
      NbtCompound compound = paletteStates.getCompound(i);
      palette.set(NbtHelper.toBlockState(blockLookup, compound), i);
    }

    // Blocks
    final NbtList blocks = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);

    final ImmutableList.Builder<BlueprintBlockInfo> builder = ImmutableList.builder();

    for (int i = 0; i < blocks.size(); i++) {

      final NbtCompound nbtBlock = blocks.getCompound(i);

      final int[] posArr = nbtBlock.getIntArray("pos");
      final BlockPos blockPos = new BlockPos(posArr[0], posArr[1], posArr[2]);
      final BlockState state = palette.getState(nbtBlock.getInt("state"));
      final NbtCompound blockNbt = nbtBlock.contains("nbt") ? nbtBlock.getCompound("nbt") : null;

      builder.add(new BlueprintBlockInfo(blockPos, state, blockNbt));
    }

    // Size
    final int[] sizeArray = nbt.getIntArray("size");
    final Vec3i size = new Vec3i(sizeArray[0], sizeArray[1], sizeArray[2]);

    return new BlueprintSchematic(builder.build(), size);
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

  static final class Palette implements Iterable<BlockState> {

    private final IdList<BlockState> ids = new IdList<>();
    private int currentIndex = 0;

    public int getIdOrCreate(BlockState state) {

      int id = ids.getRawId(state);

      if (id == -1) { // Doesn't exist yet
        id = currentIndex++;
        ids.set(state, id);
      }

      return id;
    }

    public BlockState getState(int id) {
      BlockState state = ids.get(id);
      return state == null ? Blocks.AIR.getDefaultState() : state;
    }

    public void set(BlockState state, int id) {
      ids.set(state, id);
    }

    @Override
    public @NotNull Iterator<BlockState> iterator() {
      return ids.iterator();
    }
  }

}