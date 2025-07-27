package dev.michaud.pandas_blueprints.blocks.scaffolding;

import dev.michaud.pandas_blueprints.tags.ModBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class ScaffoldingItemPlacementContextUtil {

  public static ItemPlacementContext getItemPlacementContext(ItemPlacementContext context, ScaffoldingBlockDistanceHolder block) {

    final BlockPos blockPos = context.getBlockPos();
    final World world = context.getWorld();
    BlockState blockState = world.getBlockState(blockPos);

    if (!blockState.isIn(ModBlockTags.SCAFFOLDING)) {
      return block.calculateScaffoldingDistance(world, blockPos) == block.getMaxDistance() ? null : context;
    }

    Direction direction;
    if (context.shouldCancelInteraction()) {
      direction = context.hitsInsideBlock() ? context.getSide().getOpposite() : context.getSide();
    } else {
      direction = context.getSide() == Direction.UP ? context.getHorizontalPlayerFacing() : Direction.UP;
    }

    BlockPos.Mutable mutable = blockPos.mutableCopy().move(direction);

    int i = 0;
    while (i < block.getMaxDistance()) {

      // Too high
      if (!world.isClient && !world.isInBuildLimit(mutable)) {
        if (mutable.getY() > world.getTopYInclusive() && context.getPlayer() instanceof ServerPlayerEntity serverPlayer) {
          serverPlayer.sendMessageToClient(Text.translatable("build.tooHigh", world.getTopYInclusive()).formatted(Formatting.RED), true);
        }
        break; // return null
      }

      blockState = world.getBlockState(mutable);

      // Not scaffolding
      if (!blockState.isIn(ModBlockTags.SCAFFOLDING)) {
        if (blockState.canReplace(context)) {
          return ItemPlacementContext.offset(context, mutable, direction);
        }
        break; // return null
      }

      mutable.move(direction);
      if (direction.getAxis().isHorizontal()) {
        i++;
      }
    }

    return null;
  }

}