package dev.michaud.pandas_blueprints.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BlueprintListCommand {

  public static int execute(CommandContext<ServerCommandSource> context) {
    return execute(context, 1);
  }

  public static int executeWithArg(CommandContext<ServerCommandSource> context) {
    return execute(context, IntegerArgumentType.getInteger(context, "page"));
  }

  public static int execute(CommandContext<ServerCommandSource> context, int page) {

    final ServerCommandSource source = context.getSource();
    final MinecraftServer server = source.getServer();
    final BlueprintSchematicManager manager = BlueprintSchematicManager.getInstance(server);

    final List<Identifier> ids = manager.getSchematicIds();
    final List<List<Identifier>> pages = Lists.partition(ids, 8);

    final int numIds = ids.size();
    final int numPages = pages.size();

    if (numPages == 0) {
      source.sendError(Text.literal("No blueprints to list!"));
      return 0;
    }

    if (page > numPages) {
      source.sendError(
          Text.literal(String.format("Page %d is too high, only %d exist.", page, numPages)));
      return 0;
    }

    final int pageIndex = page - 1;

    final String headStr = String.format("Showing page %d of %d:", page, numPages);
    final String listStr = pages.get(pageIndex).stream()
        .map(Identifier::toString)
        .map(s -> String.format("\"%s\"", s))
        .collect(Collectors.joining(", "));

    final boolean hasPrevPage = page > 1;
    final boolean hasNextPage = page < numPages;
    final boolean showNav = hasPrevPage || hasNextPage;

    source.sendFeedback(() -> Text.literal(headStr), false);
    source.sendFeedback(() -> Text.literal(listStr), false);

    if (showNav) {
      source.sendFeedback(() -> {
        final MutableText nav = Text.empty();

        if (hasPrevPage) {
          nav.append(Text.literal("[<- Prev]").setStyle(Style.EMPTY
              .withClickEvent(new RunCommand("/blueprint list " + (page - 1)))
              .withHoverEvent(new ShowText(Text.literal("Page " + (page - 1))))));
        }

        if (hasPrevPage && hasNextPage) {
          nav.append(Text.literal(" | "));
        }

        if (hasNextPage) {
          nav.append(Text.literal("[Next ->]").setStyle(Style.EMPTY
              .withClickEvent(new RunCommand("/blueprint list " + (page + 1)))
              .withHoverEvent(new ShowText(Text.literal("Page " + (page + 1))))));
        }

        return nav;
      }, false);
    }

    return numIds;
  }

}