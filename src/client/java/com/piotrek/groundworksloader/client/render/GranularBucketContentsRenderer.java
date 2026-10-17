package com.piotrek.groundworksloader.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piotrek.groundworks.api.material.GranularComposition;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/**
 * Renders the loader bucket contents as a generated loose-material heaped mound.
 *
 * <p>Supports mixed materials with multi-texture patch sampling, and dynamically
 * scales to a prominent, crowned volumetric mound spanning the full 2.8m width
 * and depth of the wheel loader bucket.</p>
 */
public final class GranularBucketContentsRenderer {

    // LoaderModel bucket hierarchy transforms
    private static final float LIFT_ARM_Y_PX = -5.0F;
    private static final float LIFT_ARM_Z_PX = 8.0F;
    private static final float BUCKET_Y_PX = 19.5F;
    private static final float BUCKET_Z_PX = 34.0F;

    private static final int X_SEGMENTS = 8;
    private static final int Z_SEGMENTS = 7;
    private static final long VISUAL_SEED = 0x574845454C4C4F41L; // "WHEELLOA"

    private GranularBucketContentsRenderer() {}

    public static void submit(
            LoaderRenderState state,
            PoseStack stack,
            SubmitNodeCollector collector
    ) {
        if (state.carriedUnits <= 0 || state.fillRatio <= 0.0F) {
            return;
        }

        GranularComposition composition = compositionFrom(state);
        if (composition.isEmpty()) {
            return;
        }

        float fill = Math.clamp(state.fillRatio, 0.0F, 1.0F);

        stack.pushPose();

        // front_chassis
        stack.mulPose(Axis.YP.rotationDegrees(state.steerAngle));

        // lift_arms
        stack.translate(0.0F, LIFT_ARM_Y_PX / 16.0F, LIFT_ARM_Z_PX / 16.0F);
        stack.mulPose(Axis.XP.rotationDegrees(state.boomAngle));

        // bucket
        stack.translate(0.0F, BUCKET_Y_PX / 16.0F, BUCKET_Z_PX / 16.0F);
        stack.mulPose(Axis.XP.rotationDegrees(-state.bucketAngle));

        int[] counts = composition.toArray();
        for (int materialId = 1; materialId < counts.length; materialId++) {
            if (counts[materialId] <= 0) {
                continue;
            }

            GranularMaterial material = GranularMaterialRegistry.byId(materialId);
            Identifier texture = textureFor(material);
            if (texture == null) {
                continue;
            }

            final int selectedMaterialId = materialId;
            collector.submitCustomGeometry(
                    stack,
                    RenderTypes.entityCutout(texture),
                    (pose, consumer) -> emitMoundForMaterial(
                            consumer,
                            pose,
                            fill,
                            state.lightCoords,
                            composition,
                            selectedMaterialId
                    )
            );
        }

        stack.popPose();
    }

    private static GranularComposition compositionFrom(LoaderRenderState state) {
        GranularComposition composition = new GranularComposition();
        if (state.dirtUnits > 0) composition.add(GranularMaterialRegistry.DIRT, state.dirtUnits);
        if (state.sandUnits > 0) composition.add(GranularMaterialRegistry.SAND, state.sandUnits);
        if (state.gravelUnits > 0) composition.add(GranularMaterialRegistry.GRAVEL, state.gravelUnits);
        if (state.cobblestoneUnits > 0) composition.add(GranularMaterialRegistry.COBBLESTONE, state.cobblestoneUnits);

        if (composition.isEmpty() && state.carriedMaterialId > 0 && state.carriedUnits > 0) {
            composition.add(GranularMaterialRegistry.byId(state.carriedMaterialId), state.carriedUnits);
        }
        return composition;
    }

    static Identifier textureFor(GranularMaterial material) {
        if (material == null || material.id() == 0 || material.sourceBlock() == null) {
            return null;
        }

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(material.sourceBlock());
        if (blockId == null) {
            return null;
        }

        return Identifier.fromNamespaceAndPath(
                blockId.getNamespace(),
                "textures/block/" + blockId.getPath() + ".png"
        );
    }

