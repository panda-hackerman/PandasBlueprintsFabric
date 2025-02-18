package dev.michaud.pandas_blueprints.blocks.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.BlueprintTableBlock;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManagerHolder;
import dev.michaud.pandas_blueprints.blueprint.virtualelement.VirtualSchematicDisplayElement;
import dev.michaud.pandas_blueprints.components.ModComponentTypes;
import dev.michaud.pandas_blueprints.items.FilledBlueprintItem;
import dev.michaud.pandas_blueprints.util.CustomMathHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

  @Nullable Identifier blueprintId;
  @Nullable VirtualSchematicDisplayElement schematicDisplayElement;

  DefaultedList<ItemStack> items = DefaultedList.ofSize(1, ItemStack.EMPTY);

  public BlueprintTableBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntityTypes.BLUEPRINT_TABLE, pos, state);
  }

  public static void tick(World world, BlockPos pos, BlockState state,
      BlueprintTableBlockEntity blockEntity) {

    if (blockEntity.isRemoved()) {
      return;
    }

    final ServerWorld serverWorld = (ServerWorld) world;

    // Update block state
    final boolean hasBlueprintState = state.get(BlueprintTableBlock.HAS_BLUEPRINT);
    final boolean hasBlueprintInventory = blockEntity.hasBlueprint();

    if (hasBlueprintState != hasBlueprintInventory) {
      BlockState newState = state.with(BlueprintTableBlock.HAS_BLUEPRINT, hasBlueprintInventory);
      world.setBlockState(pos, newState, Block.NOTIFY_ALL);
      world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, Emitter.of(null, state));
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
      blockEntity.showBlueprintToPlayers(serverWorld);
    }

  }

  public void showBlueprintToPlayers(ServerWorld world) {

    if (schematicDisplayElement == null) {
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

    final Optional<BlockBox> outline = detectOutline(world, getPos());

    if (outline.isEmpty()) {
      return null;
    }

    final BlueprintSchematic schematic = BlueprintSchematic.create(world, outline.get(), getPos());

    final BlueprintSchematicManagerHolder server = (BlueprintSchematicManagerHolder) world.getServer();
    final Optional<BlueprintSchematicManager> schematicManager =
        server == null ? Optional.empty() : server.getBlueprintSchematicManager();

    if (schematicManager.isEmpty()) {
      PandasBlueprints.LOGGER.error("No blueprint schematic manager on server !!!");
      return null;
    }

    return schematicManager.get().saveSchematic(schematic);
  }

  protected Optional<BlockBox> detectOutline(World world, BlockPos tablePos) {

    // Scan out in cardinal
    final int scanDistance = 10;

    final Map<Direction, List<BlockPos>> scaffoldingByDirection = ImmutableMap.of(
        Direction.NORTH, scaffoldingInDirection(world, tablePos, Direction.NORTH, scanDistance),
        Direction.EAST, scaffoldingInDirection(world, tablePos, Direction.EAST, scanDistance),
        Direction.SOUTH, scaffoldingInDirection(world, tablePos, Direction.SOUTH, scanDistance),
        Direction.WEST, scaffoldingInDirection(world, tablePos, Direction.WEST, scanDistance));

    final List<Direction> validDirections = scaffoldingByDirection.keySet().stream()
        .filter(dir -> !scaffoldingByDirection.get(dir).isEmpty()) //Get non-empty directions
        .toList();

    PandasBlueprints.LOGGER.info("Detected scaffolding in {} directions.", validDirections.size());

    if (validDirections.size() < 2) {
      PandasBlueprints.LOGGER.info("Less than 2 scaffolding detected");
      return Optional.empty();
    }

    // Get base (bottom) blocks from the 4 directions
    final List<BlockPos> baseCandidates = new ArrayList<>();

    for (final Direction direction : validDirections) {
      baseCandidates.add(scaffoldingByDirection.get(direction).get(0));
    }

    PandasBlueprints.LOGGER.info("Scaffolding at: {}", baseCandidates);

    final BlockBox boxBase = switch (baseCandidates.size()) {
      case 2 -> getValidBoxFrom(world, baseCandidates.get(0), baseCandidates.get(1));
      case 3 -> getValidBoxFrom(world, baseCandidates.get(0), baseCandidates.get(1), baseCandidates.get(2));
      case 4 -> getValidBoxFrom(world, baseCandidates.get(0), baseCandidates.get(1), baseCandidates.get(2), baseCandidates.get(3));
      default -> null;
    };

    if (boxBase == null) {
      PandasBlueprints.LOGGER.info("Scaffolding at these positions do not form a valid rectangle");
      return Optional.empty();
    }

    // Get top blocks from the two corners
    BlockPos northWest = new BlockPos(boxBase.getMaxX(), boxBase.getMaxY(), boxBase.getMaxZ());
    BlockPos southEast = new BlockPos(boxBase.getMinX(), boxBase.getMinY(), boxBase.getMinZ());

    final List<BlockPos> scaffoldingNW = scaffoldingInDirection(world, northWest, Direction.UP, scanDistance * 2);
    final List<BlockPos> scaffoldingSE = scaffoldingInDirection(world, southEast, Direction.UP, scanDistance * 2);

    final List<Integer> commonYList = scaffoldingNW.stream()
        .map(BlockPos::getY)
        .filter(i -> scaffoldingSE.stream().anyMatch(b -> b.getY() == i))
        .sorted(Comparator.reverseOrder())
        .toList();

    PandasBlueprints.LOGGER.info("Found {} scaffolding in the NW corner and {} in the SE corner. "
        + "They share {} of the same heights",
        scaffoldingNW.size(), scaffoldingSE.size(), commonYList.size());

    BlockBox boxTop = null;

    for (final Integer y : commonYList) {

      PandasBlueprints.LOGGER.info("Looking for top at y = {}", y);

      BlockPos nw = new BlockPos(boxBase.getMaxX(), y, boxBase.getMaxZ());
      BlockPos se = new BlockPos(boxBase.getMinX(), y, boxBase.getMinZ());

      boxTop = getValidBoxFrom(world, nw, se);

      if (boxTop != null) {
        break;
      }
    }

    if (boxTop == null) {
      return Optional.empty();
    }

    return Optional.of(boxBase.encompass(boxTop));
  }

  private List<BlockPos> scaffoldingInDirection(World world, BlockPos origin, Direction direction,
      int maxDistance) {

    List<BlockPos> temp = new ArrayList<>();
    final Vec3i vector = direction.getVector();

    for (int i = 1; i <= maxDistance; i++) {

      final BlockPos pos = origin.add(vector.multiply(i));
      final BlockState state = world.getBlockState(pos);

      if (state.getBlock() instanceof ScaffoldingBlock) {
        temp.add(pos);
      }
    }

    // Sort so that the farthest is first
    temp.sort(Comparator.comparingDouble((BlockPos e) -> e.getSquaredDistance(origin)).reversed());

    return ImmutableList.copyOf(temp);
  }

  /**
   * @return The box containing a and b if there is a valid perimeter of scaffolding, otherwise null.
   */
  protected @Nullable BlockBox getValidBoxFrom(World world, BlockPos a, BlockPos b) {
    final int y = a.getY();

    final BlockBox box = new BlockBox(
        Math.min(a.getX(), b.getX()),
        y,
        Math.min(a.getZ(), b.getZ()),
        Math.max(a.getX(), b.getX()),
        y,
        Math.max(a.getZ(), b.getZ())
    );

    return checkScaffoldingPerimeter(world, box) ? box : null;
  }

  /**
   * @return The box containing a, b, and c, if there is a valid perimeter of scaffolding, otherwise null.
   */
  protected @Nullable BlockBox getValidBoxFrom(World world, BlockPos a, BlockPos b, BlockPos c) {

    final int y = a.getY();

    final BlockBox box = new BlockBox(
        CustomMathHelper.min(a.getX(), b.getX(), c.getX()),
        y,
        CustomMathHelper.min(a.getZ(), b.getZ(), c.getZ()),
        CustomMathHelper.max(a.getX(), b.getX(), c.getX()),
        y,
        CustomMathHelper.max(a.getZ(), b.getZ(), c.getZ())
    );

    return checkScaffoldingPerimeter(world, box) ? box : null;
  }

  /**
   * @return The box containing a, b, c, and d, if there is a valid perimeter of scaffolding.
   */
  protected @Nullable BlockBox getValidBoxFrom(World world, BlockPos a, BlockPos b, BlockPos c, BlockPos d) {

    final int y = a.getY();

    final BlockBox box = new BlockBox(
        CustomMathHelper.min(a.getX(), b.getX(), c.getX(), d.getX()),
        y,
        CustomMathHelper.min(a.getZ(), b.getZ(), c.getZ(), d.getZ()),
        CustomMathHelper.max(a.getX(), b.getX(), c.getX(), d.getX()),
        y,
        CustomMathHelper.max(a.getZ(), b.getZ(), c.getZ(), d.getZ())
    );

    return checkScaffoldingPerimeter(world, box) ? box : null;
  }

  protected boolean checkScaffoldingPerimeter(World world, BlockBox box) {

    final int sizeX = box.getBlockCountX();
    final int sizeZ = box.getBlockCountZ();

    final BlockPos maxPos = CustomMathHelper.getMaxPos(box);
    final BlockPos minPos = CustomMathHelper.getMinPos(box);

    PandasBlueprints.LOGGER.info("Checking from bounds {} to {}", maxPos, minPos);

    for (int i = 0; i < sizeX; i++) {
      BlockPos east = minPos.east(i);
      BlockPos west = maxPos.west(i);

      if (!(world.getBlockState(east).getBlock() instanceof ScaffoldingBlock)) {
        PandasBlueprints.LOGGER.info("[East] Block at {} is not scaffolding", east);
        return false;
      }

      if (!(world.getBlockState(west).getBlock() instanceof ScaffoldingBlock)) {
        PandasBlueprints.LOGGER.info("[West] Block at {} is not scaffolding", west);
        return false;
      }
    }

    for (int i = 0; i < sizeZ; i++) {
      BlockPos south = minPos.south(i);
      BlockPos north = maxPos.north(i);

      if (!(world.getBlockState(south).getBlock() instanceof ScaffoldingBlock)) {
        PandasBlueprints.LOGGER.info("[South] Block at {} is not scaffolding", south);
        return false;
      }

      if (!(world.getBlockState(north).getBlock() instanceof ScaffoldingBlock)) {
        PandasBlueprints.LOGGER.info("[North] Block at {} is not scaffolding", north);
        return false;
      }
    }

    return true;
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