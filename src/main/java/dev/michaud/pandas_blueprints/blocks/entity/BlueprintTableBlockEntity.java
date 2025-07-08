package dev.michaud.pandas_blueprints.blocks.entity;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.BlueprintTableBlock;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import dev.michaud.pandas_blueprints.blueprint.virtualelement.VirtualSchematicDisplayElement;
import dev.michaud.pandas_blueprints.components.BlueprintIdComponent;
import dev.michaud.pandas_blueprints.items.FilledBlueprintItem;
import dev.michaud.pandas_blueprints.util.BoxDetector;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for {@link BlueprintTableBlock}
 */
public class BlueprintTableBlockEntity extends BlockEntity implements
    BlockEntityInventory<BlueprintTableBlockEntity> {

  public static final int BLUEPRINT_MAX_DISTANCE = 10; // Maximum side length of the blueprint

  private @Nullable Identifier blueprintId;
  private @Nullable VirtualSchematicDisplayElement schematicDisplayElement;

  private final DefaultedList<ItemStack> items = DefaultedList.ofSize(1, ItemStack.EMPTY);

  public BlueprintTableBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntityTypes.BLUEPRINT_TABLE, pos, state);
  }

  public static void tick(World world, BlockPos pos, BlockState state,
      BlueprintTableBlockEntity blockEntity) {

    if (blockEntity.isRemoved()) {
      return;
    }

    // Update block state
    final boolean hasBlueprintState = state.get(BlueprintTableBlock.HAS_BLUEPRINT);
    final boolean hasBlueprintInventory = blockEntity.hasBlueprint();

    if (hasBlueprintState != hasBlueprintInventory) {
      BlockState newState = state.with(BlueprintTableBlock.HAS_BLUEPRINT, hasBlueprintInventory);
      world.setBlockState(pos, newState, Block.NOTIFY_ALL);
      world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, Emitter.of(null, state));
    }

    // Update rotation...
    final Direction facingState = state.get(BlueprintTableBlock.FACING);

    if (blockEntity.schematicDisplayElement != null) {
      blockEntity.schematicDisplayElement.setFacing(facingState);
    }

    // Update blueprint id
    final Identifier blueprintId = hasBlueprintInventory
        ? BlueprintIdComponent.getIdOrNull(blockEntity.getBlueprint())
        : null;

    if (blueprintId != blockEntity.blueprintId) {
      blockEntity.blueprintId = blueprintId; //Updated
      blockEntity.setSchematicDisplay();
    }

    // Update schematic display
    if (blockEntity.schematicDisplayElement != null) {
      blockEntity.schematicDisplayElement.tick();
      blockEntity.showBlueprintToPlayers();
    }

  }

  /**
   * Show blueprint to applicable players
   */
  public void showBlueprintToPlayers() {

    final ServerWorld world = (ServerWorld) getWorld();

    if (schematicDisplayElement == null || world == null) {
      return;
    }

    final Box closeToTableBox = Box.of(Vec3d.of(getPos()), 10, 10, 10);
    final Box closeToHologramBox = Box.from(schematicDisplayElement.getBoundingBox()).expand(5);

    for (ServerPlayerEntity player : world.getPlayers()) {

      Box hitbox = player.getBoundingBox();

      if (closeToHologramBox.intersects(hitbox)
          || closeToTableBox.intersects(hitbox)) {
        schematicDisplayElement.startWatching(player);
      } else {
        schematicDisplayElement.stopWatching(player);
      }
    }
  }

  public void onDestroy() {

    if (schematicDisplayElement != null) {
      schematicDisplayElement.destroy();
    }

    schematicDisplayElement = null;
  }

  /**
   * Change the schematic display to reflect the current blueprint
   */
  protected void setSchematicDisplay() {

    if (schematicDisplayElement != null) {
      schematicDisplayElement.destroy();
    }

    schematicDisplayElement = getCurrentSchematic()
        .map(bp -> new VirtualSchematicDisplayElement(bp, this))
        .orElse(null);
  }


  /**
   * Check if this block at the given position should be considered a valid frame for a schematic
   * perimeter.
   *
   * @param world    The world
   * @param tablePos The table position
   * @param pos      The position to check
   * @return True if the block is valid.
   * @implNote Checks if the block at the given position is a scaffolding block, or if tablePos ==
   * pos.
   */
  public static boolean isValidFrameBlock(World world, BlockPos tablePos, BlockPos pos) {
    return pos.equals(tablePos) || world.getBlockState(pos).getBlock() instanceof ScaffoldingBlock;
  }

  /**
   * Get the outline for a schematic, if a valid perimeter exists.
   *
   * @param world    The world
   * @param tablePos Position of the blueprint table
   * @return The outline, or empty if no valid frame exists.
   */
  public static Optional<BlockBox> getOutline(World world, BlockPos tablePos) {
    return BoxDetector.detectOutlineOf(tablePos, BLUEPRINT_MAX_DISTANCE,
        pos -> isValidFrameBlock(world, tablePos, pos));
  }

  /**
   * Saves an outline to an .nbt file.
   *
   * @param name     The name to use for the file
   * @param world    The world
   * @param outline  The outline of the schematic
   * @param tablePos The position of the blueprint table
   * @return The ID of the saved schematic, or null if it failed to save for some reason.
   */
  public static @Nullable Identifier saveSchematic(@NotNull String name, @NotNull ServerWorld world,
      @NotNull BlockBox outline, @NotNull BlockPos tablePos) {
    final BlueprintSchematic schematic = BlueprintSchematic.create(world, outline, tablePos);
    final BlueprintSchematicManager schematicManager = BlueprintSchematicManager.getState(world);

    return schematicManager.saveSchematic(schematic, name);
  }

  /**
   * Get the schematic that corresponds to the item stored currently stored
   *
   * @return The schematic (or none, if it doesn't exist or couldn't be found)
   */
  public Optional<BlueprintSchematic> getCurrentSchematic() {

    if (!hasBlueprint()
        || getWorld() == null
        || getWorld().getServer() == null) {
      return Optional.empty();
    }

    final Identifier id = BlueprintIdComponent.getIdOrNull(getBlueprint());
    final BlueprintSchematicManager manager = BlueprintSchematicManager.getState(getWorld().getServer());

    return manager.getSchematic(id);
  }

  public ItemStack getBlueprint() {
    return getItems().getFirst();
  }

  public void setBlueprint(ItemStack blueprint) {
    getItems().set(0, blueprint);
    markDirty();
  }

  public boolean hasBlueprint() {
    return !getBlueprint().isEmpty();
  }

  @Override
  public @NotNull DefaultedList<ItemStack> getItems() {
    return items;
  }

  @Override
  public @NotNull BlueprintTableBlockEntity getBlockEntity() {
    return this;
  }

  @Override
  public void clear() {
    setBlueprint(ItemStack.EMPTY);
  }

  @Override
  public boolean isValid(int slot, ItemStack stack) {
    return (stack.getItem() instanceof FilledBlueprintItem)
        && getStack(slot).isEmpty()
        && stack.getCount() <= getMaxCountPerStack();
  }

  @Override
  public int getMaxCountPerStack() {
    return 1;
  }

  @Override
  protected void readData(ReadView view) {
    super.readData(view);

    final ItemStack blueprint = view.read("Blueprint", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    setBlueprint(blueprint);
  }

  @Override
  protected void writeData(WriteView view) {
    super.writeData(view);

    if (hasBlueprint()) {
      view.put("Blueprint", ItemStack.CODEC, getBlueprint());
    }
  }

}