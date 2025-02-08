package dev.michaud.pandas_blueprints.blocks.entity;

import dev.michaud.pandas_blueprints.PandasBlueprints;
import dev.michaud.pandas_blueprints.blocks.ModBlocks;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.Factory;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntityTypes {

  public static final BlockEntityType<BlueprintTableBlockEntity> BLUEPRINT_TABLE = register(
      "blueprint_table", BlueprintTableBlockEntity::new, ModBlocks.BLUEPRINT_TABLE
  );

  public static <T extends BlockEntity> BlockEntityType<T> register(String name, Factory<T> factory,
      Block... blocks) {

    final Identifier id = Identifier.of(PandasBlueprints.MOD_ID, name);
    final BlockEntityType<T> blockEntityType = FabricBlockEntityTypeBuilder.create(factory, blocks)
        .build();

    return Registry.register(Registries.BLOCK_ENTITY_TYPE, id, blockEntityType);
  }

  public static void registerModBlockEntities() {
    PolymerBlockUtils.registerBlockEntity(BLUEPRINT_TABLE);
  }

}