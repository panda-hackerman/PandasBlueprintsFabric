package dev.michaud.pandas_blueprints.blueprint;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.michaud.pandas_blueprints.tags.ModBlockTags;
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
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
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
  public static final int MIN_SUPPORTED_VERSION = NBT_VERSION;

  private final List<BlueprintBlockInfo> blockInfoList;
  private final Vec3i size;
  private final BlockPos offset;

  public static final Codec<BlueprintSchematic> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
          RegistryOps.getEntryLookupCodec(RegistryKeys.BLOCK),
          NbtCompound.CODEC.fieldOf("data")
              .forGetter(schematic -> schematic.writeNbt(new NbtCompound()))
      ).apply(instance, BlueprintSchematic::readNbt));

  protected BlueprintSchematic(List<BlueprintBlockInfo> blockInfoList, Vec3i size, BlockPos offset) {
    this.blockInfoList = blockInfoList;
    this.size = size;
    this.offset = offset;
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

      final BlockPos offsetPos = pos.subtract(tablePos).toImmutable();
      final BlockEntity blockEntity = world.getBlockEntity(pos);
      final BlueprintBlockInfo blockInfo = BlueprintBlockInfo.of(world, offsetPos, state,
          blockEntity);

      builder.add(blockInfo);
    }

    final Vec3i size = BoxDetector.getSize(box);
    final BlockPos offset = minCorner.subtract(tablePos);

    return new BlueprintSchematic(builder.build(), size, offset);
  }

  public List<BlueprintBlockInfo> getAll() {
    return blockInfoList;
  }

  public Vec3i getSize() {
    return size;
  }

  public BlockPos getOffset() {
    return offset;
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
    nbt.put("size", Vec3i.CODEC, size);
    nbt.put("offset", BlockPos.CODEC, offset);

    nbt.putInt("DataVersion", NBT_VERSION);

    return nbt;
  }

  /**
   * Create a schematic from nbt
   *
   * @param blockLookup Block lookup to use
   * @param nbt         Nbt to read from
   * @return A new blueprint schematic
   * @throws InvalidNbtException           If the NBT is malformed or of an unsupported version.
   * @throws UnsupportedOperationException If the data version is higher than the current version.
   */
  @Contract("_, _ -> new")
  public static @NotNull BlueprintSchematic readNbt(RegistryEntryLookup<Block> blockLookup,
      @NotNull NbtCompound nbt) {

    // Check data version
    final int version = nbt.getInt("DataVersion")
        .orElseThrow(() -> new InvalidNbtException("Missing DataVersion tag"));

    if (version > NBT_VERSION) {
      throw new UnsupportedOperationException(
          "Cannot read future version: " + version + " (I'm still on version "
              + NBT_VERSION + "!)");
    }

    if (version < MIN_SUPPORTED_VERSION) {
      throw new InvalidNbtException(
          "Unsupported version: " + version + " (minimum supported is "
              + MIN_SUPPORTED_VERSION + ")");
    }

    // Generate palette
    final Palette palette = new Palette();
    final NbtList paletteStates = nbt.getList("palette")
        .orElseThrow(() -> new InvalidNbtException("Can't find block palette"));

    for (int i = 0; i < paletteStates.size(); i++) {
      NbtCompound compound = paletteStates.getCompound(i).orElseThrow();
      palette.set(NbtHelper.toBlockState(blockLookup, compound), i);
    }

    // Blocks
    final NbtList blocks = nbt.getListOrEmpty("blocks");

    final ImmutableList.Builder<BlueprintBlockInfo> builder = ImmutableList.builder();

    for (int i = 0; i < blocks.size(); i++) {

      final NbtCompound nbtBlock = blocks.getCompound(i)
          .orElseThrow(() -> new InvalidNbtException("Couldn't find block"));

      final int[] posArr = nbtBlock.getIntArray("pos") // Position as an array [x, y, z]
          .orElseThrow(() -> new InvalidNbtException("Missing block position"));

      final int blockStateId = nbtBlock.getInt("state") // Block state id in the palette
          .orElseThrow(() -> new InvalidNbtException("Invalid block state"));

      final NbtCompound blockNbt = nbtBlock.getCompound("nbt") // If the block has nbt data
          .orElse(null);

      final BlockPos blockPos = new BlockPos(posArr[0], posArr[1], posArr[2]);
      final BlockState state = palette.getState(blockStateId);

      builder.add(new BlueprintBlockInfo(blockPos, state, blockNbt));
    }

    final Vec3i size = nbt.get("size", Vec3i.CODEC)
        .orElseThrow(() -> new InvalidNbtException("Missing blueprint size"));
    final BlockPos offset = nbt.get("offset", BlockPos.CODEC)
        .orElse(BlockPos.ORIGIN);

    return new BlueprintSchematic(builder.build(), size, offset);
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

    @Contract(value = "_, _, _, _ -> new", pure = true)
    public static @NotNull BlueprintBlockInfo of(@NotNull World world, @NotNull BlockPos pos,
        @NotNull BlockState state, @Nullable BlockEntity entity) {

      final DynamicRegistryManager registryManager = world.getRegistryManager();
      final NbtCompound nbt = (entity == null) ? null
          : entity.createNbtWithIdentifyingData(
              registryManager); //TODO: Method to write without pos ?

      return new BlueprintBlockInfo(pos, state, nbt);
    }
  }

  /**
   * A "palette" of blocks states. Associates each state with a unique ID, to more efficiently
   * serialize a large number of states.
   */
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