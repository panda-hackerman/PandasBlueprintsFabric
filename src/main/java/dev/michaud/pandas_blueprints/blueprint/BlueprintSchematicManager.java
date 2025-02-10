package dev.michaud.pandas_blueprints.blueprint;

import com.mojang.datafixers.DataFixer;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.PathUtil;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

public class BlueprintSchematicManager {

  private final DataFixer dataFixer;
  private final RegistryEntryLookup<Block> blockLookup;
  private final Path generatedPath;

  private ResourceManager resourceManager;

  public BlueprintSchematicManager(ResourceManager resourceManager, LevelStorage.Session session,
      DataFixer dataFixer, RegistryEntryLookup<Block> blockLookup) {
    this.resourceManager = resourceManager;
    this.dataFixer = dataFixer;
    this.blockLookup = blockLookup;
    this.generatedPath = session.getDirectory(WorldSavePath.GENERATED).normalize();
  }

  public void setResourceManager(ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
  }

  public BlueprintSchematic loadSchematic(Identifier id) {
    throw new NotImplementedException("Soon...");
  }

  public Identifier saveSchematic(@NotNull BlueprintSchematic schematic) {

    Identifier identifier = Identifier.of("test", "schematic1");
    NbtCompound schematicNbt = schematic.writeNbt(new NbtCompound());

    final Path path = getPath(identifier, ".nbt");
    final Path parent = path.getParent();

    if (parent == null) {
      throw new RuntimeException("Couldn't save..."); //TODO: More specific exception
    }

    // Create parent
    try {
      Path create = Files.exists(parent) ? parent.toRealPath() : parent;
      Files.createDirectories(create);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create parent directory: " + parent);
    }

    // Write to file
    try (OutputStream outStream = new FileOutputStream(path.toFile())) {
      NbtIo.writeCompressed(schematicNbt, outStream);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write to file..." + path);
    }

    return identifier;
  }

  public Path getPath(Identifier id, String extension) {
    try {
      Path path = generatedPath.resolve(id.getNamespace()).resolve("blueprints");
      path = PathUtil.getResourcePath(path, id.getPath(), extension);

      if (path.startsWith(generatedPath) && PathUtil.isNormal(path) && PathUtil.isAllowedName(
          path)) {
        return path;
      } else {
        throw new InvalidIdentifierException("Invalid path: " + path);
      }
    } catch (InvalidPathException e) {
      throw new InvalidIdentifierException("Invalid path: " + id, e);
    }
  }

}