    private static void emitMoundForMaterial(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float fill,
            int packedLight,
            GranularComposition composition,
            int selectedMaterialId
    ) {
        // Grand heaped footprint spanning the full 44px (2.75m) inner bucket width
        float footprint = (float) Math.sqrt(fill);
        float halfWidthPx = lerp(12.0F, 21.8F, footprint);
        float backZPx = lerp(6.0F, 1.8F, footprint);
        float frontZPx = lerp(12.0F, 20.8F, footprint);
        float baseYPx = 4.2F;

        // Peak rise forms a proud, majestic mound crowned near the spill guard
        float peakRisePx = lerp(2.2F, 15.8F, fill);
        float sideDrop = lerp(0.50F, 0.28F, fill);
        float longDrop = lerp(0.45F, 0.25F, fill);

        Vector3f[][] vertices = new Vector3f[X_SEGMENTS + 1][Z_SEGMENTS + 1];

        for (int x = 0; x <= X_SEGMENTS; x++) {
            float fx = (float) x / X_SEGMENTS;
            float localX = lerp(-halfWidthPx, halfWidthPx, fx);
            float nx = Math.abs((fx - 0.5F) * 2.0F);

            for (int z = 0; z <= Z_SEGMENTS; z++) {
                float fz = (float) z / Z_SEGMENTS;
                float localZ = lerp(backZPx, frontZPx, fz);

                // Peak is crowned slightly behind center against backplate, falling
                // off smoothly to cheek edges and front cutting lip.
                float longitudinal = Math.abs((fz - 0.40F) / 0.60F);
                float profile = 1.0F
                        - sideDrop * nx * nx
                        - longDrop * longitudinal * longitudinal;
                profile = Math.clamp(profile, 0.12F, 1.05F);

                // Organic granular ripple and crest variation
                float crown = 0.16F * (float) Math.sin((fx * 2.5F + fz * 2.0F) * Math.PI)
                        + 0.08F * (float) Math.cos((fx * 4.0F - fz * 3.0F) * Math.PI);
                float surfaceYPx = baseYPx - peakRisePx * Math.clamp(profile + crown * fill, 0.10F, 1.15F);

                vertices[x][z] = px(localX, surfaceYPx, localZ);
            }
        }

        // Top faceted mound surface
        for (int x = 0; x < X_SEGMENTS; x++) {
            for (int z = 0; z < Z_SEGMENTS; z++) {
                int sampled = composition.sampleVisualMaterial(
                        VISUAL_SEED,
                        x,
                        6,
                        z
                );

                if (sampled != selectedMaterialId) {
                    continue;
                }

                Vector3f v00 = vertices[x][z];
                Vector3f v01 = vertices[x][z + 1];
                Vector3f v11 = vertices[x + 1][z + 1];
                Vector3f v10 = vertices[x + 1][z];

                Vector3f normal = faceNormal(v00, v01, v11);
                emit(consumer, pose, v00, normal,
                        (float) x / X_SEGMENTS, (float) z / Z_SEGMENTS, packedLight);
                emit(consumer, pose, v01, normal,
                        (float) x / X_SEGMENTS, (float) (z + 1) / Z_SEGMENTS, packedLight);
                emit(consumer, pose, v11, normal,
                        (float) (x + 1) / X_SEGMENTS, (float) (z + 1) / Z_SEGMENTS, packedLight);
                emit(consumer, pose, v10, normal,
                        (float) (x + 1) / X_SEGMENTS, (float) z / Z_SEGMENTS, packedLight);
            }
        }

        // Side skirt seals down to bucket floor
        emitSideX(consumer, pose, vertices, 0, baseYPx, -1.0F, packedLight, composition, selectedMaterialId);
        emitSideX(consumer, pose, vertices, X_SEGMENTS, baseYPx, 1.0F, packedLight, composition, selectedMaterialId);
        emitSideZ(consumer, pose, vertices, 0, baseYPx, -1.0F, packedLight, composition, selectedMaterialId);
        emitSideZ(consumer, pose, vertices, Z_SEGMENTS, baseYPx, 1.0F, packedLight, composition, selectedMaterialId);
    }

    private static void emitSideX(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vector3f[][] vertices,
            int xIndex,
            float baseYPx,
            float normalX,
            int packedLight,
            GranularComposition composition,
            int selectedMaterialId
    ) {
        Vector3f normal = new Vector3f(normalX, 0.0F, 0.0F);
        for (int z = 0; z < Z_SEGMENTS; z++) {
            int sampled = composition.sampleVisualMaterial(
                    VISUAL_SEED,
                    xIndex,
                    3,
                    z
            );
            if (sampled != selectedMaterialId) {
                continue;
            }

            Vector3f top0 = vertices[xIndex][z];
            Vector3f top1 = vertices[xIndex][z + 1];
            Vector3f bottom1 = new Vector3f(top1.x, baseYPx / 16.0F, top1.z);
            Vector3f bottom0 = new Vector3f(top0.x, baseYPx / 16.0F, top0.z);

            float u0 = (float) z / Z_SEGMENTS;
            float u1 = (float) (z + 1) / Z_SEGMENTS;
            emit(consumer, pose, bottom0, normal, u0, 1.0F, packedLight);
            emit(consumer, pose, bottom1, normal, u1, 1.0F, packedLight);
            emit(consumer, pose, top1, normal, u1, 0.0F, packedLight);
            emit(consumer, pose, top0, normal, u0, 0.0F, packedLight);
        }
    }

    private static void emitSideZ(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vector3f[][] vertices,
            int zIndex,
            float baseYPx,
            float normalZ,
            int packedLight,
            GranularComposition composition,
            int selectedMaterialId
    ) {
        Vector3f normal = new Vector3f(0.0F, 0.0F, normalZ);
        for (int x = 0; x < X_SEGMENTS; x++) {
            int sampled = composition.sampleVisualMaterial(
                    VISUAL_SEED,
                    x,
                    3,
                    zIndex
            );
            if (sampled != selectedMaterialId) {
                continue;
            }

            Vector3f top0 = vertices[x][zIndex];
            Vector3f top1 = vertices[x + 1][zIndex];
            Vector3f bottom1 = new Vector3f(top1.x, baseYPx / 16.0F, top1.z);
            Vector3f bottom0 = new Vector3f(top0.x, baseYPx / 16.0F, top0.z);

            float u0 = (float) x / X_SEGMENTS;
            float u1 = (float) (x + 1) / X_SEGMENTS;
            emit(consumer, pose, bottom0, normal, u0, 1.0F, packedLight);
            emit(consumer, pose, bottom1, normal, u1, 1.0F, packedLight);
            emit(consumer, pose, top1, normal, u1, 0.0F, packedLight);
            emit(consumer, pose, top0, normal, u0, 0.0F, packedLight);
        }
    }

    private static void emit(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vector3f vertex,
            Vector3f normal,
            float u,
            float v,
            int packedLight
    ) {
        consumer.addVertex(pose, vertex.x, vertex.y, vertex.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }

    private static Vector3f faceNormal(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f ab = new Vector3f(b).sub(a);
        Vector3f ac = new Vector3f(c).sub(a);
        return ab.cross(ac).normalize();
    }

    private static Vector3f px(float x, float y, float z) {
        return new Vector3f(x / 16.0F, y / 16.0F, z / 16.0F);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
