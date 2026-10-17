package com.techmonkey.no_soliciting;

import com.techmonkey.no_soliciting.block.NoSolicitingSignBlock;
import com.techmonkey.no_soliciting.block.entity.NoSolicitingSignBlockEntity;
import com.techmonkey.no_soliciting.network.NoSolicitingPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Set;

public final class NoSoliciting implements ModInitializer {
    public static final String MOD_ID = "no_soliciting";

    public static NoSolicitingSignBlock NO_SOLICITING_SIGN;
    public static Item NO_SOLICITING_SIGN_ITEM;
    public static BlockEntityType<NoSolicitingSignBlockEntity> NO_SOLICITING_SIGN_BLOCK_ENTITY;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Identifier signId = id("no_soliciting_sign");

        NO_SOLICITING_SIGN = Registry.register(
                BuiltInRegistries.BLOCK,
                signId,
                new NoSolicitingSignBlock(
                        BlockBehaviour.Properties.of()
                                .setId(ResourceKey.create(Registries.BLOCK, signId))
                                .strength(0.8F)
                                .sound(SoundType.WOOD)
                                .noOcclusion()
                )
        );

        NO_SOLICITING_SIGN_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                signId,
                new BlockItem(
                        NO_SOLICITING_SIGN,
                        new Item.Properties().setId(ResourceKey.create(Registries.ITEM, signId))
                )
        );

        NO_SOLICITING_SIGN_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("no_soliciting_sign"),
                new BlockEntityType<>(NoSolicitingSignBlockEntity::new, Set.of(NO_SOLICITING_SIGN))
        );

        ResourceKey<CreativeModeTab> functionalBlocks = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.withDefaultNamespace("functional_blocks")
        );
        CreativeModeTabEvents.modifyOutputEvent(functionalBlocks)
                .register(entries -> entries.accept(NO_SOLICITING_SIGN_ITEM));

        NoSolicitingPackets.registerServer();
    }
}

