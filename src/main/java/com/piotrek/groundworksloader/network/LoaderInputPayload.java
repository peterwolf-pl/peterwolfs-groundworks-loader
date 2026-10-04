package com.piotrek.groundworksloader.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Compact client operator intent packet for the wheel loader.
 *
 * <p>Transmits:
 * <ul>
 *   <li>Throttle (-1.0 = reverse / brake, +1.0 = forward, 0.0 = neutral)</li>
 *   <li>Steer (-1.0 = left, +1.0 = right, 0.0 = straight)</li>
 *   <li>Boom Lift (-1.0 = lower boom, +1.0 = raise boom, 0.0 = hold)</li>
 *   <li>Bucket Tilt (-1.0 = curl / tilt up, +1.0 = dump / tilt down, 0.0 = hold)</li>
 * </ul>
 */
public record LoaderInputPayload(
        float throttle,
        float steer,
        float boomLift,
        float bucketTilt
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
                    buffer.readFloat()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, LoaderInputPayload payload) {
            buffer.writeFloat(payload.throttle);
            buffer.writeFloat(payload.steer);
            buffer.writeFloat(payload.boomLift);
            buffer.writeFloat(payload.bucketTilt);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
