package dev.michaud.pandas_blueprints.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.michaud.pandas_blueprints.tags.ModBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * We modify the sync world event and play sound calls to force it to also be sent to the player, if
 * it's one of our custom blocks.
 *
 * @implNote Replacing the player with null is enough for this to work. We check that it's
 * actually a custom block to prevent the event happening twice (since the vanilla client will
 * already check).
 * @see MixinHoneycombItem
 */
@Mixin(AxeItem.class)
public abstract class MixinAxeItem extends Item {

  public MixinAxeItem(Settings settings) {
    super(settings);
  }

  @ModifyArg(method = "tryStrip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;syncWorldEvent(Lnet/minecraft/entity/Entity;ILnet/minecraft/util/math/BlockPos;I)V"))
  private Entity broadcastParticlesToPlayer(Entity entity, @Local(argsOnly = true) BlockState state) {
    if (state.isIn(ModBlockTags.CUSTOM_WAXABLE) || state.isIn(ModBlockTags.CUSTOM_WAXED)) {
      return null;
    } else {
      return entity;
    }
  }

  @ModifyArg(method = "tryStrip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"))
  private Entity broadcastSoundToPlayer(Entity entity, @Local(argsOnly = true) BlockState state) {
    if (state.isIn(ModBlockTags.CUSTOM_WAXABLE) || state.isIn(ModBlockTags.CUSTOM_WAXED)) {
      return null;
    } else {
      return entity;
    }
  }
}