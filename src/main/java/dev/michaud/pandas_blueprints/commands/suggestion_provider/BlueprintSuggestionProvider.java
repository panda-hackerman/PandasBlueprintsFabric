package dev.michaud.pandas_blueprints.commands.suggestion_provider;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

public class BlueprintSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

  @Override
  public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context,
      SuggestionsBuilder builder) {

    final ServerCommandSource source = context.getSource();
    final MinecraftServer server = source.getServer();
    final BlueprintSchematicManager manager = BlueprintSchematicManager.getInstance(server);

    for (Identifier id : manager.getSchematicIds()) {
      builder.suggest(id.toString());
    }

    return builder.buildFuture();
  }
}