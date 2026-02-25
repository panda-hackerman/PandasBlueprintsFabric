package dev.michaud.pandas_blueprints.blueprint;

import com.google.common.collect.ImmutableList;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class BlueprintDialogBuilder {

//  public static @NotNull Dialog buildExportDialog(@NotNull BlueprintSchematic blueprint) {
//
//    final byte[] raw;
//    try (ByteArrayOutputStream boas = new ByteArrayOutputStream();
//        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(boas))) {
//
//      final NbtCompound nbt = blueprint.writeNbtSafe();
//      NbtIo.writeCompressed(nbt, out);
//
//      raw = boas.toByteArray();
//    } catch (IOException e) {
//      throw new UncheckedIOException("Couldn't write blueprint to file", e);
//    }
//
//    final int size = raw.length;
//    final String b64 = Base64.getEncoder().encodeToString(raw);
//
//
//  }
//
//  public static @NotNull Dialog buildMaterialList(@NotNull BlueprintSchematic blueprint) {
//
//    final List<DialogBody> dialogBodyList = blueprint.getAllBlocks().stream()
//        .sorted(Comparator.comparingInt(blueprint::getCount).reversed()
//            .thenComparing(b -> Registries.BLOCK.getId(b).getPath(), String::compareTo)
//            .thenComparing(b -> Registries.BLOCK.getId(b).getNamespace(), String::compareTo))
//        .map(block -> {
//          final ItemStack item = block.asItem().getDefaultStack();
//          final int count = blueprint.getCount(block);
//
//          return new ItemDialogBody(item, Optional.of(
//              new PlainMessageDialogBody(item.getItemName().copyContentOnly().append(" x" + count),
//                  200)), true, true, 16, 16);
//        }).collect(ImmutableList.toImmutableList());
//
//    return new NoticeDialog(
//        new DialogCommonData(
//            Text.literal("Material list for blueprint"),  // Title
//            Optional.of(Text.literal("Material list")),   // External title
//            true,                                                // Can close with escape
//            false,                                               // Pause
//            AfterAction.CLOSE,                                   // After action
//            dialogBodyList,                                      // Body
//            List.of()                                            // Inputs
//        ),
//        new DialogActionButtonData(new DialogButtonData(ScreenTexts.DONE, 150), Optional.empty())
//    );
//  }

}