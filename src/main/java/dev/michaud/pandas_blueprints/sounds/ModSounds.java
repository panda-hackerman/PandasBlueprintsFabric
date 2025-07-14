package dev.michaud.pandas_blueprints.sounds;

import static dev.michaud.pandas_blueprints.blocks.BlockWithCustomSounds.getSoundsToOverride;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.BlockWithCustomSounds;
import dev.michaud.pandas_blueprints.blocks.ModBlocks;
import eu.pb4.polymer.soundpatcher.api.SoundPatcher;
import java.util.stream.Stream;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

  public static final SoundEvent COPPER_WRENCH_USE = register("wrench_use");
  public static final SoundEvent BLUEPRINT_FILL = register("blueprint_fill");

  private static SoundEvent register(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return SoundEvent.of(id);
  }


  public static void registerSounds() {
    convertSoundsToServerSide();
  }

  /**
   * Convert certain sounds to server side, using {@link SoundPatcher}. The sounds that are
   * converted depends on which block states are being used by Polymer.
   *
   * @see BlockWithCustomSounds
   */
  @SuppressWarnings("UnstableApiUsage")
  private static void convertSoundsToServerSide() {
    final Stream<SoundEvent> sounds = Stream.of(
            (BlockWithCustomSounds) ModBlocks.BLUEPRINT_TABLE,
            (BlockWithCustomSounds) ModBlocks.COPPER_SCAFFOLDING)
        .flatMap(block -> getSoundsToOverride(block).stream())
        .distinct();

    sounds.forEach(sound -> {
      SoundPatcher.convertIntoServerSound(sound);
      PandasBlueprints.LOGGER.debug("Converted \"{}\" into a server-side only sound!", sound.id());
    });
  }
}