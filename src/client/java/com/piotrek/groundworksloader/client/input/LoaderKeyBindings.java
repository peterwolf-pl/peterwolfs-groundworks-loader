package com.piotrek.groundworksloader.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.groundworksloader.GroundworksLoaderMod;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public final class LoaderKeyBindings {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(GroundworksLoaderMod.id("controls"));

    public static KeyMapping KEY_BOOM_UP;
    public static KeyMapping KEY_BOOM_DOWN;
    public static KeyMapping KEY_BUCKET_CURL;
    public static KeyMapping KEY_BUCKET_DUMP;
    public static KeyMapping KEY_HORN;

    private LoaderKeyBindings() {}

    public static void register() {
        KEY_BOOM_UP = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pw_groundworks_loader.boom_up",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_UP,
                CATEGORY
        ));

        KEY_BOOM_DOWN = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pw_groundworks_loader.boom_down",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_DOWN,
                CATEGORY
        ));

        KEY_BUCKET_CURL = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pw_groundworks_loader.bucket_close",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_LEFT,
                CATEGORY
        ));

        KEY_BUCKET_DUMP = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pw_groundworks_loader.bucket_open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_RIGHT,
                CATEGORY
        ));

        KEY_HORN = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pw_groundworks_loader.horn",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_C,
                CATEGORY
        ));
    }
}
