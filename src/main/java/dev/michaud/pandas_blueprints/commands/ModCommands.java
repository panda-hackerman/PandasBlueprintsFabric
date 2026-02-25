package dev.michaud.pandas_blueprints.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.michaud.pandas_blueprints.commands.suggestion_provider.BlueprintSuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.PermissionLevelSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;

public class ModCommands {

  public static void registerModCommands() {

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
      dispatcher.register(CommandManager.literal("blueprint")
          .requires(PermissionLevelSource::hasElevatedPermissions)
          .then(CommandManager.literal("list")
              .executes(BlueprintListCommand::execute)
              .then(CommandManager.argument("page", IntegerArgumentType.integer(0))
                  .executes(BlueprintListCommand::executeWithArg)))
          .then(CommandManager.literal("size")
              .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                  .suggests(new BlueprintSuggestionProvider())
                  .executes(BlueprintSizeCommand::execute)))
          .then(CommandManager.literal("give")
              .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                  .suggests(new BlueprintSuggestionProvider())
                  .executes(BlueprintGiveCommand::execute)
                  .then(CommandManager.argument("targets", EntityArgumentType.players())
                      .executes(BlueprintGiveCommand::executeWithTargets)))))
    );

  }

}