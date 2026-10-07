package com.piotrek.groundworksloader.bucket;

import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworksloader.integration.groundworks.IGranularTerrainAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kinematics and terrain interaction controller for the wheel loader's boom and bucket.
 */
public class LoaderBucketController {

    public static final float MIN_BOOM_ANGLE = -35.0F;
    public static final float MAX_BOOM_ANGLE = 55.0F;
    public static final float BOOM_SPEED = 2.0F;

    public static final float MIN_BUCKET_ANGLE = -40.0F;
    public static final float MAX_BUCKET_ANGLE = 90.0F;
    public static final float BUCKET_SPEED = 2.8F;

    public static final float DUMP_THRESHOLD_ANGLE = 15.0F;
    public static final int BUCKET_CAPACITY = 1664;
    public static final float BUCKET_WIDTH_METERS = 2.8F;

    private static final int MAX_EXCAVATION_PER_TICK = 160;
    private static final int MAX_EXCAVATION_PER_CONTACT = 32;
    private static final double CONTACT_SURFACE_TOLERANCE = 0.10D;
    private static final int DUMP_SURFACE_SEARCH_DEPTH = 14;

    private float boomAngle = 0.0F;
    private float bucketAngle = 0.0F;
    private int carriedUnits = 0;
    private GranularMaterial carriedMaterial = GranularMaterial.EMPTY;
    private List<Vec3> previousCuttingEdgePoints = List.of();

    public record BucketTickResult(
            int unitsExcavated,
            int unitsDeposited,
            int carriedUnitsAfter,
            GranularMaterial carriedMaterialAfter,
            boolean isScooping,
            boolean isDumping,
            Vec3 lipWorldCenter,
            List<BlockPos> affectedPositions
    ) {
        public static final BucketTickResult NONE = new BucketTickResult(
                0, 0, 0, GranularMaterial.EMPTY, false, false, Vec3.ZERO, List.of()
        );
    }

    private record ContactTarget(BlockPos pos, GranularMaterial material) {}

    private record ResolvedSweepContact(
            Vec3 worldPoint,
            ContactTarget target
    ) {}

    public void updateAngles(float boomInput, float bucketInput) {
        if (boomInput > 0.05F) {
            boomAngle = Math.min(MAX_BOOM_ANGLE, boomAngle + (BOOM_SPEED * boomInput));
        } else if (boomInput < -0.05F) {
            boomAngle = Math.max(MIN_BOOM_ANGLE, boomAngle + (BOOM_SPEED * boomInput));
        }

        if (bucketInput > 0.05F) {
            bucketAngle = Math.min(MAX_BUCKET_ANGLE, bucketAngle + (BUCKET_SPEED * bucketInput));
        } else if (bucketInput < -0.05F) {
            bucketAngle = Math.max(MIN_BUCKET_ANGLE, bucketAngle + (BUCKET_SPEED * bucketInput));
        }
    }

    /**
     * Computes the world-space positions along the bucket cutting edge (5 sample points).
     */
    public List<Vec3> getCuttingEdgePoints(Vec3 vehiclePos, float vehicleYaw, float vehiclePitch) {
        return getCuttingEdgePoints(
                vehiclePos,
                vehicleYaw,
                vehiclePitch,
                this.boomAngle,
                this.bucketAngle
        );
    }

    /**
     * Shared world-space cutting-edge transform used by both terrain interaction
     * and the client HUD. Explicit angles allow the HUD to use synchronized entity
     * data instead of a client-only controller copy.
     */
    public static List<Vec3> getCuttingEdgePoints(
            Vec3 vehiclePos,
            float vehicleYaw,
            float vehiclePitch,
            float boomAngle,
            float bucketAngle
    ) {
        float yawRad = (float) Math.toRadians(vehicleYaw);
        float pitchRad = (float) Math.toRadians(vehiclePitch);

        float boomRad = (float) Math.toRadians(boomAngle);
        float bucketRad = (float) Math.toRadians(-bucketAngle);

        double armPivotY = 1.8125D;
        double armPivotZ = 0.50D;

        double bRelY = -1.21875D;
        double bRelZ = 2.125D;

        double cosB = Math.cos(boomRad);
        double sinB = Math.sin(boomRad);
        double bucketPivotY = armPivotY + (bRelY * cosB + bRelZ * sinB);
        double bucketPivotZ = armPivotZ + (-bRelY * sinB + bRelZ * cosB);

        double tRelY = -0.28125D;
        double tRelZ = 1.00D;

        double totalRad = boomRad + bucketRad;
        double cosT = Math.cos(totalRad);
        double sinT = Math.sin(totalRad);

        double lipRelY = bucketPivotY + (tRelY * cosT + tRelZ * sinT);
        double lipRelZ = bucketPivotZ + (-tRelY * sinT + tRelZ * cosT);

        List<Vec3> points = new ArrayList<>(5);
        float halfW = BUCKET_WIDTH_METERS * 0.5F;
        float[] xOffsets = new float[]{-halfW, -halfW * 0.5F, 0.0F, halfW * 0.5F, halfW};

        for (float x : xOffsets) {
            double cosP = Math.cos(pitchRad);
            double sinP = Math.sin(pitchRad);
            double py = (lipRelY * cosP) - (lipRelZ * sinP);
            double pz = (lipRelY * sinP) + (lipRelZ * cosP);

            double cosY = Math.cos(-yawRad);
            double sinY = Math.sin(-yawRad);
            double wx = (x * cosY) + (pz * sinY);
            double wz = (-x * sinY) + (pz * cosY);

            points.add(new Vec3(vehiclePos.x + wx, vehiclePos.y + py, vehiclePos.z + wz));
        }

        return points;
    }

