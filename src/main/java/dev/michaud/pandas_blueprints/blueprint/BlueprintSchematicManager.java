package dev.michaud.pandas_blueprints.blueprint;

import com.mojang.datafixers.DataFixer;
import java.nio.file.Path;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;

public class BlueprintSchematicManager {

  private final DataFixer dataFixer;
  private final RegistryEntryLookup<Block> blockLookup;
  private final Path generatedPath;

  private ResourceManager resourceManager;

  BlueprintSchematicManager(ResourceManager resourceManager, LevelStorage.Session session, DataFixer dataFixer, RegistryEntryLookup<Block> blockLookup) {
    this.resourceManager = resourceManager;
    this.dataFixer = dataFixer;
    this.blockLookup = blockLookup;
    this.generatedPath = session.getDirectory(WorldSavePath.GENERATED).normalize();
  }

  public void setResourceManager(ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
  }

  public Identifier saveSchematic(BlueprintSchematic schematic) {

  }

}