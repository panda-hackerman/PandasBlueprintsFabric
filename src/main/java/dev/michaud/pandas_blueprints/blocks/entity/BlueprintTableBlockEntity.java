package dev.michaud.pandas_blueprints.blocks.entity;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.BlueprintTableBlock;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManagerHolder;
import dev.michaud.pandas_blueprints.blueprint.virtualelement.VirtualSchematicDisplayElement;
import dev.michaud.pandas_blueprints.components.ModComponentTypes;
import dev.michaud.pandas_blueprints.items.FilledBlueprintItem;
import dev.michaud.pandas_blueprints.util.BoxDetector;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
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
      blockEntity.schematicDisplayElement.setRotation(facingState);
    }

    // Update blueprint id
    final Identifier blueprintId = hasBlueprintInventory
        ? blockEntity.getBlueprint().get(ModComponentTypes.BLUEPRINT_ID)
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

  /** Show blueprint to applicable players */
  public void showBlueprintToPlayers() {

    ServerWorld world = (ServerWorld) getWorld();

    if (schematicDisplayElement == null || world == null) {
      return;
    }

    final Vec3i schematicSize = schematicDisplayElement.getSchematic().getSize();
    final BlockPos blockPos = getPos();
    final BlockPos schematicCorner = getPos().add(schematicSize);

    final Box closeToTableBox = Box.of(Vec3d.of(blockPos), 10, 10, 10);
    final Box closeToHologramBox = Box.enclosing(blockPos, schematicCorner).expand(5);

    for (ServerPlayerEntity player : world.getPlayers()) {

      Vec3d pos = player.getPos();
      boolean shouldShow = closeToHologramBox.contains(pos) || closeToTableBox.contains(pos);

      if (shouldShow) {
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
   * Saves the surrounding area to a .nbt file
   *
   * @return The ID of the saves schematic
   */
  public @Nullable Identifier saveSchematic() {
    if (world == null) {
      return null;
    }

    final BlockPos tablePos = getPos();
    final Optional<BlockBox> outline = BoxDetector.detectOutlineOf(getPos(), 10,
        (pos) -> pos.equals(tablePos)
            || world.getBlockState(pos).getBlock() instanceof ScaffoldingBlock);

    if (outline.isEmpty()) {
      return null;
    }

    final BlueprintSchematic schematic = BlueprintSchematic.create(world, outline.get(), tablePos);

    final BlueprintSchematicManagerHolder server = (BlueprintSchematicManagerHolder) world.getServer();
    final Optional<BlueprintSchematicManager> schematicManager =
        server == null ? Optional.empty() : server.getBlueprintSchematicManager();

    if (schematicManager.isEmpty()) {
      PandasBlueprints.LOGGER.error("No blueprint schematic manager on server !!!");
      return null;
    }

    return schematicManager.get().saveSchematic(schematic);
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

    final Optional<BlueprintSchematicManager> manager = ((BlueprintSchematicManagerHolder) getWorld().getServer()).getBlueprintSchematicManager();

    if (manager.isEmpty()) {
      PandasBlueprints.LOGGER.error("No Blueprint Schematic manager found on the server!");
      return Optional.empty();
    }

    final Identifier id = getBlueprint().get(ModComponentTypes.BLUEPRINT_ID);
    return manager.get().getSchematic(id);
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
  protected void readNbt(NbtCompound nbt, WrapperLookup registries) {
    super.readNbt(nbt, registries);

    final ItemStack blueprint = ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Blueprint"));
    setBlueprint(blueprint);
  }

  @Override
  protected void writeNbt(NbtCompound nbt, WrapperLookup registries) {
    super.writeNbt(nbt, registries);

    if (hasBlueprint()) {
      nbt.put("Blueprint", getBlueprint().toNbt(registries));
    }
  }

}