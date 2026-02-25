package dev.michaud.pandas_blueprints.items;

import com.google.common.collect.ImmutableList;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import dev.michaud.pandas_blueprints.components.BlueprintIdComponent;
import dev.michaud.pandas_blueprints.components.ModComponentTypes;
import dev.michaud.pandas_blueprints.util.MaterialListUtil;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.dialog.AfterAction;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.DialogButtonData;
import net.minecraft.dialog.DialogCommonData;
import net.minecraft.dialog.body.DialogBody;
import net.minecraft.dialog.body.ItemDialogBody;
import net.minecraft.dialog.body.PlainMessageDialogBody;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.dialog.type.NoticeDialog;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

/**
 * A blueprint that has an associated schematic
 *
 * @see EmptyBlueprintItem
 */
public class FilledBlueprintItem extends Item implements PolymerItem {

  public FilledBlueprintItem(Settings settings) {
    super(settings);
  }

  public static ItemStack createBlueprint(Identifier id, PlayerEntity author) {
    final ItemStack itemStack = new ItemStack(ModItems.FILLED_BLUEPRINT);
    itemStack.set(ModComponentTypes.BLUEPRINT_ID, new BlueprintIdComponent(id));

    return itemStack;
  }

  @Override
  public ActionResult use(World world, PlayerEntity player, Hand hand) {
    final ItemStack itemStack = player.getStackInHand(hand);

    if (!(player instanceof ServerPlayerEntity serverPlayer)) {
      return ActionResult.PASS;
    }

    final BlueprintIdComponent blueprintId = itemStack.get(ModComponentTypes.BLUEPRINT_ID);
    final BlueprintSchematicManager manager = BlueprintSchematicManager.getInstance(
        serverPlayer.getEntityWorld());

    if (blueprintId == null) {
      return ActionResult.PASS; //Can't open material list on non-blueprint item!
    }

    manager.getSchematic(blueprintId.id()).ifPresent(blueprint -> {
      showMaterialList(blueprintId.id(), blueprint, serverPlayer);
    });

    return ActionResult.SUCCESS;
  }

  //TODO: Translation
  protected static void showMaterialList(Identifier id, BlueprintSchematic blueprint,
      ServerPlayerEntity player) {

    ImmutableList.Builder<DialogBody> materialListBuilder = ImmutableList.builder();

    for (Map.Entry<ItemStack, Integer> entry : MaterialListUtil.getMaterialsList(blueprint)
        .entrySet()) {
      final ItemStack item = entry.getKey();
      final int count = entry.getValue();

      materialListBuilder.add(new ItemDialogBody(item, Optional.of(
          new PlainMessageDialogBody(item.getItemName().copyContentOnly().append(" x" + count),
              200)), true, true, 16, 16));
    }

    final Dialog dialog = new NoticeDialog(
        new DialogCommonData(
            Text.literal("Material list for blueprint \"" + id + "\""),
            Optional.empty(),
            true,
            false,
            AfterAction.CLOSE,
            materialListBuilder.build(),
            List.of()
        ),
        new DialogActionButtonData(new DialogButtonData(ScreenTexts.DONE, 150), Optional.empty())
    );

    final RegistryEntry<Dialog> entry = RegistryEntry.of(dialog);
    player.openDialog(entry);
  }

  @Override
  public ItemStack getDefaultStack() {
    final ItemStack stack = super.getDefaultStack();
    stack.set(ModComponentTypes.BLUEPRINT_ID, BlueprintIdComponent.EMPTY);
    return stack;
  }

  @Override
  public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
    return Items.MAP;
  }

  @Override
  public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
    if (PolymerResourcePackUtils.hasMainPack(context)) {
      return Identifier.of(PandasBlueprints.GREENPANDA_ID, "filled_blueprint");
    } else {
      return Identifier.ofVanilla("painting");
    }
  }
}