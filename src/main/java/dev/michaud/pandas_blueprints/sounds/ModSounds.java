package dev.michaud.pandas_blueprints.sounds;

import com.google.common.collect.ImmutableSet;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.BlockWithCustomSounds;
import dev.michaud.pandas_blueprints.blocks.ModBlocks;
import eu.pb4.polymer.soundpatcher.api.SoundPatcher;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

@SuppressWarnings("SameParameterValue")
public class ModSounds {

  public static final SoundEvent COPPER_WRENCH_USE = register("wrench_use");

  private static SoundEvent register(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return SoundEvent.of(id);
  }


  public static void registerSounds() {
    convertSoundsToServerSide(true);
  }

  @SuppressWarnings("UnstableApiUsage")
  private static void convertSoundsToServerSide(boolean log) {
    final Stream<BlockWithCustomSounds> blocks = Stream.of(
        (BlockWithCustomSounds) ModBlocks.BLUEPRINT_TABLE,
        (BlockWithCustomSounds) ModBlocks.COPPER_SCAFFOLDING
    );

    final Set<SoundEvent> toOverride = blocks
        .flatMap(b -> BlockWithCustomSounds.getSoundsToOverride(b).stream())
        .collect(ImmutableSet.toImmutableSet());

    for (final SoundEvent sound : toOverride) {
      if (log) {
        PandasBlueprints.LOGGER.info("Converting sound \"{}\" into a server-side only sound!",
            sound.id());
      }

      SoundPatcher.convertIntoServerSound(sound);
    }
  }
}