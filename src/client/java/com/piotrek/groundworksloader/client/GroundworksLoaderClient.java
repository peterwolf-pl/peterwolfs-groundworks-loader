package com.piotrek.groundworksloader.client;

import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.client.input.LoaderInputHandler;
import com.piotrek.groundworksloader.client.input.LoaderKeyBindings;
import com.piotrek.groundworksloader.client.model.LoaderModel;
import com.piotrek.groundworksloader.client.render.LoaderHudOverlay;
import com.piotrek.groundworksloader.client.render.LoaderRenderer;
import com.piotrek.groundworksloader.client.sound.LoaderEngineSoundController;
import com.piotrek.groundworksloader.client.sound.LoaderHornSoundController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class GroundworksLoaderClient implements ClientModInitializer {

    public static final ModelLayerLocation LOADER_LAYER =
            new ModelLayerLocation(GroundworksLoaderMod.id("loader"), "main");
    public static final ModelLayerLocation GIANT_LOADER_LAYER =
            new ModelLayerLocation(GroundworksLoaderMod.id("giant_loader"), "main");

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(LOADER_LAYER, LoaderModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(GIANT_LOADER_LAYER, LoaderModel::createGiantBodyLayer);
        EntityRendererRegistry.register(GroundworksLoaderMod.LOADER, LoaderRenderer::new);
        EntityRendererRegistry.register(GroundworksLoaderMod.GIANT_LOADER, LoaderRenderer::new);
        LoaderKeyBindings.register();

        ClientTickEvents.END_CLIENT_TICK.register(LoaderInputHandler::clientTick);
        ClientTickEvents.END_CLIENT_TICK.register(LoaderEngineSoundController::clientTick);
        ClientTickEvents.END_CLIENT_TICK.register(LoaderHornSoundController::clientTick);

        LoaderHudOverlay.register();
    }
}
