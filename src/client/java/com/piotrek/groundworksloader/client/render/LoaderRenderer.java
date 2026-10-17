package com.piotrek.groundworksloader.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.client.GroundworksLoaderClient;
import com.piotrek.groundworksloader.client.model.LoaderModel;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
import com.piotrek.groundworksloader.entity.LoaderVariant;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * 26.3 entity renderer for the 4-wheel industrial wheel loader.
 */
public class LoaderRenderer extends EntityRenderer<GroundworksLoaderEntity, LoaderRenderState> {

    public static final Identifier TEXTURE = GroundworksLoaderMod.id("textures/entity/loader.png");
    public static final Identifier GIANT_TEXTURE = GroundworksLoaderMod.id("textures/entity/giant_loader.png");

    private final LoaderModel standardModel;
    private final LoaderModel giantModel;

    public LoaderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.standardModel = new LoaderModel(context.bakeLayer(GroundworksLoaderClient.LOADER_LAYER));
        this.giantModel = new LoaderModel(context.bakeLayer(GroundworksLoaderClient.GIANT_LOADER_LAYER));
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
        state.dirtUnits = entity.getDirtUnits();
        state.sandUnits = entity.getSandUnits();
        state.gravelUnits = entity.getGravelUnits();
        state.cobblestoneUnits = entity.getCobblestoneUnits();
        state.fillRatio = (float) entity.getCarriedUnits() / (float) entity.capacity();
        state.renderScale = entity.renderScale();
        state.giantVariant = entity.variant() == LoaderVariant.GIANT;

        state.isScooping = entity.isScooping();
        state.isDumping = entity.isDumping();
        state.isEngineRunning = entity.isEngineRunning();

        state.beaconSpin = (entity.tickCount + partialTick) * 0.75F;
        state.beaconFlash = state.isEngineRunning && ((entity.tickCount / 4) % 2 == 0);
    }

    @Override
    protected AABB getBoundingBoxForCulling(GroundworksLoaderEntity entity) {
        AABB bounds = super.getBoundingBoxForCulling(entity);
        return entity.variant() == LoaderVariant.GIANT
                ? bounds.inflate(4.5D, 3.0D, 4.5D)
                : bounds;
    }

    @Override
    protected float getShadowRadius(LoaderRenderState state) {
        return state.giantVariant ? 3.2F : super.getShadowRadius(state);
    }

    @Override
    public void submit(LoaderRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
        stack.pushPose();

        // 1. Vehicle heading (facing where rear frame points)
        stack.mulPose(Axis.YP.rotationDegrees(-state.baseYaw));

        // 2. Ground pitch & roll
        if (Math.abs(state.basePitch) > 0.01F) {
            stack.mulPose(Axis.XP.rotationDegrees(state.basePitch));
        }
        if (Math.abs(state.baseRoll) > 0.01F) {
            stack.mulPose(Axis.ZP.rotationDegrees(state.baseRoll));
        }

        // Standard Minecraft entity model transform, enlarged for the giant variant.
        stack.scale(-state.renderScale, -state.renderScale, state.renderScale);
        stack.translate(0.0F, -1.5F, 0.0F);

        LoaderModel model = state.giantVariant ? this.giantModel : this.standardModel;
        Identifier texture = state.giantVariant ? GIANT_TEXTURE : TEXTURE;
        model.setupAnim(state);

        // Render full model with the variant's own texture and geometry.
        collector.submitModel(
                model,
                state,
                stack,
                RenderTypes.entityCutout(texture),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null
        );

        // Render carried material as a generated loose-material surface using the
        // Groundworks material's source-block texture. This works for cobblestone
        // and future registry materials without material-ID-specific loader code.
        GranularBucketContentsRenderer.submit(state, stack, collector);

        stack.popPose();
    }
}
