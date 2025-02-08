package dev.michaud.pandas_blueprints.blocks.entity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;

public interface BlockEntityInventory<T extends BlockEntity> extends Inventory {

  @NotNull DefaultedList<ItemStack> getItems();

  @NotNull T getBlockEntity();

  static <T extends BlockEntity> @NotNull BlockEntityInventory<T> of(@NotNull T entity, int size) {
    return new BlockEntityInventory<>() {

      final DefaultedList<ItemStack> items = DefaultedList.ofSize(size, ItemStack.EMPTY);

      @Override
      public @NotNull DefaultedList<ItemStack> getItems() {
        return items;
      }

      @Override
      public @NotNull T getBlockEntity() {
        return entity;
      }
    };
  }

  @Override
  default int size() {
    return getItems().size();
  }

  @Override
  default boolean isEmpty() {
    return getItems().stream().allMatch(ItemStack::isEmpty);
  }

  @Override
  default ItemStack getStack(int slot) {
    return getItems().get(slot);
  }

  @Override
  default ItemStack removeStack(int slot, int amount) {
    ItemStack result = Inventories.splitStack(getItems(), slot, amount);
    if (!result.isEmpty()) {
      markDirty();
    }
    return result;
  }

  @Override
  default ItemStack removeStack(int slot) {
    return Inventories.removeStack(getItems(), slot);
  }

  @Override
  default void setStack(int slot, ItemStack stack) {
    getItems().set(slot, stack);

    if (stack.getCount() > stack.getMaxCount()) {
      stack.setCount(stack.getMaxCount());
    }
  }

  @Override
  default void markDirty() {
    getBlockEntity().markDirty();
  }

  @Override
  default boolean canPlayerUse(PlayerEntity player) {
    return true;
  }

  @Override
  default void clear() {
    getItems().clear();
  }

}