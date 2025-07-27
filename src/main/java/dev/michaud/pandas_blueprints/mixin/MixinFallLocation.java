package dev.michaud.pandas_blueprints.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.michaud.pandas_blueprints.tags.ModBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.FallLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes it so that our custom scaffolding is treated as scaffolding in death messages.
 */
@Mixin(FallLocation.class)
public abstract class MixinFallLocation {

  @Definition(id = "state", local = @Local(type = BlockState.class, argsOnly = true))
  @Definition(id = "isOf", method = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z")
  @Definition(id = "SCAFFOLDING", field = "Lnet/minecraft/block/Blocks;SCAFFOLDING:Lnet/minecraft/block/Block;")
  @Expression("state.isOf(SCAFFOLDING)")
  @ModifyExpressionValue(method = "fromBlockState", at = @At("MIXINEXTRAS:EXPRESSION"))
  private static boolean isStateScaffolding(boolean original,
      @Local(argsOnly = true) BlockState state) {
    return original || state.isIn(ModBlockTags.SCAFFOLDING);
  }

}