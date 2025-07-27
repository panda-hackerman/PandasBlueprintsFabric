package dev.michaud.pandas_blueprints.blueprint.virtualelement;

import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class BlueprintHighlight extends ItemDisplayElement {

  public enum BlockStateMatch {
    /**
     * One or both of the blocks are air
     */
    AIR(0x0CA6E8),
    /**
     * The block and state are the same
     */
    EXACT_MATCH(null),
    /**
     * The blocks are the same, but the states are different
     */
    BLOCK_MATCH_ONLY(0xff7b00),
    /**
     * The blocks (and presumably states) are different
     */
    NO_BLOCK_MATCH(0xD42020);

    public final Integer color;

    BlockStateMatch(final Integer color) {
      this.color = color;
    }

    public static BlockStateMatch from(BlockState virtualBlockState, BlockState realBlockState) {
      if (realBlockState.isAir() || virtualBlockState.isAir()) {
        return BlockStateMatch.AIR;
      }

      if (realBlockState.equals(virtualBlockState)) {
        return BlockStateMatch.EXACT_MATCH;
      }

      if (realBlockState.getBlock().equals(virtualBlockState.getBlock())) {
        return BlockStateMatch.BLOCK_MATCH_ONLY;
      }

      return BlockStateMatch.NO_BLOCK_MATCH;
    }
  }

  public static final Identifier ITEM_MODEL = Identifier.of(PandasBlueprints.GREENPANDA_ID, "highlight_block");
  public static final Map<BlockStateMatch, ItemStack> DISPLAY_ITEM_FOR_MATCH;

  static {
    final EnumMap<BlockStateMatch, ItemStack> map = new EnumMap<>(BlockStateMatch.class);

    for (BlockStateMatch match : BlockStateMatch.values()) {
      map.put(match, buildDisplayItemForMatch(match));
    }

    DISPLAY_ITEM_FOR_MATCH = ImmutableMap.copyOf(map);
  }

  private final BlueprintBlockDisplay blockDisplay;

  public BlueprintHighlight(BlueprintBlockDisplay blockDisplay) {
    this.blockDisplay = blockDisplay;

    setBrightness(new Brightness(15, 15));
    setShadowRadius(0);
  }

  @Override
  public void tick() {

    final BlockStateMatch match = blockDisplay.getMatch();
    final ItemStack item = DISPLAY_ITEM_FOR_MATCH.get(match);

    setItem(item);

    if (match.color != null) {
      setGlowing(true);
      setGlowColorOverride(match.color);
    } else {
      setGlowing(false);
    }

    super.tick();
  }

  @Override
  public Vec3d getOffset() {
    return blockDisplay.getOffset().add(0.5, 0.5, 0.5);
  }

  private static ItemStack buildDisplayItemForMatch(BlockStateMatch match) {

    if (match.color == null) {
      return ItemStack.EMPTY;
    }

    final ItemStack item = new ItemStack(Items.LEATHER_HORSE_ARMOR);

    item.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(match.color));
    item.set(DataComponentTypes.ITEM_MODEL, ITEM_MODEL);

    return item;
  }
}