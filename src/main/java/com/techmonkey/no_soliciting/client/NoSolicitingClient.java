package com.techmonkey.no_soliciting.client;

import com.techmonkey.no_soliciting.NoSoliciting;
import com.techmonkey.no_soliciting.block.NoSolicitingSignBlock;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public final class NoSolicitingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide()
                    || !player.isShiftKeyDown()
                    || !player.getItemInHand(hand).is(Items.STICK)) {
                return InteractionResult.PASS;
            }

            BlockState state = level.getBlockState(hit.getBlockPos());
            if (!state.is(NoSoliciting.NO_SOLICITING_SIGN)) {
                return InteractionResult.PASS;
            }

            Minecraft.getInstance().setScreenAndShow(new NoSolicitingScreen(
                    hit.getBlockPos().immutable(),
                    state.getValue(NoSolicitingSignBlock.RADIUS_INDEX)
            ));
            return InteractionResult.SUCCESS;
        });

        LevelRenderEvents.COLLECT_SUBMITS.register(BoundaryRenderer::render);
    }
}

