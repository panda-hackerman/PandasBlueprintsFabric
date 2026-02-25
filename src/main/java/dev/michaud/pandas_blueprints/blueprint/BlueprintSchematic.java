package dev.michaud.pandas_blueprints.blueprint;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic.BlueprintBlockInfo;
import dev.michaud.pandas_blueprints.tags.ModBlockTags;
import dev.michaud.pandas_blueprints.util.BoxDetector;
import dev.michaud.pandas_blueprints.util.CodecFormatUtil;
import dev.michaud.pandas_blueprints.util.VarIntUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.InvalidNbtException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.ErrorReporter.Logging;
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
public class BlueprintSchematic implements Iterable<BlueprintBlockInfo> {

  public static final int VERSION = 1;
  public static final int MIN_SUPPORTED_VERSION = 1;
  public static final int DATA_VERSION = SharedConstants.getGameVersion().dataVersion().id();
  public static final int MIN_SUPPORTED_DATA_VERSION = 4438; //TODO: Verify

  private final List<BlueprintBlockInfo> blockInfos;
  private final Object2IntMap<Block> blockCounts;
  private final Vec3i size;
  private final BlockPos offset;

  public static final Codec<BlueprintSchematic> CODEC = NbtCompound.CODEC.comapFlatMap(
      BlueprintSchematic::readNbtSafe, BlueprintSchematic::writeNbtSafe);

  protected BlueprintSchematic(List<BlueprintBlockInfo> blockInfos, Vec3i size, BlockPos offset) {
    this.blockInfos = blockInfos;
    this.size = size;
    this.offset = offset;

    blockCounts = buildBlockCounts(blockInfos);
  }

  private static Object2IntMap<Block> buildBlockCounts(List<BlueprintBlockInfo> blockInfos) {
    final Object2IntMap<Block> blockCount = new Object2IntOpenHashMap<>();

    for (BlueprintBlockInfo info : blockInfos) {
      Block block = info.state().getBlock();
      int count = blockCount.getInt(block);

      blockCount.put(block, count + 1);
    }

    return Object2IntMaps.unmodifiable(blockCount);
  }

  public Vec3i getSize() {
    return size;
  }

  public BlockPos getOffset() {
    return offset;
  }

  public Set<Block> getAllBlocks() {
    return blockCounts.keySet();
  }

  public int getCount(Block block) {
    return blockCounts.getInt(block);
  }

  /**
   * Creates a new schematic
   *
   * @param world    The world
   * @param box      The bounding box to save
   * @param tablePos The position of the blueprint table
   * @return A new schematic that stores the blocks in the given box
   */
  @Contract(value = "_, _, _-> new", pure = true)
  public static @NotNull BlueprintSchematic create(@NotNull World world, @NotNull BlockBox box,
      @NotNull BlockPos tablePos) {

    final ImmutableList.Builder<BlueprintBlockInfo> builder = ImmutableList.builder();

    final BlockPos maxCorner = BoxDetector.getMaxPos(box);
    final BlockPos minCorner = BoxDetector.getMinPos(box);

    for (BlockPos pos : BlockPos.iterate(minCorner, maxCorner)) {

      if (pos.equals(tablePos)) {
        continue;
      }

      final BlockState state = world.getBlockState(pos);

      if (state.isIn(ModBlockTags.BLUEPRINT_IGNORES)) {
        continue;
      }

      final BlockPos offsetPos = pos.subtract(minCorner).toImmutable();
      final BlockEntity blockEntity = world.getBlockEntity(pos);
      final BlueprintBlockInfo blockInfo = BlueprintBlockInfo.of(offsetPos, state, blockEntity);

      builder.add(blockInfo);
    }

    final Vec3i size = BoxDetector.getSize(box);
    final BlockPos offset = minCorner.subtract(tablePos);

    return new BlueprintSchematic(builder.build(), size, offset);
  }

