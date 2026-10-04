package com.piotrek.groundworksloader.vehicle;

/**
 * Maps authoritative loader vehicle speed, engine load and hydraulic activity to a diesel sound profile.
 */
public final class EngineSoundProfile {

    private static final float IDLE_VOLUME = 0.60F;
    private static final float LOAD_VOLUME = 0.85F;
    private static final float IDLE_PITCH = 0.72F;
    private static final float LOAD_PITCH = 1.05F;

    private EngineSoundProfile() {}

    public record Mix(float volume, float pitch) {}

    public static Mix forLoaderState(float speed, boolean isHydraulicActive) {
        float absSpeed = Math.abs(speed);
        float load = Math.min(1.0F, absSpeed / LoaderMovementController.MAX_FORWARD_SPEED);
        if (isHydraulicActive) {
            load = Math.max(load, 0.45F);
        }

        return new Mix(
                IDLE_VOLUME + (LOAD_VOLUME - IDLE_VOLUME) * load,
                IDLE_PITCH + (LOAD_PITCH - IDLE_PITCH) * load
        );
    }
}
