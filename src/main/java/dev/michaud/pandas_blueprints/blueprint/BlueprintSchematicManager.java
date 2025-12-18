package dev.michaud.pandas_blueprints.blueprint;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.path.PathUtil;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlueprintSchematicManager extends PersistentState {

  private static final Pattern NAME_WITH_COUNT = Pattern.compile("(?<name>.*) _(?<count>\\d+)",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private static final Pattern RESERVED_WINDOWS_NAMES = Pattern.compile(
      ".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?",
      Pattern.CASE_INSENSITIVE);

  public static final Codec<ConcurrentMap<Identifier, BlueprintSchematic>> CONCURRENT_MAP_CODEC =
      Codec.unboundedMap(Identifier.CODEC, BlueprintSchematic.CODEC)
          .xmap(ConcurrentHashMap::new, Function.identity());

  public static final Codec<BlueprintSchematicManager> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
          CONCURRENT_MAP_CODEC
              .fieldOf("schematics")
              .forGetter(manager -> manager.schematicMap)
      ).apply(instance, BlueprintSchematicManager::new));

  public static final PersistentStateType<BlueprintSchematicManager> TYPE = new PersistentStateType<>(
      PandasBlueprints.GREENPANDA_ID + "_schematics",
      BlueprintSchematicManager::new,
      CODEC,
      null
  );

  private final ConcurrentMap<Identifier, BlueprintSchematic> schematicMap;

  private BlueprintSchematicManager() {
    this(new ConcurrentHashMap<>());
  }

  private BlueprintSchematicManager(ConcurrentMap<Identifier, BlueprintSchematic> schematicMap) {
    this.schematicMap = schematicMap;
  }

  public static BlueprintSchematicManager getState(ServerWorld world) {
    return getState(world.getServer());
  }

  public static BlueprintSchematicManager getState(MinecraftServer server) {
    final ServerWorld world = server.getWorld(ServerWorld.OVERWORLD);
    assert world != null;

    return world.getPersistentStateManager().getOrCreate(TYPE);
  }

  /**
   * Get a schematic with the given identifier, if it exists.
   *
   * @param id The id of the schematic
   * @return The schematic, or empty if it doesn't exist or couldn't be found
   */
  public Optional<BlueprintSchematic> getSchematic(@Nullable Identifier id) {
    if (id != null) {
      return Optional.ofNullable(schematicMap.get(id));
    }

    return Optional.empty();
  }

  public List<Identifier> getAllSchematicIds() {
    return ImmutableList.sortedCopyOf(schematicMap.keySet());
  }

  /**
   * Add a new schematic
   *
   * @param schematic The schematic to save
   * @param name      The of the blueprint
   * @return The identifier of the new schematic
   */
  public @Nullable Identifier saveSchematic(@NotNull BlueprintSchematic schematic,
      @NotNull String name) {

    final String namespace = PandasBlueprints.GREENPANDA_ID;
    final Identifier identifier = getNextUnusedIdentifier(namespace, name);

    schematicMap.put(identifier, schematic);
    markDirty();

    return identifier;
  }

  /**
   * Get an identifier that isn't already associated with a blueprint. If the given values are
   * already absent, the return value is equivalent to {@code Identifier.of(namespace, name)}.
   * Otherwise, the name is appended with {@literal "_<number>"}, increasing until a unique name is
   * found.
   *
   * @param namespace The namespace to use
   * @param name      The proposed name
   * @return A unique identifier.
   */
  public @NotNull Identifier getNextUnusedIdentifier(String namespace, String name) {

    final String baseName = formatBlueprintName(name);
    final Identifier baseId = Identifier.of(namespace, baseName);

    if (!schematicMap.containsKey(baseId)) {
      return baseId; // We're good!
    }

    // Extract existing count, if present
    String path = baseName;
    int count = 1;

    final Matcher matcher = NAME_WITH_COUNT.matcher(baseName);
    if (matcher.matches()) {
      path = matcher.group("name");
      count = Integer.parseInt(matcher.group("count"));
    }

    // Find a new value
    Identifier candidate;
    do {
      candidate = baseId.withPath(path + "_" + count);
      count += 1;
    } while (schematicMap.containsKey(candidate));

    return candidate;
  }

  /**
   * Format the given string so that it is a valid name.
   *
   * @param name The string to format
   * @return A valid blueprint name
   * @implNote Converts the string to lowercase and replaces all non-alphanumeric characters (a-Z,
   * 0-9) other than underscores and hyphens with an underscore. Also checks that the string doesn't
   * match any reserved Windows file names.
   */
  public static String formatBlueprintName(String name) {
    name = PathUtil.replaceInvalidChars(name.toLowerCase())
        .replaceAll("(?![a-z0-9._-]).", "_");

    if (RESERVED_WINDOWS_NAMES.matcher(name).matches()) {
      name = "_" + name + "_";
    }

    return name;
  }

}