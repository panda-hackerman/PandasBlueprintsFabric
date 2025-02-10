package dev.michaud.pandas_blueprints.mixin;

import com.mojang.datafixers.DataFixer;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManager;
import dev.michaud.pandas_blueprints.blueprint.BlueprintSchematicManagerHolder;
import java.net.Proxy;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.network.QueryableServer;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.ServerTask;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.world.ChunkErrorHandler;
import net.minecraft.util.ApiServices;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage.Session;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer extends ReentrantThreadExecutor<ServerTask> implements
    QueryableServer, ChunkErrorHandler, CommandOutput, BlueprintSchematicManagerHolder {

  @Shadow
  private @Final CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries;

  @Shadow
  protected @Final SaveProperties saveProperties;

  @Unique
  private BlueprintSchematicManager blueprintSchematicManager;

  public MixinMinecraftServer(String string) {
    super(string);
  }

  @Override
  public Optional<BlueprintSchematicManager> getBlueprintSchematicManager() {
    return Optional.of(blueprintSchematicManager);
  }

  @Inject(method = "<init>", at = @At("TAIL"))
  public void constructor(Thread serverThread, Session session, ResourcePackManager dataPackManager,
      SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices,
      WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory,
      CallbackInfo ci) {

    RegistryEntryLookup<Block> registryEntryLookup = combinedDynamicRegistries
        .getCombinedRegistryManager()
        .getOrThrow(RegistryKeys.BLOCK)
        .withFeatureFilter(saveProperties.getEnabledFeatures());

    blueprintSchematicManager = new BlueprintSchematicManager(saveLoader.resourceManager(), session,
        dataFixer, registryEntryLookup);
  }

  @Inject(method = "method_29440", at = @At("TAIL"))
  public void reloadResourcesThenAccept(Collection<String> dataPacks,
      MinecraftServer.ResourceManagerHolder resourceManagerHolder,
      CallbackInfo ci) {
    blueprintSchematicManager.setResourceManager(resourceManagerHolder.resourceManager());
  }

}
