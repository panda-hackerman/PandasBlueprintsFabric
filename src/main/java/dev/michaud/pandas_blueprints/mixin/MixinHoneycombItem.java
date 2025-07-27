package dev.michaud.pandas_blueprints.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.michaud.pandas_blueprints.tags.ModBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.HoneycombItem;
import net.minecraft.item.Item;
import net.minecraft.item.SignChangingItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * We modify the sync world event call to force the server to also send the packet to the player,
 * if it's one of our custom blocks.
 *
 * @implNote Replacing the player with null is enough for this to work. We check that it's
 * actually a custom block to prevent the event happening twice (since the vanilla client will
 * already check).
 * @see MixinAxeItem
 */
@Mixin(HoneycombItem.class)
public abstract class MixinHoneycombItem extends Item implements SignChangingItem {

  public MixinHoneycombItem(Settings settings) {
    super(settings);
  }

  @ModifyArg(method = "method_34719", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;syncWorldEvent(Lnet/minecraft/entity/Entity;ILnet/minecraft/util/math/BlockPos;I)V"))
  private static Entity broadcastToPlayer(Entity entity, @Local(argsOnly = true) BlockState state) {
    if (state.isIn(ModBlockTags.CUSTOM_WAXABLE) || state.isIn(ModBlockTags.CUSTOM_WAXED)) {
      return null;
    } else {
      return entity;
    }
  }
}