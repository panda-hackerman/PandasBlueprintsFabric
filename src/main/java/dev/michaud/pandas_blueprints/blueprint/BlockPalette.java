package dev.michaud.pandas_blueprints.blueprint;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import dev.michaud.pandas_blueprints.util.CodecFormatUtil;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.collection.IdList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A "palette" of blocks states. Associates each state with a unique ID, to more efficiently
 * serialize a large number of states.
 *
 * @see BlueprintSchematic
 */
public final class BlockPalette extends IdList<BlockState> {

  public static final Codec<BlockPalette> CODEC = CodecFormatUtil.BLOCK_STATE_TO_STRING
      .listOf().xmap(BlockPalette::copyOf, BlockPalette::getList);

  public BlockPalette() {
    super();
  }

  public BlockPalette(int initialSize) {
    super(initialSize);
  }

  public static @NotNull BlockPalette copyOf(@NotNull List<BlockState> list) {
    final BlockPalette out = new BlockPalette(list.size());

    for (int i = 0; i < list.size(); i++) {
      out.set(list.get(i), i);
    }

    return out;
  }

  public @NotNull @Unmodifiable List<BlockState> getList() {
    return ImmutableList.copyOf(list);
  }

  public int getIdOrCreate(BlockState state) {
    if (!idMap.containsKey(state)) {
      add(state);
    }

    return idMap.getInt(state);
  }

  @Override
  public @NotNull BlockState get(int index) {
    final BlockState state = super.get(index);
    return state == null ? Blocks.AIR.getDefaultState() : state;
  }
}