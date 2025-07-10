package dev.michaud.pandas_blueprints.sounds;

import static eu.pb4.polymer.core.api.block.PolymerBlockUtils.getBlockStateSafely;

import com.google.common.collect.ImmutableSet;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.ModBlocks;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.soundpatcher.api.SoundPatcher;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import xyz.nucleoid.packettweaker.PacketContext;
import xyz.nucleoid.packettweaker.impl.EmptyContext;

@SuppressWarnings("SameParameterValue")
public class ModSounds {

  public static final SoundEvent COPPER_WRENCH_USE = register("wrench_use");

  private static SoundEvent register(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return SoundEvent.of(id);
  }

  @SuppressWarnings("UnstableApiUsage")
  public static void registerSounds() {
    for (SoundEvent sound : getSoundsToMakeServerSide()) {
      SoundPatcher.convertIntoServerSound(sound);
      PandasBlueprints.LOGGER.debug("Converted sound to be server-side: {}", sound.id());
    }
  }

  /**
   * Get all the sound events that can be played by custom blocks, and make them server-side only.
   *
   * @return A list of sounds that should be set to server-side only.
   * @implNote This works by making a list of every possible block state (for each custom block),
   * getting the client-side block for that state, getting the sound group for that block, and
   * throwing all the sounds into a set. Since of course there isn't any real player or packet in
   * this context, we use {@link EmptyContext#INSTANCE}. However, this means that if any custom
   * block has a state that is shown when the player or context is non-null when sent to
   * {@link PolymerBlock#getPolymerBlockState(BlockState, PacketContext)} then this will break. It's
   * kind of a silly way to do things anyway, but I'm pretty positive it won't be a problem anytime
   * soon (surely...)
   */
  private static @NotNull @Unmodifiable Set<SoundEvent> getSoundsToMakeServerSide() {
    final Stream<Block> blocks = Stream.of(ModBlocks.BLUEPRINT_TABLE, ModBlocks.COPPER_SCAFFOLDING);
    final Stream<BlockSoundGroup> soundGroups = blocks
        .flatMap(block -> {
          if (!(block instanceof PolymerBlock polymerBlock)) {
            throw new IllegalStateException(
                "We only need to make vanilla block sounds server side!");
          }

          return block.getStateManager().getStates().stream()
              .map(state -> getBlockStateSafely(polymerBlock, state, EmptyContext.INSTANCE))
              .map(AbstractBlockState::getSoundGroup);
        });

    return soundGroups
        .flatMap(g -> getGroupSounds(g).stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  private static @Unmodifiable Set<SoundEvent> getGroupSounds(BlockSoundGroup group) {
    return ImmutableSet.of(
        group.getStepSound(),
        group.getBreakSound(),
        group.getFallSound(),
        group.getHitSound(),
        group.getPlaceSound());
  }

}