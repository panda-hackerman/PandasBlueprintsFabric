package dev.michaud.pandas_blueprints.blueprint;

import com.mojang.datafixers.DataFixer;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.util.BlueprintPathUtil;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.nbt.InvalidNbtException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.FixedBufferInputStream;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.PathUtil;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlueprintSchematicManager {

  private final DataFixer dataFixer;
  private final RegistryEntryLookup<Block> blockLookup;
  private final Path generatedPath; // File path to ./world/generated/

  private ResourceManager resourceManager;

  public Map<Identifier, BlueprintSchematic> schematicMap = new ConcurrentHashMap<>();

  public BlueprintSchematicManager(ResourceManager resourceManager, Session session,
      DataFixer dataFixer, RegistryEntryLookup<Block> blockLookup) {
    this.resourceManager = resourceManager;
    this.dataFixer = dataFixer;
    this.blockLookup = blockLookup;
    this.generatedPath = session.getDirectory(WorldSavePath.GENERATED).normalize();
  }

  public void setResourceManager(ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
  }

  /**
   * Get a schematic with the given identifier. If it isn't already loaded, will attempt to load it
   * from the file system.
   *
   * @param id The id of the schematic
   * @return The schematic, or empty if it doesn't exist or couldn't be found
   */
  public Optional<BlueprintSchematic> getSchematic(@Nullable Identifier id) {

    if (id == null) {
      return Optional.empty();
    }

    if (schematicMap.containsKey(id)) {
      return Optional.of(schematicMap.get(id));
    }

    return loadSchematic(id);
  }

  /**
   * Load a schematic from the file system
   *
   * @param id The id of the schematic
   * @return The schematic, or empty if it couldn't be loaded
   */
  public Optional<BlueprintSchematic> loadSchematic(Identifier id) {

    final Path path = getPath(id);

    if (!Files.exists(path)) {
      return Optional.empty();
    }

    try (InputStream inStream = new FixedBufferInputStream(new FileInputStream(path.toFile()))) {
      NbtCompound readNbt = NbtIo.readCompressed(inStream, NbtSizeTracker.ofUnlimitedBytes());
      BlueprintSchematic schematic = BlueprintSchematic.readNbt(blockLookup, readNbt);

      return Optional.of(schematic);
    } catch (IOException | InvalidNbtException e) {
      PandasBlueprints.LOGGER.error("Couldn't open blueprint: {}.\n{}", id, e);
    }

    return Optional.empty();
  }

  /**
   * Save a schematic to the file system
   *
   * @param schematic The schematic to save
   * @return The identifier of the new schematic
   */
  public @Nullable Identifier saveSchematic(@NotNull BlueprintSchematic schematic, @NotNull String name) {

    final String namespace = PandasBlueprints.GREENPANDA_ID;

    final NbtCompound schematicNbt = schematic.writeNbt(new NbtCompound());

    final Identifier identifier;
    final Path path;
    final Path parent;

    try {
      final String uniqueName = BlueprintPathUtil.getNextUniqueName(getParent(namespace), name, ".nbt");

      identifier = Identifier.of(namespace, uniqueName);
      path = getPath(identifier);
      parent = path.getParent();
    } catch (IOException e) {
      PandasBlueprints.LOGGER.error("Ran into an IO problem while trying to save schematic: ", e);
      return null;
    } catch (InvalidIdentifierException e) {
      PandasBlueprints.LOGGER.error("Invalid identifier trying to save schematic: ", e);
      return null;
    }

    if (parent == null) {
      PandasBlueprints.LOGGER.error("Couldn't save the schematic, the parent folder is null!");
      return null;
    }

    // Create parent
    try {
      Path create = Files.exists(parent) ? parent.toRealPath() : parent;
      Files.createDirectories(create);
    } catch (IOException e) {
      PandasBlueprints.LOGGER.error("Couldn't save the schematic, failed to create the parent directory!");
      return null;
    }

    // Write to file
    try (OutputStream outStream = new FileOutputStream(path.toFile())) {
      NbtIo.writeCompressed(schematicNbt, outStream);
    } catch (IOException e) {
      PandasBlueprints.LOGGER.error("Couldn't save the schematic, failed to write to file at {}", path);
      return null;
    }

    schematicMap.put(identifier, schematic);
    return identifier;
  }

  /**
   * Gets the path to the blueprint folder of this namespace
   *
   * @param namespace The namespace of the blueprint folder
   * @return A path that points to {@code ./world/generated/<namespace>/blueprints/}
   *
   * @see BlueprintSchematicManager#getPath(Identifier)
   */
  protected Path getParent(@NotNull String namespace) {
    try {
      Path blueprintFolder = generatedPath
          .resolve(namespace)
          .resolve("blueprints");

      if (blueprintFolder.startsWith(generatedPath)
          && PathUtil.isAllowedName(blueprintFolder)) {
        return blueprintFolder;
      } else {
        throw new InvalidIdentifierException("Invalid path: " + blueprintFolder);
      }
    } catch (InvalidPathException e) {
      throw new InvalidIdentifierException("Invalid path with namespace" + namespace);
    }
  }

  /**
   * Gets the path to the blueprint file with this id.
   *
   * @param id The identifier of the blueprint
   * @return A path that points to {@code ./world/generated/<namespace>/blueprints/<path>.nbt}
   * @see BlueprintSchematicManager#getParent(String)
   */
  protected Path getPath(@NotNull Identifier id) {
    try {
      // ./world/generated/<namespace>/blueprints/
      Path blueprintFolder = generatedPath
          .resolve(id.getNamespace())
          .resolve("blueprints");

      // Points to filename.nbt in the blueprint folder (^^^)
      Path filePath = PathUtil.getResourcePath(blueprintFolder, id.getPath(), ".nbt").normalize();

      if (filePath.startsWith(generatedPath)
          && PathUtil.isAllowedName(filePath)) {
        return filePath;
      } else {
        throw new InvalidIdentifierException("Invalid path: " + filePath);
      }
    } catch (InvalidPathException e) {
      throw new InvalidIdentifierException("Invalid path: " + id, e);
    }
  }

}