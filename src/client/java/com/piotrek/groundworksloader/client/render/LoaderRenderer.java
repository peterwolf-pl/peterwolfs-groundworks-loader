package com.piotrek.groundworksloader.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.bucket.LoaderBucketController;
import com.piotrek.groundworksloader.client.GroundworksLoaderClient;
import com.piotrek.groundworksloader.client.model.LoaderModel;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * 26.3 entity renderer for the 4-wheel industrial wheel loader.
 */
public class LoaderRenderer extends EntityRenderer<GroundworksLoaderEntity, LoaderRenderState> {

    public static final Identifier TEXTURE = GroundworksLoaderMod.id("textures/entity/loader.png");

    private final LoaderModel model;

    public LoaderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new LoaderModel(context.bakeLayer(GroundworksLoaderClient.LOADER_LAYER));
        this.shadowRadius = 1.9F;
    }

    @Override
    public LoaderRenderState createRenderState() {
        return new LoaderRenderState();
    }

    @Override
    public void extractRenderState(GroundworksLoaderEntity entity, LoaderRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.baseYaw = entity.getYRot(partialTick);
        state.basePitch = entity.getVehiclePitch();
        state.baseRoll = entity.getVehicleRoll();

        state.boomAngle = entity.getBoomAngle();
        state.bucketAngle = entity.getBucketAngle();
        state.steerAngle = entity.getSteerAngle();
        state.forwardSpeed = entity.getForwardSpeed();
        state.wheelRotation = entity.getWheelRotation();

        state.carriedMaterialId = entity.getCarriedMaterialId();
        state.carriedUnits = entity.getCarriedUnits();
        state.fillRatio = (float) entity.getCarriedUnits() / (float) LoaderBucketController.BUCKET_CAPACITY;

        state.isScooping = entity.isScooping();
        state.isDumping = entity.isDumping();
        state.isEngineRunning = entity.isEngineRunning();

        state.beaconSpin = (entity.tickCount + partialTick) * 0.75F;
        state.beaconFlash = state.isEngineRunning && ((entity.tickCount / 4) % 2 == 0);
    }

    @Override
    public void submit(LoaderRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
        stack.pushPose();

        // 1. Vehicle heading (facing where rear frame points)
        stack.rotateDegrees(Axis.YP, -state.baseYaw);

        // 2. Ground pitch & roll
        if (Math.abs(state.basePitch) > 0.01F) {
            stack.rotateDegrees(Axis.XP, state.basePitch);
        }
        if (Math.abs(state.baseRoll) > 0.01F) {
            stack.rotateDegrees(Axis.ZP, state.baseRoll);
        }

        // Standard Minecraft entity model coordinate transform
        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.translate(0.0F, -1.5F, 0.0F);

        this.model.setupAnim(state);

        // Render full model with cutout render type
        collector.submitModel(
                this.model,
                state,
                stack,
                RenderTypes.entityCutout(TEXTURE),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );

        // Render carried material as a generated loose-material surface using the
        // Groundworks material's source-block texture. This works for cobblestone
        // and future registry materials without material-ID-specific loader code.
        GranularBucketContentsRenderer.submit(state, stack, collector);

        stack.popPose();
    }
}
