package dev.michaud.pandas_blueprints.blocks.entity;

import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
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

  protected boolean saveStructure() {

    BlockPos currentPos = getPos();
    Vec3i size = new Vec3i(5, 5, 5);

    BlueprintSchematic schematic = BlueprintSchematic.create(world, currentPos, size);


  }
}