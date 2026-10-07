package com.piotrek.groundworksloader.vehicle;

import net.minecraft.world.phys.Vec3;

/**
 * Converts the loader model's exhaust-stack outlet into world coordinates.
 *
 * <p>The outlet constants are tied directly to LoaderModel's stack geometry:
 * cap center X = 7.25 model units, cap center Z = -14.75, cap top Y = -24.5.
 * The X sign and Y origin account for LoaderRenderer's standard
 * scale(-1,-1,1) and translate(0,-1.5,0) model transform.
 */
public final class LoaderExhaustTransform {

    private static final double LOCAL_X = -7.25D / 16.0D;
    private static final double LOCAL_Y = 1.5D + 24.5D / 16.0D;
    private static final double LOCAL_Z = -14.75D / 16.0D;

    private LoaderExhaustTransform() {}

    public static Vec3 getExhaustWorldPosition(
            Vec3 vehiclePosition,
            float vehicleYaw,
            float vehiclePitch,
            float vehicleRoll
    ) {
        double x = LOCAL_X;
        double y = LOCAL_Y;
        double z = LOCAL_Z;

        // Renderer order after model-space conversion: roll, pitch, then yaw.
        double roll = Math.toRadians(vehicleRoll);
        double cosR = Math.cos(roll);
        double sinR = Math.sin(roll);
        double xRolled = (x * cosR) - (y * sinR);
        double yRolled = (x * sinR) + (y * cosR);

        double pitch = Math.toRadians(vehiclePitch);
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);
        double yPitched = (yRolled * cosP) - (z * sinP);
        double zPitched = (yRolled * sinP) + (z * cosP);

        double yaw = Math.toRadians(-vehicleYaw);
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double xWorld = (xRolled * cosY) + (zPitched * sinY);
        double zWorld = (-xRolled * sinY) + (zPitched * cosY);

        return vehiclePosition.add(xWorld, yPitched, zWorld);
    }
}
