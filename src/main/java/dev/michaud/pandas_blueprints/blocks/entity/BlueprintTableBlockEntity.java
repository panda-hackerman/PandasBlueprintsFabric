package dev.michaud.pandas_blueprints.blocks.entity;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManagerHolder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class BlueprintTableBlockEntity extends BlockEntity implements
    BlockEntityInventory<BlueprintTableBlockEntity> {

  ItemStack blueprint = ItemStack.EMPTY;

  public BlueprintTableBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntityTypes.BLUEPRINT_TABLE, pos, state);
  }

  public ItemStack getBlueprint() {
    return blueprint;
  }

  public void setBlueprint(ItemStack blueprint) {
    this.blueprint = blueprint;
    markDirty();
  }

  public boolean hasBlueprint() {
    return !getBlueprint().isEmpty();
  }

  public void clear() {
    setBlueprint(ItemStack.EMPTY);
  }

  @Override
  public @NotNull DefaultedList<ItemStack> getItems() {
    return DefaultedList.ofSize(1, blueprint);
  }

  @Override
  public @NotNull BlueprintTableBlockEntity getBlockEntity() {
    return this;
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

  public Identifier saveStructure() {
    if (world == null) {
      return null;
    }

    Vec3i size = new Vec3i(5, 5, 5); //TODO: Generate based on outline
    final BlockPos currentPos = getPos();

    final BlueprintSchematic schematic = BlueprintSchematic.create(world, currentPos, size);

    final Optional<BlueprintSchematicManager> schematicManager = (
        (BlueprintSchematicManagerHolder) Objects.requireNonNull(world.getServer()))
        .getBlueprintSchematicManager();

    if (schematicManager.isEmpty()) {
      PandasBlueprints.LOGGER.error("No blueprint schematic manager on server !!!");
      return null;
    }

    return schematicManager.get().saveSchematic(schematic);
  }
}