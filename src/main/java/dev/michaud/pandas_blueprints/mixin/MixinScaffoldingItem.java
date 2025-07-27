package dev.michaud.pandas_blueprints.mixin;

import dev.michaud.pandas_blueprints.tags.ModBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ScaffoldingItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScaffoldingItem.class)
public abstract class MixinScaffoldingItem extends BlockItem {

  public MixinScaffoldingItem(Block block, Settings settings) {
    super(block, settings);
  }

  @Redirect(method = "getPlacementContext", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
  private boolean blockIsScaffolding(BlockState state, Block block) {
    return state.isIn(ModBlockTags.SCAFFOLDING);
  }
}