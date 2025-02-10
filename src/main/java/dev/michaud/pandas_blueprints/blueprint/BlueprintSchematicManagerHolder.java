package dev.michaud.pandas_blueprints.blueprint;

import java.util.Optional;

public interface BlueprintSchematicManagerHolder {
  default Optional<BlueprintSchematicManager> getBlueprintSchematicManager() {
    return Optional.empty();
  }
}
