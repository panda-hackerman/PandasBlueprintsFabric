package dev.michaud.pandas_blueprints.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.michaud.pandas_blueprints.tags.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ScaffoldingItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScaffoldingItem.class)
public abstract class MixinScaffoldingItem extends BlockItem {

  public MixinScaffoldingItem(Block block, Settings settings) {
    super(block, settings);
  }

  @ModifyExpressionValue(method = "getPlacementContext", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
  private boolean blockIsScaffolding(boolean original, @Local BlockState state) {
    return original || state.isIn(ModTags.SCAFFOLDING_BLOCK);
  }

}