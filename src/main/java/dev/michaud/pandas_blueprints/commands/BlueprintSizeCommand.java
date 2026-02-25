package dev.michaud.pandas_blueprints.commands;

import com.mojang.brigadier.context.CommandContext;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent.CopyToClipboard;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BlueprintSizeCommand {

  public static int execute(CommandContext<ServerCommandSource> context) {

    final ServerCommandSource source = context.getSource();
    final MinecraftServer server = source.getServer();
    final BlueprintSchematicManager manager = BlueprintSchematicManager.getInstance(server);

    final Identifier id = IdentifierArgumentType.getIdentifier(context, "id");

    Optional<BlueprintSchematic> blueprint = manager.getSchematic(id);

    if (blueprint.isEmpty()) {
      source.sendError(Text.of("Not a valid blueprint id!"));
      return 0;
    }

    final byte[] raw;
    try (ByteArrayOutputStream boas = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(boas))) {

      final NbtCompound nbt = blueprint.get().writeNbtSafe();
      NbtIo.writeCompressed(nbt, out);

      raw = boas.toByteArray();
    } catch (IOException e) {
      source.sendError(Text.of(e.getMessage()));
      return 0;
    }

    final int size = raw.length;
    final String b64 = Base64.getEncoder().encodeToString(raw);

    source.sendFeedback(() -> Text.literal(String.format("%s is %d bytes", id, size))
        .setStyle(Style.EMPTY.withClickEvent(new CopyToClipboard(b64))), false);

    return size;
  }

}