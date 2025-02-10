package dev.michaud.pandas_blueprints;

import dev.michaud.pandas_blueprints.blocks.ModBlocks;
import dev.michaud.pandas_blueprints.blocks.entity.ModBlockEntityTypes;
import dev.michaud.pandas_blueprints.components.ModDataComponentTypes;
import dev.michaud.pandas_blueprints.items.ModItems;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Adds blueprints */
public class PandasBlueprints implements ModInitializer {

  public static final String MOD_ID = "pandas_blueprints";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {

    ModItems.registerModItems();
    ModBlocks.registerModBlocks();
    ModDataComponentTypes.registerModComponents();
    ModBlockEntityTypes.registerModBlockEntities();

    PolymerResourcePackUtils.addModAssets(MOD_ID);

    LOGGER.info("Panda's Blueprints initialized");
  }
}