package com.piotrek.groundworksloader.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.groundworksloader.GroundworksLoaderMod;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Keybindings for the wheel loader boom elevation and bucket tilt.
 */
public final class LoaderKeyBindings {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(GroundworksLoaderMod.id("controls"));

    public static KeyMapping KEY_BOOM_UP;
    public static KeyMapping KEY_BOOM_DOWN;
    public static KeyMapping KEY_BUCKET_CURL;
    public static KeyMapping KEY_BUCKET_DUMP;

    private LoaderKeyBindings() {}

    public static void register() {
        KEY_BOOM_UP = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_loader.boom_up",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_UP,
                CATEGORY
        ));

        KEY_BOOM_DOWN = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_loader.boom_down",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_DOWN,
                CATEGORY
        ));

        KEY_BUCKET_CURL = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_loader.bucket_curl",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_LEFT,
                CATEGORY
        ));

        KEY_BUCKET_DUMP = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_loader.bucket_dump",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RIGHT,
                CATEGORY
        ));
    }
}
