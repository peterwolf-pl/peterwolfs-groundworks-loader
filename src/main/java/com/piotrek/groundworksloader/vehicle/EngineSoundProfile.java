package com.piotrek.groundworksloader.vehicle;

/** Maps loader drive and hydraulic load onto the excavator diesel sound layers. */
public final class EngineSoundProfile {

    private static final float IDLE_VOLUME = 0.42F;
    private static final float DRIVE_VOLUME = 0.64F;
    private static final float IDLE_PITCH = 0.94F;
    private static final float DRIVE_PITCH = 1.12F;
    private static final float LOAD_LAYER_VOLUME = 0.86F;

    private EngineSoundProfile() {}

    public record Mix(float volume, float pitch, float loadVolume, float loadPitch) {}

    public static float machineLoad(float speed, boolean isHydraulicActive) {
        float driveLoad = Math.min(1.0F, Math.abs(speed) / LoaderMovementController.MAX_FORWARD_SPEED);
        float hydraulicLoad = isHydraulicActive ? 0.65F : 0.0F;
        return Math.max(hydraulicLoad, driveLoad * 0.78F);
    }

    public static Mix forLoaderState(float speed, boolean isHydraulicActive) {
        float driveLoad = Math.min(1.0F, Math.abs(speed) / LoaderMovementController.MAX_FORWARD_SPEED);
        float hydraulicLoad = isHydraulicActive ? 0.65F : 0.0F;
        float load = machineLoad(speed, isHydraulicActive);
        float lug = hydraulicLoad * (1.0F - driveLoad);

        return new Mix(
                IDLE_VOLUME + (DRIVE_VOLUME - IDLE_VOLUME) * load,
                IDLE_PITCH + (DRIVE_PITCH - IDLE_PITCH) * load,
                LOAD_LAYER_VOLUME * load,
                0.88F + 0.16F * driveLoad - 0.06F * lug
        );
    }
}
