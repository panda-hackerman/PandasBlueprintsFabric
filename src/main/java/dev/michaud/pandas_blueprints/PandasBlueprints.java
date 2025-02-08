package dev.michaud.pandas_blueprints;

import dev.michaud.pandas_blueprints.blocks.ModBlocks;
import dev.michaud.pandas_blueprints.blocks.entity.ModBlockEntityTypes;
import dev.michaud.pandas_blueprints.items.ModItems;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import java.util.logging.Logger;
import net.fabricmc.api.ModInitializer;

public class PandasBlueprints implements ModInitializer {

  public static final String MOD_ID = "pandas_blueprints";
  public static final Logger LOGGER = Logger.getLogger(MOD_ID);

  @Override
  public void onInitialize() {

    ModItems.registerModItems();
    ModBlocks.registerModBlocks();
    ModBlockEntityTypes.registerModBlockEntities();

    PolymerResourcePackUtils.addModAssets(MOD_ID);

    LOGGER.info("Panda's Blueprints initialized");
  }
}