package com.piotrek.groundworksloader.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
import com.piotrek.groundworksloader.network.LoaderInputPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class LoaderInputHandler {

    private static float lastThrottle;
    private static float lastSteer;
    private static float lastBoomLift;
    private static float lastBucketTilt;
    private static boolean lastHornActive;
    private static int keepaliveTicks;

    private LoaderInputHandler() {}

    public static void clientTick(Minecraft client) {
        if (client.player == null) return;

        if (client.player.getVehicle() instanceof GroundworksLoaderEntity loader) {
            boolean inGame = client.mouseHandler != null && client.mouseHandler.isMouseGrabbed() && client.getWindow() != null;

            boolean keyForward = (client.options.keyUp != null && client.options.keyUp.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.forward())
                    || (inGame && InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_W));
            boolean keyBackward = (client.options.keyDown != null && client.options.keyDown.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.backward())
                    || (inGame && InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_S));
            boolean keyLeft = (client.options.keyLeft != null && client.options.keyLeft.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.left())
                    || (inGame && InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_A));
            boolean keyRight = (client.options.keyRight != null && client.options.keyRight.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.right())
                    || (inGame && InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_D));

            boolean boomUp = (LoaderKeyBindings.KEY_BOOM_UP != null && LoaderKeyBindings.KEY_BOOM_UP.isDown())
                    || (inGame && InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_UP));
            boolean boomDown = (LoaderKeyBindings.KEY_BOOM_DOWN != null && LoaderKeyBindings.KEY_BOOM_DOWN.isDown())
                    || (inGame && InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_DOWN));

            boolean bucketCurl = (LoaderKeyBindings.KEY_BUCKET_CURL != null && LoaderKeyBindings.KEY_BUCKET_CURL.isDown())
                    || (inGame && InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_LEFT));
            boolean bucketDump = (LoaderKeyBindings.KEY_BUCKET_DUMP != null && LoaderKeyBindings.KEY_BUCKET_DUMP.isDown())
                    || (inGame && InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_RIGHT));

            boolean hornActive = (LoaderKeyBindings.KEY_HORN != null && LoaderKeyBindings.KEY_HORN.isDown())
                    || (inGame && InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_C));

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

            if (bucketCurl) bucketTilt -= 1.0F;
            if (bucketDump) bucketTilt += 1.0F;

            boolean changed = throttle != lastThrottle
                    || steer != lastSteer
                    || boomLift != lastBoomLift
                    || bucketTilt != lastBucketTilt
                    || hornActive != lastHornActive;

            if (changed || --keepaliveTicks <= 0) {
                ClientPlayNetworking.send(new LoaderInputPayload(
                        throttle, steer, boomLift, bucketTilt, hornActive
                ));

                lastThrottle = throttle;
                lastSteer = steer;
                lastBoomLift = boomLift;
                lastBucketTilt = bucketTilt;
                lastHornActive = hornActive;
                keepaliveTicks = 3;
            }
        } else {
            lastThrottle = 0.0F;
            lastSteer = 0.0F;
            lastBoomLift = 0.0F;
            lastBucketTilt = 0.0F;
            lastHornActive = false;
            keepaliveTicks = 0;
        }
    }
}