    public BucketTickResult tick(
            IGranularTerrainAccess terrain,
            Vec3 vehiclePos,
            float vehicleYaw,
            float vehiclePitch,
            float forwardSpeed
    ) {
        List<Vec3> edgePoints = getCuttingEdgePoints(vehiclePos, vehicleYaw, vehiclePitch);
        Vec3 lipCenter = edgePoints.get(edgePoints.size() / 2);

        List<Vec3> previousEdge = previousCuttingEdgePoints;
        if (previousEdge.size() != edgePoints.size()) {
            previousEdge = inferPreviousEdgeFromVehicleMotion(
                    edgePoints, vehicleYaw, forwardSpeed);
        }

        LoaderSweptBucketVolume.SweptResult sweep =
                LoaderSweptBucketVolume.compute(previousEdge, edgePoints);

        // Store current geometry for the next tick before mutating terrain.
        previousCuttingEdgePoints = List.copyOf(edgePoints);

        int totalExcavated = 0;
        int totalDeposited = 0;
        Set<BlockPos> affected = new HashSet<>();
        boolean isScooping = false;
        boolean isDumping = false;

        // ── 1. Swept world-space scooping ───────────────────────────────────
        if (forwardSpeed > 0.01F
                && bucketAngle < 20.0F
                && sweep.valid()) {

            // Resolve contacts before excavation so the per-tick intake budget
            // can be distributed across the entire swept edge instead of being
            // consumed by the first few teeth in iteration order.
            List<ResolvedSweepContact> resolvedContacts = new ArrayList<>();
            for (LoaderSweptBucketVolume.Contact contact : sweep.contacts()) {
                ContactTarget target = resolveContact(terrain, contact.worldPoint());
                if (target == null) {
                    continue;
                }
                if (carriedMaterial != GranularMaterial.EMPTY
                        && target.material() != GranularMaterial.EMPTY
                        && target.material().id() != carriedMaterial.id()) {
                    continue;
                }
                resolvedContacts.add(new ResolvedSweepContact(
                        contact.worldPoint(), target));
            }

            for (int i = 0; i < resolvedContacts.size(); i++) {
                if (carriedUnits >= BUCKET_CAPACITY
                        || totalExcavated >= MAX_EXCAVATION_PER_TICK) {
                    break;
                }

                ResolvedSweepContact contact = resolvedContacts.get(i);
                ContactTarget target = contact.target();

                if (carriedMaterial != GranularMaterial.EMPTY
                        && target.material() != GranularMaterial.EMPTY
                        && target.material().id() != carriedMaterial.id()) {
                    continue;
                }

                int room = BUCKET_CAPACITY - carriedUnits;
                int remainingBudget = MAX_EXCAVATION_PER_TICK - totalExcavated;
                int remainingContacts = resolvedContacts.size() - i;

                // Fair-share the remaining budget across all remaining contacts.
                // This makes the cut represent the whole swept bucket volume,
                // rather than only the first left-to-right tooth samples.
                int fairShare = Math.max(
                        1,
                        (remainingBudget + remainingContacts - 1) / remainingContacts
                );
                int requested = Math.min(
                        room,
                        Math.min(
                                MAX_EXCAVATION_PER_CONTACT,
                                Math.min(remainingBudget, fairShare)
                        )
                );
                if (requested <= 0) {
                    break;
                }

                GranularMaterial requiredMaterial =
                        carriedMaterial != GranularMaterial.EMPTY
                                ? carriedMaterial
                                : target.material();

                ExcavationResult result = terrain.excavateAt(
                        contact.worldPoint(),
                        requested,
                        requiredMaterial
                );
                if (!result.success()) {
                    continue;
                }

                if (requiredMaterial != GranularMaterial.EMPTY
                        && result.material().id() != requiredMaterial.id()) {
                    throw new IllegalStateException(
                            "Groundworks material filter violation: required="
                                    + requiredMaterial.name()
                                    + ", removed=" + result.material().name()
                    );
                }

                totalExcavated += result.unitsRemoved();
                carriedUnits += result.unitsRemoved();
                affected.addAll(result.affectedCells());
                isScooping = true;

                if (carriedMaterial == GranularMaterial.EMPTY) {
                    carriedMaterial = result.material();
                }
            }
        }

        // ── 2. Gravity surface search + dumping ─────────────────────────────
        if (bucketAngle > DUMP_THRESHOLD_ANGLE
                && carriedUnits > 0
                && carriedMaterial != GranularMaterial.EMPTY) {

            float tiltExcess = bucketAngle - DUMP_THRESHOLD_ANGLE;
            int flowRate = Math.min(
                    carriedUnits,
                    16 + (int) (tiltExcess * 1.5F)
            );
            flowRate = Math.min(flowRate, 96);

            IGranularTerrainAccess.ContainerTransferResult transfer =
                    terrain.transferToWorldContainer(
                            lipCenter,
                            carriedMaterial,
                            flowRate
                    );

            if (transfer.receiverPresent()) {
                int consumed = Math.min(
                        carriedUnits,
                        Math.max(0, transfer.unitsConsumed())
                );

                if (consumed > 0) {
                    totalDeposited += consumed;
                    carriedUnits -= consumed;
                    isDumping = true;
                }
            } else {
                BlockPos dumpTarget = terrain.findDepositSurface(
                        lipCenter,
                        carriedMaterial,
                        DUMP_SURFACE_SEARCH_DEPTH
                );

                if (dumpTarget != null) {
                    int deposited = terrain.deposit(
                            dumpTarget,
                            carriedMaterial,
                            flowRate
                    );

                    if (deposited > 0) {
                        totalDeposited += deposited;
                        carriedUnits -= deposited;
                        affected.add(dumpTarget);
                        terrain.markSimulate(dumpTarget);
                        isDumping = true;
                    }
                }
            }

            if (carriedUnits <= 0) {
                carriedUnits = 0;
                carriedMaterial = GranularMaterial.EMPTY;
            }
        }

        return new BucketTickResult(
                totalExcavated,
                totalDeposited,
                carriedUnits,
                carriedMaterial,
                isScooping,
                isDumping,
                lipCenter,
                new ArrayList<>(affected)
        );
    }