  public static DataResult<BlueprintSchematic> readNbtSafe(@NotNull NbtCompound nbt) {
    try {
      return DataResult.success(readNbt(nbt));
    } catch (IOException e) {
      return DataResult.error(() -> "Ran into an I/O problem: " + e.getMessage());
    } catch (InvalidNbtException | UnsupportedOperationException e) {
      return DataResult.error(() -> "Got malformed or invalid NBT: " + e.getMessage());
    }
  }

  public NbtCompound writeNbtSafe() {
    try {
      return writeNbt(new NbtCompound(), false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write this schematic to nbt
   *
   * @param nbt                 The nbt to write to.
   * @param saveBlockEntityData If false, block entity data is omitted from the output.
   * @return The given {@code nbt} after writing this schematic as nbt to it
   */
  @Contract("_, _ -> param1")
  public NbtCompound writeNbt(NbtCompound nbt, boolean saveBlockEntityData) throws IOException {

    final int width = size.getX();
    final int height = size.getY();
    final int length = size.getZ();

    final NbtCompound blockData = new NbtCompound();

    if (!blockInfos.isEmpty()) {
      final BlockPalette blockPalette = new BlockPalette();
      final NbtList blockEntities = new NbtList();

      final List<List<Integer>> idToPositions = new ArrayList<>();

      for (BlueprintBlockInfo blockInfo : blockInfos) {
        final int x = blockInfo.pos.getX();
        final int y = blockInfo.pos.getY();
        final int z = blockInfo.pos.getZ();

        final int pos = (x) + (z * width) + (y * width * length);
        final int id = blockPalette.getIdOrCreate(blockInfo.state);

        while (idToPositions.size() <= id) {
          idToPositions.add(new ArrayList<>());
        }

        idToPositions.get(id).add(pos);

        if (blockInfo.hasBlockEntityData() && saveBlockEntityData) {
          NbtCompound blockEntity = new NbtCompound();
          blockEntity.put("Data", blockInfo.nbt);
          blockEntity.put("Pos", BlockPos.CODEC, blockInfo.pos);

          blockEntities.add(blockEntity);
        }
      }

      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      for (List<Integer> positions : idToPositions) {
        VarIntUtil.writeVarInt(positions.size(), buffer);
        VarIntUtil.writeVarInts(positions, buffer);
      }

      blockData.put("Palette", BlockPalette.CODEC, blockPalette);
      blockData.putByteArray("Data", buffer.toByteArray());

      if (!blockEntities.isEmpty()) {
        blockData.put("BlockEntities", blockEntities);
      }
    }

    nbt.put("Blocks", blockData);
    nbt.put("Width", CodecFormatUtil.UNSIGNED_SHORT, width);
    nbt.put("Height", CodecFormatUtil.UNSIGNED_SHORT, height);
    nbt.put("Length", CodecFormatUtil.UNSIGNED_SHORT, length);

    if (!offset.equals(Vec3i.ZERO)) {
      nbt.put("Offset", BlockPos.CODEC, offset);
    }

    nbt.putInt("Version", VERSION);
    nbt.putInt("DataVersion", DATA_VERSION);

    return nbt;
  }

  /**
   * Create a schematic from nbt
   *
   * @param nbt Nbt to read from
   * @return A new blueprint schematic
   * @throws InvalidNbtException           If the NBT is malformed or of an unsupported version.
   * @throws UnsupportedOperationException If the data version is higher than the current version.
   */
  public static BlueprintSchematic readNbt(@NotNull NbtCompound nbt) throws IOException {

    final int version = nbt.getInt("Version")
        .orElseThrow(() -> new InvalidNbtException("Missing panda's blueprint version"));

    final int dataVersion = nbt.getInt("DataVersion")
        .orElseThrow(() -> new InvalidNbtException("Missing data version"));

    //region Check data version
    if (version > VERSION) {
      throw new UnsupportedOperationException(
          "Cannot read future Panda's Blueprints version: " + version + " (I'm still on version "
              + VERSION + "!)");
    }

    if (dataVersion > DATA_VERSION) {
      throw new UnsupportedOperationException(
          "Cannot read future Minecraft data version: " + dataVersion + " (I'm still on version "
              + DATA_VERSION + "!)");
    }

    if (version < MIN_SUPPORTED_VERSION) {
      throw new InvalidNbtException(
          "Unsupported Panda's Blueprints version: " + version + " (minimum supported is "
              + MIN_SUPPORTED_VERSION + ")");
    }

    if (dataVersion < MIN_SUPPORTED_DATA_VERSION) {
      throw new InvalidNbtException(
          "Unsupported Minecraft data version: " + dataVersion + " (minimum supported is "
              + MIN_SUPPORTED_DATA_VERSION + ")");
    }
    //endregion

    final int width = nbt.get("Width", CodecFormatUtil.UNSIGNED_SHORT).orElse(0);
    final int height = nbt.get("Height", CodecFormatUtil.UNSIGNED_SHORT).orElse(0);
    final int length = nbt.get("Length", CodecFormatUtil.UNSIGNED_SHORT).orElse(0);

    if (width <= 0 || height <= 0 || length <= 0) {
      throw new InvalidNbtException(
          String.format("Invalid blueprint size! Got %d/%d/%d", width, height, length));
    }

    final Vec3i size = new Vec3i(width, height, length);
    final BlockPos offset = nbt.get("Offset", BlockPos.CODEC).orElse(BlockPos.ORIGIN);

    final NbtCompound blockData = nbt.getCompoundOrEmpty("Blocks");

    if (blockData.isEmpty()) {
      PandasBlueprints.LOGGER.warn("Empty blueprint!");
      return new BlueprintSchematic(ImmutableList.of(), size, offset);
    }

    // Get block stuff
    final BlockPalette palette = blockData.get("Palette", BlockPalette.CODEC)
        .orElseThrow(() -> new InvalidNbtException("No block palette!"));

    final byte[] data = blockData.getByteArray("Data")
        .orElseThrow(() -> new InvalidNbtException("Invalid block data!"));

    final NbtList blockEntities = blockData.getListOrEmpty("BlockEntities");

    final ByteArrayInputStream in = new ByteArrayInputStream(data);
    ImmutableList.Builder<BlueprintBlockInfo> builder = ImmutableList.builder();

    int id = 0;
    while (in.available() > 0) {
      int listSize = VarIntUtil.readVarInt(in);

      for (int i = 0; i < listSize; i++) {
        int packedPos = VarIntUtil.readVarInt(in);

        int base = packedPos % (width * length);
        final BlockPos pos = new BlockPos(
            base % width,
            packedPos / (width * length),
            base / width
        );

        final BlockState state = palette.get(id);

        //TODO: Block Entities
        builder.add(new BlueprintBlockInfo(pos, state, null));
      }

      id++;
    }

    return new BlueprintSchematic(builder.build(), size, offset);
  }

  @Override
  public @NotNull Iterator<BlueprintBlockInfo> iterator() {
    return blockInfos.iterator();
  }

  /**
   * Information about a specific block in a blueprint
   *
   * @param pos   The block's relative position
   * @param state The block state
   * @param nbt   The block's nbt, if any.
   */
  public record BlueprintBlockInfo(@NotNull BlockPos pos, @NotNull BlockState state,
                                   @Nullable NbtCompound nbt) {

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull BlueprintBlockInfo of(@NotNull BlockPos pos,
        @NotNull BlockState state, @Nullable BlockEntity entity) {
      return new BlueprintBlockInfo(pos, state, createBlockEntityData(entity));
    }

    public boolean hasBlockEntityData() {
      return nbt != null && !nbt.isEmpty();
    }

    public static @Nullable NbtCompound createBlockEntityData(@Nullable BlockEntity entity) {
      if (entity == null) {
        return null;
      }

      try (ErrorReporter.Logging logger = new Logging(entity.getReporterContext(),
          PandasBlueprints.LOGGER)) {
        final NbtWriteView writeView = NbtWriteView.create(logger);
        entity.writeDataWithId(writeView);

        return writeView.getNbt();
      }
    }
  }

}