package dev.michaud.pandas_blueprints.blocks;

import com.mojang.serialization.MapCodec;
import dev.michaud.pandas_blueprints.blocks.entity.BlueprintTableBlockEntity;
import dev.michaud.pandas_blueprints.items.EmptyBlueprintItem;
import dev.michaud.pandas_blueprints.items.FilledBlueprintItem;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

/**
 * Table that lets you display blueprints
 */
public class BlueprintTableBlock extends Block implements BlockEntityProvider,
    PolymerTexturedBlock {

//  private static final int MAX_BLUEPRINT_SIZE = 5;

  public static final MapCodec<BlueprintTableBlock> CODEC = createCodec(BlueprintTableBlock::new);

  /** If this block has a blueprint or not */
  public static final BooleanProperty HAS_BLUEPRINT = BooleanProperty.of("has_blueprint");

  /* Block Models */
  public static final PolymerBlockModel BLOCK_MODEL_EMPTY = PolymerBlockModel.of(
      Identifier.of("greenpanda", "block/blueprint_table"));
  public static final PolymerBlockModel BLOCK_MODEL_WITH_BLUEPRINT = PolymerBlockModel.of(
      Identifier.of("greenpanda", "block/blueprint_table_filled"));

  public static final BlockState POLYMER_BLOCK_STATE_EMPTY = PolymerBlockResourceUtils.requestBlock(
      BlockModelType.FULL_BLOCK, BLOCK_MODEL_EMPTY);
  public static final BlockState POLYMER_BLOCK_STATE_WITH_BLUEPRINT = PolymerBlockResourceUtils.requestBlock(
      BlockModelType.FULL_BLOCK, BLOCK_MODEL_WITH_BLUEPRINT);

  public BlueprintTableBlock(AbstractBlock.Settings settings) {
    super(settings);
    setDefaultState(getDefaultState().with(HAS_BLUEPRINT, false));
  }

  @Override
  protected MapCodec<? extends BlueprintTableBlock> getCodec() {
    return CODEC;
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(HAS_BLUEPRINT);
  }

  @Override
  public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
    if (state.get(HAS_BLUEPRINT)) {
      return POLYMER_BLOCK_STATE_WITH_BLUEPRINT;
    } else {
      return POLYMER_BLOCK_STATE_EMPTY;
    }
  }

  @Override
  public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new BlueprintTableBlockEntity(pos, state);
  }

  @Override
  protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
      BlockHitResult hit) {

    if (world.getBlockEntity(pos) instanceof BlueprintTableBlockEntity blockEntity
        && blockEntity.hasBlueprint()) {
      if (dropBlueprint(world, pos)) {
        setHasBlueprint(player, state, world, pos, false);
        return ActionResult.SUCCESS;
      }
    }

    return ActionResult.PASS;
  }

  @Override
  protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
      PlayerEntity player, Hand hand, BlockHitResult hit) {

    if (!(world.getBlockEntity(pos) instanceof BlueprintTableBlockEntity blockEntity)) {
      return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
    }

    // Put filled blueprint
    if (blockEntity.isEmpty() && stack.getItem() instanceof FilledBlueprintItem) {
      if (putBlueprint(player, world, pos, stack)) {
        setHasBlueprint(player, state, world, pos, true);
        return ActionResult.SUCCESS;
      }
    }

    // Use empty blueprint
    if (blockEntity.isEmpty() && stack.getItem() instanceof EmptyBlueprintItem) {
      //Fill blueprint
    }

    return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
  }

  @Override
  protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState,
      boolean moved) {

    if (!state.isOf(newState.getBlock())) { // Block was removed
      dropBlueprint(world, pos);
      super.onStateReplaced(state, world, pos, newState, moved);
    }
  }

  protected boolean tryFillBlueprint(PlayerEntity player, Hand hand, World world, BlockPos pos, ItemStack stack) {

//    int origin_x = pos.getX();
//    int origin_y = pos.getY();
//    int origin_z = pos.getZ();
//
//    for (int x = 0; x < MAX_BLUEPRINT_SIZE; x++) {
//      for (int y = 0; y < MAX_BLUEPRINT_SIZE; y++) {
//        for (int z = 0; z < MAX_BLUEPRINT_SIZE; z++) {
//
//          BlockPos blockPos = new BlockPos(origin_x + x, origin_y + y, origin_z + z);
//          BlockState state = world.getBlockState(blockPos);
//
//          if (state.isAir()) {
//            continue;
//          }
//
//        }
//      }
//    }

    return true;
  }

  /**
   * Set if this block has a blueprint
   * @see BlueprintTableBlock#HAS_BLUEPRINT
   */
  protected void setHasBlueprint(LivingEntity player, BlockState state, World world, BlockPos pos,
      boolean has_blueprint) {
    BlockState newState = state.with(HAS_BLUEPRINT, has_blueprint);
    world.setBlockState(pos, newState, Block.NOTIFY_ALL);
    world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, state));
  }

  /**
   * Drop the current blueprint
   * @return If the blueprint was actually dropped
   */
  protected boolean dropBlueprint(World world, BlockPos pos) {
    if (!(world.getBlockEntity(pos) instanceof BlueprintTableBlockEntity blockEntity)) {
      return false;
    }

    final ItemStack itemStack = blockEntity.getBlueprint().copy();
    final ItemEntity itemEntity = new ItemEntity(world,
        pos.getX() + 0.5,
        pos.getY() + 1,
        pos.getZ() + 0.5,
        itemStack);

    itemEntity.setToDefaultPickupDelay();
    world.spawnEntity(itemEntity);
    blockEntity.clear();

    return true;
  }

  /**
   * Store the given blueprint
   * @return If the blueprint was actually stored
   */
  protected boolean putBlueprint(LivingEntity player, World world, BlockPos pos, ItemStack blueprint) {
    if (!(world.getBlockEntity(pos) instanceof BlueprintTableBlockEntity blockEntity)) {
      return false;
    }

    blockEntity.setBlueprint(blueprint.splitUnlessCreative(1, player));
    world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1, 1);

    return true;
  }

  /**
   * Held {@link BlueprintTableBlock} Item
   */
  public static class BlueprintTableItem extends PolymerBlockItem {

    public BlueprintTableItem(Block block, Settings settings) {
      super(block, settings);
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
      if (PolymerResourcePackUtils.hasMainPack(context)) {
        return Identifier.of("greenpanda", "blueprint_table");
      } else {
        return Identifier.of("minecraft", "crafting_table");
      }
    }
  }
}