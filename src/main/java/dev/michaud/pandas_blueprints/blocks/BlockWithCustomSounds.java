package dev.michaud.pandas_blueprints.blocks;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.block.BlockState;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Helper interface for overriding vanilla sounds and making them server-side.
 */
public interface BlockWithCustomSounds {

  /**
   * Return all possible block states this block can override. Needed so that we can make all the
   * default sounds for this block server-side.
   */
  Set<BlockState> getAllClientBlockStates();

  /**
   * Get the sound groups of every vanilla block this block overrides so we can make them
   * server-side.
   *
   * @return A set of the sound groups we should override
   * @see BlockWithCustomSounds#getSoundsToOverride(BlockWithCustomSounds)
   */
  default @Unmodifiable Set<BlockSoundGroup> getSoundGroupsToOverride() {
    return getAllClientBlockStates().stream()
        .map(AbstractBlockState::getSoundGroup)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Get all the sounds of every vanilla block this block overrides so we can make them
   * server-side.
   *
   * @return A set of the sounds to override for this block
   * @implNote Returns all the unique sounds in every sound group returned by
   * {@link BlockWithCustomSounds#getSoundGroupsToOverride()}
   */
  static @Unmodifiable Set<SoundEvent> getSoundsToOverride(BlockWithCustomSounds block) {
    return block.getSoundGroupsToOverride().stream()
        .flatMap(g -> Stream.of(
            g.getStepSound(),
            g.getBreakSound(),
            g.getFallSound(),
            g.getHitSound(),
            g.getPlaceSound()))
        .collect(ImmutableSet.toImmutableSet());
  }
}