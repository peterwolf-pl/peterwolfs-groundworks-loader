package com.piotrek.groundworksloader.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/**
 * Renders the loader bucket contents as a generated loose-material mound.
 *
 * <p>The mesh is independent from material IDs. The currently carried
 * {@link GranularMaterial} supplies its source block, and that block supplies the
 * texture namespace/path. Adding another Groundworks material therefore does not
 * require another loader model part or another material switch statement.
 */
public final class GranularBucketContentsRenderer {

    // LoaderModel bucket hierarchy, shared here so the loose-material mesh follows
    // the exact same articulated front frame, boom and bucket transforms.
    private static final float LIFT_ARM_Y_PX = -5.0F;
    private static final float LIFT_ARM_Z_PX = 8.0F;
    private static final float BUCKET_Y_PX = 19.5F;
    private static final float BUCKET_Z_PX = 34.0F;

    private static final int X_SEGMENTS = 6;
    private static final int Z_SEGMENTS = 5;

    private GranularBucketContentsRenderer() {}

    public static void submit(
            LoaderRenderState state,
            PoseStack stack,
            SubmitNodeCollector collector
    ) {
        if (state.carriedUnits <= 0 || state.carriedMaterialId <= 0 || state.fillRatio <= 0.0F) {
            return;
        }

        GranularMaterial material = GranularMaterialRegistry.byId(state.carriedMaterialId);
        Identifier texture = textureFor(material);
        if (texture == null) {
            return;
        }

        float fill = Math.clamp(state.fillRatio, 0.0F, 1.0F);

        stack.pushPose();

        // front_chassis
        stack.rotateDegrees(Axis.YP, state.steerAngle);

        // lift_arms
        stack.translate(0.0F, LIFT_ARM_Y_PX / 16.0F, LIFT_ARM_Z_PX / 16.0F);
        stack.rotateDegrees(Axis.XP, state.boomAngle);

        // bucket
        stack.translate(0.0F, BUCKET_Y_PX / 16.0F, BUCKET_Z_PX / 16.0F);
        stack.rotateDegrees(Axis.XP, -state.bucketAngle);

        collector.submitCustomGeometry(
                stack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> emitMound(consumer, pose, fill, state.lightCoords)
        );

        stack.popPose();
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

    private static void emitMound(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float fill,
            int packedLight
    ) {
        // At low fill the material occupies only the rear-middle of the bowl.
        // As volume increases, the footprint grows toward the side plates and lip.
        float footprint = (float) Math.sqrt(fill);
        float halfWidthPx = lerp(9.0F, 20.5F, footprint);
        float backZPx = lerp(6.5F, 3.0F, footprint);
        float frontZPx = lerp(11.5F, 18.0F, footprint);
        float baseYPx = 4.15F;

        // Full bucket rises close to the old spill-guard height, but the surface
        // remains a mound rather than two rectangular cuboids.
        float peakRisePx = lerp(1.4F, 9.2F, fill);

        Vector3f[][] vertices = new Vector3f[X_SEGMENTS + 1][Z_SEGMENTS + 1];

        for (int x = 0; x <= X_SEGMENTS; x++) {
            float fx = (float) x / X_SEGMENTS;
            float localX = lerp(-halfWidthPx, halfWidthPx, fx);
            float nx = Math.abs((fx - 0.5F) * 2.0F);

            for (int z = 0; z <= Z_SEGMENTS; z++) {
                float fz = (float) z / Z_SEGMENTS;
                float localZ = lerp(backZPx, frontZPx, fz);

                // Peak is slightly rear of center, as loose material loads against
                // the back wall during scooping. Height falls continuously toward
                // the side cheeks and cutting lip.
                float longitudinal = Math.abs((fz - 0.38F) / 0.62F);
                float profile = 1.0F
                        - 0.52F * nx * nx
                        - 0.38F * longitudinal * longitudinal;
                profile = Math.clamp(profile, 0.12F, 1.0F);

                // Small deterministic crown breaks the perfectly planar look without
                // introducing animation or nondeterministic visual jitter.
                float crown = 0.16F
                        * (float) Math.sin((fx * 2.0F + fz * 1.5F) * Math.PI);
                float surfaceYPx = baseYPx - peakRisePx * Math.clamp(profile + crown, 0.10F, 1.0F);

                vertices[x][z] = px(localX, surfaceYPx, localZ);
            }
        }

        // Faceted top surface. The segment count is high enough to read as a loose
        // pile while staying very cheap compared with terrain meshing.
        for (int x = 0; x < X_SEGMENTS; x++) {
            for (int z = 0; z < Z_SEGMENTS; z++) {
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

        // Close the perimeter down to the bucket floor. These skirts make the pile
        // feel volumetric from side views but follow the sloped top edge, so the
        // contents never read as a rectangular box.
        emitSideX(consumer, pose, vertices, 0, baseYPx, -1.0F, packedLight);
        emitSideX(consumer, pose, vertices, X_SEGMENTS, baseYPx, 1.0F, packedLight);
        emitSideZ(consumer, pose, vertices, 0, baseYPx, -1.0F, packedLight);
        emitSideZ(consumer, pose, vertices, Z_SEGMENTS, baseYPx, 1.0F, packedLight);
    }

    private static void emitSideX(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vector3f[][] vertices,
            int xIndex,
            float baseYPx,
            float normalX,
            int packedLight
    ) {
        Vector3f normal = new Vector3f(normalX, 0.0F, 0.0F);
        for (int z = 0; z < Z_SEGMENTS; z++) {
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
            int packedLight
    ) {
        Vector3f normal = new Vector3f(0.0F, 0.0F, normalZ);
        for (int x = 0; x < X_SEGMENTS; x++) {
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
