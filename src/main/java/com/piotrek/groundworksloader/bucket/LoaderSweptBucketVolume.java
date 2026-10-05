package com.piotrek.groundworksloader.bucket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the world-space volume swept by the loader cutting edge between ticks.
 *
 * <p>Samples are deduplicated at Groundworks' 1/8-block microvoxel resolution,
 * not at whole-block resolution. Each surviving contact is later excavated with
 * the public Groundworks world-space brush, so the loader removes only terrain
 * intersected by the moving bucket instead of shaving complete cell layers.
 */
public final class LoaderSweptBucketVolume {

    public static final double MIN_SWEEP_DISTANCE = 0.006D;
    public static final double MAX_VALID_SWEEP_DISTANCE = 2.5D;
    public static final int SWEEP_SUBDIVISIONS = 4;
    private static final int MICRO_RESOLUTION = 8;

    public record Contact(BlockPos pos, Vec3 worldPoint) {}

    public record SweptResult(
            boolean valid,
            List<Contact> contacts,
            double movementDistance
    ) {
        public static final SweptResult EMPTY =
                new SweptResult(false, List.of(), 0.0D);
    }

    private record MicroKey(int x, int y, int z) {}

    private LoaderSweptBucketVolume() {}

    public static SweptResult compute(List<Vec3> previousEdge, List<Vec3> currentEdge) {
        if (previousEdge == null || currentEdge == null
                || previousEdge.isEmpty() || currentEdge.isEmpty()) {
            return SweptResult.EMPTY;
        }

        int count = Math.min(previousEdge.size(), currentEdge.size());
        if (count <= 0) {
            return SweptResult.EMPTY;
        }

        Vec3 previousCenter = previousEdge.get(count / 2);
        Vec3 currentCenter = currentEdge.get(count / 2);
        double distance = currentCenter.distanceTo(previousCenter);

        if (distance < MIN_SWEEP_DISTANCE || distance > MAX_VALID_SWEEP_DISTANCE) {
            return SweptResult.EMPTY;
        }

        Map<MicroKey, Contact> unique = new LinkedHashMap<>();

        for (int step = 0; step <= SWEEP_SUBDIVISIONS; step++) {
            double alpha = (double) step / SWEEP_SUBDIVISIONS;

            for (int i = 0; i < count; i++) {
                Vec3 point = previousEdge.get(i).lerp(currentEdge.get(i), alpha);
                MicroKey key = new MicroKey(
                        microCoordinate(point.x),
                        microCoordinate(point.y),
                        microCoordinate(point.z)
                );
                unique.putIfAbsent(
                        key,
                        new Contact(BlockPos.containing(point), point)
                );
            }
        }

        if (unique.isEmpty()) {
            return SweptResult.EMPTY;
        }

        return new SweptResult(true, List.copyOf(unique.values()), distance);
    }

    private static int microCoordinate(double worldCoordinate) {
        return (int) Math.floor(worldCoordinate * MICRO_RESOLUTION);
    }
}
