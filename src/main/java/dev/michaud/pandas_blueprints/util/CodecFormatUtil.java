package dev.michaud.pandas_blueprints.util;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Splitter;
import com.google.common.base.Splitter.MapSplitter;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

public class CodecFormatUtil {

  public static final Codec<Integer> UNSIGNED_SHORT = Codec.SHORT
      .flatComapMap(
          shortVal -> ((int) shortVal) & 0xFFFF,
          intVal -> {
            if (intVal.compareTo(0) >= 0 && intVal.compareTo(0xFFFF) <= 0) {
              return DataResult.success((short) (int) intVal);
            } else {
              return DataResult.error(
                  () -> "Value " + intVal + " out of range for unsigned short (0-65535)");
            }
          });

  public static final Codec<BlockState> BLOCK_STATE_TO_STRING = Codec.STRING
      .xmap(CodecFormatUtil::stringToBlockState, CodecFormatUtil::blockStateToString);

  private static final MapSplitter SPLITTER = Splitter.on(',').withKeyValueSeparator('=');
  private static final MapJoiner JOINER = Joiner.on(',').withKeyValueSeparator('=');

  public static String blockStateToString(BlockState state) {

    final Block block = state.getBlock();
    final Identifier id = Registries.BLOCK.getId(block);
    final BlockState defaultState = block.getDefaultState();

    final Map<String, String> propertyMap = state.getProperties().stream()
        .map(property -> property.createValue(state))
        .filter(value -> !value.value().equals(defaultState.get(value.property())))
        .collect(ImmutableMap.toImmutableMap(
            val -> val.property().getName(),
            val -> val.property().name(val.value()),
            (first, second) -> first));

    final String propertyString = JOINER.join(propertyMap);
    final String idString = id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();

    if (propertyString.isBlank()) {
      return idString;
    } else {
      return idString + "[" + propertyString + "]";
    }
  }

  public static BlockState stringToBlockState(String string) {
    final String nameStr;
    final String propStr;

    final int propStart = string.indexOf('[');
    final int propEnd = string.indexOf(']');

    if (propStart == -1) {
      nameStr = string;
      propStr = "";
    } else {
      nameStr = string.substring(0, propStart);
      propStr = string.substring(propStart + 1, propEnd);
    }

    final Identifier blockId = Identifier.of(nameStr);
    final RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, blockId);
    final Block block = Registries.BLOCK.getOrThrow(key).value();

    BlockState state = block.getDefaultState();
    var stateManager = block.getStateManager();

    if (!propStr.isBlank()) {
      final Map<String, String> propToVal = SPLITTER.split(propStr);

      for (final Map.Entry<String, String> keyVal : propToVal.entrySet()) {
        final String keyName = keyVal.getKey();
        final String valName = keyVal.getValue();

        final Property<?> property = stateManager.getProperty(keyName);

        if (property == null) {
          throw new IllegalArgumentException(
              "Property " + keyName + " not found on " + nameStr + ".");
        }

        state = withProperty(state, property, valName)
            .orElseThrow(() -> new IllegalArgumentException(
                "Invalid value " + valName + " for property " + keyName + "."));
      }
    }

    return state;
  }

  private static <T extends Comparable<T>> Optional<BlockState> withProperty(BlockState state,
      Property<T> property, String valueString) {
    return property.parse(valueString)
        .map(value -> state.with(property, value));
  }

  private static <T extends Comparable<T>> String getPropertyString(Property<T> property,
      Comparable<?> value) {
    final T val = property.getType().cast(value);
    return property.createValue(val).toString();
  }

}