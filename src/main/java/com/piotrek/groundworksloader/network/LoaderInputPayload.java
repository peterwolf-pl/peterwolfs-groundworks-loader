package com.piotrek.groundworksloader.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LoaderInputPayload(
        float throttle,
        float steer,
        float boomLift,
        float bucketTilt,
        boolean hornActive
) implements CustomPacketPayload {

    public static final Type<LoaderInputPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("pw_groundworks_loader", "loader_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LoaderInputPayload> CODEC = new StreamCodec<>() {
        @Override
        public LoaderInputPayload decode(RegistryFriendlyByteBuf buffer) {
            return new LoaderInputPayload(
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readBoolean()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, LoaderInputPayload payload) {
            buffer.writeFloat(payload.throttle);
            buffer.writeFloat(payload.steer);
            buffer.writeFloat(payload.boomLift);
            buffer.writeFloat(payload.bucketTilt);
            buffer.writeBoolean(payload.hornActive);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
