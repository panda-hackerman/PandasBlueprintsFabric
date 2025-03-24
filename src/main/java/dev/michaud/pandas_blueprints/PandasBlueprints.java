package dev.michaud.pandas_blueprints;

import dev.michaud.pandas_blueprints.blocks.ModBlocks;
import dev.michaud.pandas_blueprints.blocks.entity.ModBlockEntityTypes;
import dev.michaud.pandas_blueprints.blocks.scaffolding.OxidizableScaffoldingBlockModels;
import dev.michaud.pandas_blueprints.components.ModComponentTypes;
import dev.michaud.pandas_blueprints.items.ModItems;
import dev.michaud.pandas_blueprints.recipe.ModRecipeSerializers;
import dev.michaud.pandas_blueprints.sounds.ModSounds;
import dev.michaud.pandas_blueprints.tags.ModTags;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Adds blueprints
 */
public class PandasBlueprints implements ModInitializer {

  public static final String MOD_ID = "pandas_blueprints";
  public static final String GREENPANDA_ID = "greenpanda";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    ModItems.registerModItems();
    ModBlocks.registerModBlocks();
    OxidizableScaffoldingBlockModels.registerScaffoldingBlockModels();
    ModSounds.registerSounds();
    ModComponentTypes.registerModComponents();
    ModBlockEntityTypes.registerModBlockEntities();
    ModRecipeSerializers.registerModRecipeSerializers();
    ModTags.registerModTags();

    PolymerResourcePackUtils.addModAssets(MOD_ID);

    LOGGER.info("Panda's Blueprints initialized");
  }
}