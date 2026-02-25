package dev.michaud.pandas_blueprints.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematic;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import dev.michaud.pandas_blueprints.items.FilledBlueprintItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BlueprintGiveCommand {

  public static int executeWithTargets(CommandContext<ServerCommandSource> context)
      throws CommandSyntaxException {
    return execute(context, EntityArgumentType.getPlayers(context, "targets"));
  }

  public static int execute(CommandContext<ServerCommandSource> context) {
    final ServerCommandSource source = context.getSource();
    final ServerPlayerEntity player = source.getPlayer();

    if (player == null) {
      source.sendError(Text.of("Can't give item to a non-player!"));
      return 0;
    }

    return execute(context, List.of(player));
  }

  public static int execute(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> targets) {
    final ServerCommandSource source = context.getSource();
    final MinecraftServer server = source.getServer();
    final BlueprintSchematicManager manager = BlueprintSchematicManager.getInstance(server);

    final Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
    final Optional<BlueprintSchematic> blueprint = manager.getSchematic(id);

    if (blueprint.isEmpty()) {
      source.sendError(Text.of("Not a valid blueprint id!"));
      return 0;
    }

    final ItemStack itemStack = FilledBlueprintItem.createBlueprint(id, null);
    for (ServerPlayerEntity player : targets) {
      player.giveOrDropStack(itemStack);
    }

    return targets.size();
  }

}