    private static ContactTarget resolveContact(
            IGranularTerrainAccess terrain,
            Vec3 worldPoint
    ) {
        BlockPos direct = BlockPos.containing(worldPoint);
        BlockPos[] candidates = {
                direct,
                direct.below()
        };

        for (BlockPos candidate : candidates) {
            if (!terrain.isDiggable(candidate)) {
                continue;
            }

            double surfaceY = terrain.getSurfaceWorldY(
                    candidate, worldPoint.x, worldPoint.z);
            if (!Double.isFinite(surfaceY)) {
                continue;
            }

            if (worldPoint.y <= surfaceY + CONTACT_SURFACE_TOLERANCE) {
                return new ContactTarget(
                        candidate,
                        terrain.getMaterial(candidate)
                );
            }
        }

        return null;
    }

    private static List<Vec3> inferPreviousEdgeFromVehicleMotion(
            List<Vec3> currentEdge,
            float vehicleYaw,
            float forwardSpeed
    ) {
        if (Math.abs(forwardSpeed) < LoaderSweptBucketVolume.MIN_SWEEP_DISTANCE) {
            return currentEdge;
        }

        double yawRad = Math.toRadians(vehicleYaw);
        Vec3 vehicleDelta = new Vec3(
                -Math.sin(yawRad) * forwardSpeed,
                0.0D,
                Math.cos(yawRad) * forwardSpeed
        );

        List<Vec3> inferred = new ArrayList<>(currentEdge.size());
        for (Vec3 point : currentEdge) {
            inferred.add(point.subtract(vehicleDelta));
        }
        return inferred;
    }

    public void resetSweepHistory() {
        previousCuttingEdgePoints = List.of();
    }

    public float boomAngle() {
        return boomAngle;
    }

    public void setBoomAngle(float boomAngle) {
        this.boomAngle = boomAngle;
    }

    public float bucketAngle() {
        return bucketAngle;
    }

    public void setBucketAngle(float bucketAngle) {
        this.bucketAngle = bucketAngle;
    }

    public int carriedUnits() {
        return carriedUnits;
    }

    public void setCarriedUnits(int carriedUnits) {
        this.carriedUnits = Math.clamp(carriedUnits, 0, BUCKET_CAPACITY);
    }

    public GranularMaterial carriedMaterial() {
        return carriedMaterial;
    }

    public void setCarriedMaterial(GranularMaterial carriedMaterial) {
        this.carriedMaterial =
                carriedMaterial != null ? carriedMaterial : GranularMaterial.EMPTY;
    }
}
