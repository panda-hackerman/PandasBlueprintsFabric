package dev.michaud.pandas_blueprints.sounds;

import static dev.michaud.pandas_blueprints.blocks.BlockWithCustomSounds.getSoundsToOverride;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.BlockWithCustomSounds;
import dev.michaud.pandas_blueprints.blocks.ModBlocks;
import eu.pb4.polymer.core.api.other.PolymerSoundEvent;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import eu.pb4.polymer.soundpatcher.api.SoundPatcher;
import java.util.stream.Stream;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

  public static final SoundEvent COPPER_WRENCH_USE = register("wrench_use");
  public static final SoundEvent BLUEPRINT_FILL = register("blueprint_fill");
  public static final Reference<SoundEvent> HARD_HAT_BLOCK = registerReference("hard_hat_block");

  private static SoundEvent register(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    final SoundEvent event = Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));

    RegistrySyncUtils.setServerEntry(Registries.SOUND_EVENT, event);

    return event;
  }

  private static RegistryEntry.Reference<SoundEvent> registerReference(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    final var event = Registry.registerReference(Registries.SOUND_EVENT, id, SoundEvent.of(id));

    RegistrySyncUtils.setServerEntry(Registries.SOUND_EVENT, id);

    return event;
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