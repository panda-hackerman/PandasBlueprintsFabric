package dev.michaud.pandas_blueprints.damage;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModDamageTypes {

  public static final RegistryKey<DamageType> FALLING_SCAFFOLDING = of("falling_scaffolding");

  private static RegistryKey<DamageType> of(String name) {
    final Identifier id = Identifier.of(PandasBlueprints.GREENPANDA_ID, name);
    return RegistryKey.of(RegistryKeys.DAMAGE_TYPE, id);
  }
}