package com.piotrek.groundworksloader.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
import com.piotrek.groundworksloader.network.LoaderInputPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Gathers operator keyboard inputs each client tick and transmits control packets to the server.
 *
 * <p>Supports:
 * <ul>
 *   <li><b>Driving (W / S / A / D)</b>: Forward / reverse throttle & articulated steering.</li>
 *   <li><b>Boom (Arrow Up / Arrow Down)</b>: Raise / lower loader arms.</li>
 *   <li><b>Bucket (Arrow Left / Arrow Right)</b>: Curl (tilt up) / dump (tilt down) bucket.</li>
 * </ul>
 */
public final class LoaderInputHandler {

    private static float lastThrottle;
    private static float lastSteer;
    private static float lastBoomLift;
    private static float lastBucketTilt;
    private static int keepaliveTicks;

    private LoaderInputHandler() {}

    public static void clientTick(Minecraft client) {
        if (client.player == null) {
            return;
        }

        if (client.player.getVehicle() instanceof GroundworksLoaderEntity loader) {
            boolean inGame = client.mouseHandler != null && client.mouseHandler.isMouseGrabbed();

            // Read driving inputs (WASD)
            boolean keyForward = (client.options.keyUp != null && client.options.keyUp.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.forward())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_W));
            boolean keyBackward = (client.options.keyDown != null && client.options.keyDown.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.backward())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_S));
            boolean keyLeft = (client.options.keyLeft != null && client.options.keyLeft.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.left())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_A));
            boolean keyRight = (client.options.keyRight != null && client.options.keyRight.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.right())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_D));

            // Read boom lift inputs (Arrow Up / Down)
            boolean boomUp = (LoaderKeyBindings.KEY_BOOM_UP != null && LoaderKeyBindings.KEY_BOOM_UP.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_UP));
            boolean boomDown = (LoaderKeyBindings.KEY_BOOM_DOWN != null && LoaderKeyBindings.KEY_BOOM_DOWN.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_DOWN));

            // Read bucket tilt inputs (Arrow Left = open/dump, Arrow Right = close/curl)
            boolean bucketOpen = (LoaderKeyBindings.KEY_BUCKET_OPEN != null && LoaderKeyBindings.KEY_BUCKET_OPEN.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_LEFT));
            boolean bucketClose = (LoaderKeyBindings.KEY_BUCKET_CLOSE != null && LoaderKeyBindings.KEY_BUCKET_CLOSE.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_RIGHT));

            float throttle = 0.0F;
            float steer = 0.0F;
            float boomLift = 0.0F;
            float bucketTilt = 0.0F;

            if (keyForward) throttle += 1.0F;
            if (keyBackward) throttle -= 1.0F;
            if (keyLeft) steer -= 1.0F;
            if (keyRight) steer += 1.0F;

            if (boomUp) boomLift += 1.0F;
            if (boomDown) boomLift -= 1.0F;

            if (bucketOpen) bucketTilt += 1.0F;  // Left Arrow opens bucket (+angle = open)
            if (bucketClose) bucketTilt -= 1.0F; // Right Arrow closes bucket (-angle = closed)

            boolean changed = throttle != lastThrottle
                    || steer != lastSteer
                    || boomLift != lastBoomLift
                    || bucketTilt != lastBucketTilt;

            if (changed || --keepaliveTicks <= 0) {
                ClientPlayNetworking.send(new LoaderInputPayload(
                        throttle, steer, boomLift, bucketTilt
                ));

                lastThrottle = throttle;
                lastSteer = steer;
                lastBoomLift = boomLift;
                lastBucketTilt = bucketTilt;
                keepaliveTicks = 3;
            }
        } else {
            lastThrottle = 0.0F;
            lastSteer = 0.0F;
            lastBoomLift = 0.0F;
            lastBucketTilt = 0.0F;
            keepaliveTicks = 0;
        }
    }
}
