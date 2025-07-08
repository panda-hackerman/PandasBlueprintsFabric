package dev.michaud.pandas_blueprints.blocks;

import com.mojang.serialization.MapCodec;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.entity.BlueprintTableBlockEntity;
import dev.michaud.pandas_blueprints.blocks.entity.ModBlockEntityTypes;
import dev.michaud.pandas_blueprints.items.EmptyBlueprintItem;
import dev.michaud.pandas_blueprints.items.FilledBlueprintItem;
import dev.michaud.pandas_blueprints.items.wrench.CopperWrenchItem;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

/**
 * Table that lets you display blueprints
 */
public class BlueprintTableBlock extends BlockWithEntity implements PolymerTexturedBlock {

  /**
   * If this block has a blueprint or not
   */
  public static final BooleanProperty HAS_BLUEPRINT = BooleanProperty.of("has_blueprint");
  public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
  public static final MapCodec<BlueprintTableBlock> CODEC = createCodec(BlueprintTableBlock::new);

  /* Block Models */
  public static final PolymerBlockModel BLOCK_MODEL_EMPTY = PolymerBlockModel.of(
      Identifier.of(PandasBlueprints.GREENPANDA_ID, "block/blueprint_table"));
  public static final PolymerBlockModel BLOCK_MODEL_WITH_BLUEPRINT = PolymerBlockModel.of(
      Identifier.of(PandasBlueprints.GREENPANDA_ID, "block/blueprint_table_filled"));

  public static final BlockState POLYMER_BLOCK_STATE_EMPTY;
  public static final BlockState POLYMER_BLOCK_STATE_WITH_BLUEPRINT;

  // Find an unused model
  static {
    final BlockModelType typeEmpty = BlockModelType.FULL_BLOCK;
    final BlockModelType typeFull;

    if (PolymerBlockResourceUtils.getBlocksLeft(BlockModelType.TRANSPARENT_BLOCK) > 1) {
      typeFull = BlockModelType.TRANSPARENT_BLOCK;
    } else {
      typeFull = BlockModelType.CACTUS_BLOCK;
    }

    POLYMER_BLOCK_STATE_EMPTY = PolymerBlockResourceUtils.requestBlock(typeEmpty,
        BLOCK_MODEL_EMPTY);
    POLYMER_BLOCK_STATE_WITH_BLUEPRINT = PolymerBlockResourceUtils.requestBlock(typeFull,
        BLOCK_MODEL_WITH_BLUEPRINT);
  }

