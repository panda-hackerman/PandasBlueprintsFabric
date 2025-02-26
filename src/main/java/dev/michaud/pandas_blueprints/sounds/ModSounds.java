package dev.michaud.pandas_blueprints.sounds;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

  public static final SoundEvent COPPER_WRENCH_USE = registerSoundEvent("wrench_use");

  public static SoundEvent registerSoundEvent(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return SoundEvent.of(id);
  }

  public static void registerSounds() {

  }

}