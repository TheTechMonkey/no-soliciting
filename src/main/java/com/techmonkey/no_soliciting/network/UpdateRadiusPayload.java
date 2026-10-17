package com.techmonkey.no_soliciting.network;

import com.techmonkey.no_soliciting.NoSoliciting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UpdateRadiusPayload(BlockPos pos, int radiusIndex) implements CustomPacketPayload {
    public static final Type<UpdateRadiusPayload> TYPE = new Type<>(NoSoliciting.id("update_radius"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateRadiusPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            UpdateRadiusPayload::pos,
            ByteBufCodecs.VAR_INT,
            UpdateRadiusPayload::radiusIndex,
            UpdateRadiusPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