  public BlueprintTableBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState().with(HAS_BLUEPRINT, false));
  }

  /**
   * Generate a schematic and turn an empty blueprint into a filled blueprint with the generated id
   *
   * @see BlueprintTableBlockEntity#saveSchematic(String, ServerWorld, BlockBox, BlockPos)
   */
  protected boolean tryFillBlueprint(PlayerEntity player, Hand hand, @NotNull World world,
      BlockPos pos, ItemStack stack) {

    if (!(world instanceof ServerWorld serverWorld)) {
      return false;
    }

    // -- Get the blueprint name from the item, if it's renamed. Otherwise, use the default.
    final Text itemCustomName = stack.getCustomName();
    final String blueprintName;

    if (itemCustomName != null && itemCustomName.getLiteralString() != null) {
      blueprintName = itemCustomName.getLiteralString().toLowerCase();
    } else {
      blueprintName = "blueprint";
    }

    // -- Create the schematic outline
    final Optional<BlockBox> outline = BlueprintTableBlockEntity.getOutline(world, pos);

    if (outline.isEmpty()) {
      player.sendMessage(
          Text.translatable("block.pandas_blueprints.blueprint_table.invalid_structure"), true);
      return false;
    }

    // -- Save the file...
    final Identifier blueprintId = BlueprintTableBlockEntity.saveSchematic(blueprintName,
        serverWorld, outline.get(), pos);

    if (blueprintId == null) {
      player.sendMessage(Text.translatable("block.pandas_blueprints.blueprint_table.internal_error")
          .formatted(Formatting.RED), true);
      return false; // If the id is null that means something went wrong saving...
    }

    // -- Create filled blueprint and remove empty blueprint
    final ItemStack filled = FilledBlueprintItem.createBlueprint(blueprintId, player);

    stack.decrementUnlessCreative(1, player);

    if (stack.isEmpty()) {
      player.setStackInHand(hand, filled);
    } else {
      boolean inserted = player.getInventory().insertStack(filled.copy());
      if (!inserted) {
        player.dropItem(filled, false);
      }
    }

    return true;
  }

  /**
   * Set if this block has a blueprint
   *
   * @see BlueprintTableBlock#HAS_BLUEPRINT
   */
  protected void setHasBlueprint(LivingEntity player, BlockState state, World world, BlockPos pos,
      boolean has_blueprint) {
    BlockState newState = state.with(HAS_BLUEPRINT, has_blueprint);
    world.setBlockState(pos, newState, Block.NOTIFY_ALL);
    world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, Emitter.of(player, state));
  }

  /**
   * Drop the current blueprint
   *
   * @return If the blueprint was actually dropped
   */
  protected boolean dropBlueprint(World world, BlockPos pos) {
    if (!(world.getBlockEntity(pos) instanceof BlueprintTableBlockEntity blockEntity)) {
      return false;
    }

    final ItemStack itemStack = blockEntity.getBlueprint().copy();
    final ItemEntity itemEntity = new ItemEntity(world,
        pos.getX() + 0.5,
        pos.getY() + 1,
        pos.getZ() + 0.5,
        itemStack);

    itemEntity.setToDefaultPickupDelay();
    world.spawnEntity(itemEntity);
    blockEntity.clear();

    return true;
  }

  /**
   * Store the given blueprint
   *
   * @return If the blueprint was actually stored
   */
  protected boolean putBlueprint(LivingEntity player, World world, BlockPos pos,
      ItemStack blueprint) {
    if (!(world.getBlockEntity(pos) instanceof BlueprintTableBlockEntity blockEntity)) {
      return false;
    }

    blockEntity.setBlueprint(blueprint.splitUnlessCreative(1, player));
    world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1, 1);

    return true;
  }

  @Override
  public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
    if (state.get(HAS_BLUEPRINT)) {
      return POLYMER_BLOCK_STATE_WITH_BLUEPRINT;
    } else {
      return POLYMER_BLOCK_STATE_EMPTY;
    }
  }

  @Override
  protected MapCodec<? extends BlueprintTableBlock> getCodec() {
    return CODEC;
  }

  @Override
  protected void appendProperties(Builder<Block, BlockState> builder) {
    builder.add(HAS_BLUEPRINT, FACING);
  }

  @Override
  protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
      BlockHitResult hit) {

    if (world.getBlockEntity(pos) instanceof BlueprintTableBlockEntity blockEntity
        && blockEntity.hasBlueprint()) {
      if (dropBlueprint(world, pos)) {
        setHasBlueprint(player, state, world, pos, false);
        return ActionResult.SUCCESS;
      }
    }

    return ActionResult.PASS;
  }

  @Override
  protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
      PlayerEntity player, Hand hand, BlockHitResult hit) {

    if (!(world.getBlockEntity(pos) instanceof BlueprintTableBlockEntity blockEntity)) {
      return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
    }

    final Item item = stack.getItem();

    if (item instanceof CopperWrenchItem) {
      return ActionResult.PASS; // Allow for rotation
    }

    // Put filled blueprint
    if (blockEntity.isEmpty() && item instanceof FilledBlueprintItem) {
      if (putBlueprint(player, world, pos, stack)) {
        setHasBlueprint(player, state, world, pos, true);
        return ActionResult.SUCCESS;
      }
    }

    // Use empty blueprint
    if (blockEntity.isEmpty() && item instanceof EmptyBlueprintItem) {
      if (tryFillBlueprint(player, hand, world, pos, stack)) {
        return ActionResult.SUCCESS;
      }
    }

    return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
  }

  @Override
  protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {

    if (world.getBlockEntity(pos) instanceof BlueprintTableBlockEntity blockEntity) {
      ItemScatterer.spawn(world, pos, blockEntity);
      world.updateComparators(pos, this);
      blockEntity.onDestroy();
    }

    super.onStateReplaced(state, world, pos, moved);
  }

  @Override
  protected BlockState rotate(BlockState state, BlockRotation rotation) {
    return state.with(FACING, rotation.rotate(state.get(FACING)));
  }

  @Override
  protected BlockState mirror(BlockState state, BlockMirror mirror) {
    return state.rotate(mirror.getRotation(state.get(FACING)));
  }

  @Override
  public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new BlueprintTableBlockEntity(pos, state);
  }

  @Override
  public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,
      BlockState state, BlockEntityType<T> type) {
    return validateTicker(type, ModBlockEntityTypes.BLUEPRINT_TABLE,
        BlueprintTableBlockEntity::tick);
  }

  @Override
  protected boolean hasComparatorOutput(BlockState state) {
    return true;
  }

  @Override
  protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
    if (world.getBlockEntity(pos) instanceof BlueprintTableBlockEntity blockEntity) {
      return blockEntity.hasBlueprint() ? 15 : 0;
    } else {
      return 0;
    }
  }

  /**
   * Held {@link BlueprintTableBlock} Item
   */
  public static class BlueprintTableItem extends PolymerBlockItem {

    public BlueprintTableItem(Block block, Settings settings) {
      super(block, settings);
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
      if (PolymerResourcePackUtils.hasMainPack(context)) {
        return Identifier.of(PandasBlueprints.GREENPANDA_ID, "blueprint_table");
      } else {
        return Identifier.ofVanilla("crafting_table");
      }
    }
  }
}