package com.techmonkey.no_soliciting.network;

import com.techmonkey.no_soliciting.NoSoliciting;
import com.techmonkey.no_soliciting.block.NoSolicitingSignBlock;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class NoSolicitingPackets {
    private NoSolicitingPackets() {
    }

    public static void registerServer() {
        PayloadTypeRegistry.serverboundPlay().register(UpdateRadiusPayload.TYPE, UpdateRadiusPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UpdateRadiusPayload.TYPE, NoSolicitingPackets::updateRadius);
    }

    private static void updateRadius(UpdateRadiusPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (payload.radiusIndex() < 0 || payload.radiusIndex() >= NoSolicitingSignBlock.RADII.length) {
            return;
        }
        if (!player.getMainHandItem().is(Items.STICK) && !player.getOffhandItem().is(Items.STICK)) {
            return;
        }
        if (player.blockPosition().distSqr(payload.pos()) > 64.0) {
            return;
        }

        BlockState state = player.level().getBlockState(payload.pos());
        if (!state.is(NoSoliciting.NO_SOLICITING_SIGN)) {
            return;
        }

        player.level().setBlock(
                payload.pos(),
                state.setValue(NoSolicitingSignBlock.RADIUS_INDEX, payload.radiusIndex()),
                Block.UPDATE_ALL
        );
    }
}

