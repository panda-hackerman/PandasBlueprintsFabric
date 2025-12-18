package dev.michaud.pandas_blueprints.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.PermissionLevelSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModCommands {

  private static final int BLUEPRINT_LIST_PAGE_SIZE = 16;

  private static int listAllBlueprints(CommandContext<ServerCommandSource> context) {
    return listAllBlueprints(context, 1);
  }

  private static int listAllBlueprints(CommandContext<ServerCommandSource> context, int page) {

    final ServerCommandSource source = context.getSource();
    final MinecraftServer server = source.getServer();
    final BlueprintSchematicManager manager = BlueprintSchematicManager.getState(server);

    final List<Identifier> ids = manager.getAllSchematicIds();
    final List<List<Identifier>> pages = Lists.partition(ids, BLUEPRINT_LIST_PAGE_SIZE);

    final int numPages = pages.size();

    if (numPages == 0) {
      source.sendError(Text.literal("No blueprints to list"));
      return 0;
    }

    if (page > numPages) {
      page = numPages;
    }

    final String listStr = pages.get(page - 1).stream()
        .map(Identifier::toString)
        .collect(Collectors.joining(","));

    final String head = String.format("Showing page %d of %d:", page, numPages);

    source.sendFeedback(() -> Text.literal(head), false);
    source.sendFeedback(() -> Text.literal(listStr), false);

    final MutableText nav = Text.empty();
    boolean showPrev = false;
    boolean showNext = false;

    if (page > 0) {
      final String previousPage = String.valueOf(page - 1);

      nav.append(Text.literal("[<- Prev.]").setStyle(Style.EMPTY
          .withClickEvent(new RunCommand("/blueprints list " + previousPage))
          .withHoverEvent(new ShowText(Text.literal("Page " + previousPage)))
      ));

      showPrev = true;
    }

    if (page < pages.size()) {
      final String nextPage = String.valueOf(page + 1);

      if (showPrev) {
        nav.append(Text.literal(" | "));
      }

      nav.append(Text.literal("[Next ->]").setStyle(Style.EMPTY
          .withClickEvent(new RunCommand("/blueprints list " + nextPage))
          .withHoverEvent(new ShowText(Text.literal("Page " + nextPage)))
      ));

      showNext = true;
    }

    if (showPrev || showNext) {
      source.sendFeedback(() -> nav, false);
    }

    return ids.size();
  }

  public static void registerModCommands() {

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

      dispatcher.register(CommandManager.literal("blueprints")
          .then(CommandManager.literal("list")
              .requires(PermissionLevelSource::hasElevatedPermissions)
              .executes(ModCommands::listAllBlueprints)
              .then(CommandManager.argument("page", IntegerArgumentType.integer(0))
                  .executes(context -> listAllBlueprints(context,
                      IntegerArgumentType.getInteger(context, "page"))))));


    });

  }

}
