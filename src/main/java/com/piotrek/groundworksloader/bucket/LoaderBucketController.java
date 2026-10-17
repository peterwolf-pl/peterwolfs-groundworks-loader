package com.piotrek.groundworksloader.bucket;

import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularComposition;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
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
    public static final int BUCKET_CAPACITY = 2048; // 4.0 m³ full heavy loader bucket
    public static final float BUCKET_WIDTH_METERS = 2.8F;
    private static final float HORIZONTAL_SPILL_TOLERANCE = 1.0F;

    public static final int MAX_EXCAVATION_PER_TICK = 384;
    public static final int MAX_EXCAVATION_PER_CONTACT = 96;
    private static final double CONTACT_SURFACE_TOLERANCE = 0.18D;
    private static final int DUMP_SURFACE_SEARCH_DEPTH = 14;

    private final int bucketCapacity;
    private final float bucketWidthMeters;
    private final float geometryScale;
    private float boomAngle = 0.0F;
    private float bucketAngle = 0.0F;
    private float previousBoomAngle = 0.0F;
    private float previousBucketAngle = 0.0F;
    private boolean boomLiftHeld;
    private boolean liftSpillGestureActive;
    private int pendingLiftSpillUnits;
    private final GranularComposition composition = new GranularComposition();
    private List<Vec3> previousCuttingEdgePoints = List.of();

    public LoaderBucketController() {
        this(BUCKET_CAPACITY, BUCKET_WIDTH_METERS, 1.0F);
    }

    public LoaderBucketController(int bucketCapacity, float bucketWidthMeters, float geometryScale) {
        if (bucketCapacity <= 0 || bucketWidthMeters <= 0.0F || geometryScale <= 0.0F) {
            throw new IllegalArgumentException("Loader bucket dimensions and capacity must be positive");
        }
        this.bucketCapacity = bucketCapacity;
        this.bucketWidthMeters = bucketWidthMeters;
        this.geometryScale = geometryScale;
    }

    public record BucketTickResult(
            int unitsExcavated,
            int unitsDeposited,
            int carriedUnitsAfter,
            GranularMaterial carriedMaterialAfter,
            boolean isScooping,
            boolean isDumping,
            Vec3 lipWorldCenter,
            List<BlockPos> affectedPositions,
            GranularComposition compositionAfter
    ) {
        public BucketTickResult(
                int unitsExcavated,
                int unitsDeposited,
                int carriedUnitsAfter,
                GranularMaterial carriedMaterialAfter,
                boolean isScooping,
                boolean isDumping,
                Vec3 lipWorldCenter,
                List<BlockPos> affectedPositions
        ) {
            this(unitsExcavated, unitsDeposited, carriedUnitsAfter, carriedMaterialAfter,
                    isScooping, isDumping, lipWorldCenter, affectedPositions,
                    GranularComposition.pure(carriedMaterialAfter, carriedUnitsAfter));
        }

        public static final BucketTickResult NONE = new BucketTickResult(
                0, 0, 0, GranularMaterial.EMPTY, false, false, Vec3.ZERO, List.of(),
                new GranularComposition()
        );
    }

    private record ContactTarget(BlockPos pos, GranularMaterial material) {}

    private record ResolvedSweepContact(
            Vec3 worldPoint,
            ContactTarget target
    ) {}

    public void updateAngles(float boomInput, float bucketInput) {
        boolean lifting = boomInput > 0.05F;
        if (lifting && boomAngle < MAX_BOOM_ANGLE && !boomLiftHeld && !composition.isEmpty()
                && Math.abs(bucketAngle - boomAngle) <= HORIZONTAL_SPILL_TOLERANCE) {
            pendingLiftSpillUnits = composition.totalUnits() / 3;
            liftSpillGestureActive = pendingLiftSpillUnits > 0;
        } else if (!lifting) {
            liftSpillGestureActive = false;
            pendingLiftSpillUnits = 0;
        }
        boomLiftHeld = lifting;

        float oldBoomAngle = boomAngle;
        if (lifting) {
            boomAngle = Math.min(MAX_BOOM_ANGLE, boomAngle + (BOOM_SPEED * boomInput));
        } else if (boomInput < -0.05F) {
            boomAngle = Math.max(MIN_BOOM_ANGLE, boomAngle + (BOOM_SPEED * boomInput));
        }

        // Z-bar self-leveling: preserve world bucket pitch while the boom moves.
        bucketAngle = Math.clamp(bucketAngle + (boomAngle - oldBoomAngle), MIN_BUCKET_ANGLE, MAX_BUCKET_ANGLE);

        // Manual curl/dump adjusts the retained offset from level. Left/curl is negative.
        if (bucketInput > 0.05F) {
            bucketAngle = Math.min(MAX_BUCKET_ANGLE, bucketAngle + (BUCKET_SPEED * bucketInput));
        } else if (bucketInput < -0.05F) {
            bucketAngle = Math.max(MIN_BUCKET_ANGLE, bucketAngle + (BUCKET_SPEED * bucketInput));
        }
    }

    /**
     * Computes the world-space positions along the bucket cutting edge for a given sample count.
     */
    public static List<Vec3> computeLipSamplePoints(
            Vec3 vehiclePos,
            float vehicleYaw,
            float vehiclePitch,
            float boomAngle,
            float bucketAngle,
            int sampleCount
    ) {
        return computeLipSamplePoints(
                vehiclePos, vehicleYaw, vehiclePitch, boomAngle, bucketAngle,
                sampleCount, BUCKET_WIDTH_METERS, 1.0F
        );
    }

    public static List<Vec3> computeLipSamplePoints(
            Vec3 vehiclePos,
            float vehicleYaw,
            float vehiclePitch,
            float boomAngle,
            float bucketAngle,
            int sampleCount,
            float bucketWidthMeters,
            float geometryScale
    ) {
        float yawRad = (float) Math.toRadians(vehicleYaw);
        float pitchRad = (float) Math.toRadians(vehiclePitch);

        float boomRad = (float) Math.toRadians(boomAngle);
        float bucketRad = (float) Math.toRadians(-bucketAngle);

        double armPivotY = 1.8125D * geometryScale;
        double armPivotZ = 0.50D * geometryScale;

        double bRelY = -1.21875D * geometryScale;
        double bRelZ = 2.125D * geometryScale;

        double cosB = Math.cos(boomRad);
        double sinB = Math.sin(boomRad);
        double bucketPivotY = armPivotY + (bRelY * cosB + bRelZ * sinB);
        double bucketPivotZ = armPivotZ + (-bRelY * sinB + bRelZ * cosB);

        double tRelY = -0.28125D * geometryScale;
        double tRelZ = 1.00D * geometryScale;

        double totalRad = boomRad + bucketRad;
        double cosT = Math.cos(totalRad);
        double sinT = Math.sin(totalRad);

        double lipRelY = bucketPivotY + (tRelY * cosT + tRelZ * sinT);
        double lipRelZ = bucketPivotZ + (-tRelY * sinT + tRelZ * cosT);

        int count = Math.max(2, sampleCount);
        List<Vec3> points = new ArrayList<>(count);
        float halfW = bucketWidthMeters * 0.5F;

        for (int i = 0; i < count; i++) {
            float frac = (float) i / (count - 1);
            float x = -halfW + frac * (2.0F * halfW);

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

    /**
     * Computes the world-space positions along the bucket cutting edge (5 sample points).
     */
    public static List<Vec3> computeCuttingEdgePoints(
            Vec3 vehiclePos,
            float vehicleYaw,
            float vehiclePitch,
            float boomAngle,
            float bucketAngle
    ) {
        return computeLipSamplePoints(vehiclePos, vehicleYaw, vehiclePitch, boomAngle, bucketAngle, 5);
    }

    /**
     * Computes the world-space positions along the bucket cutting edge (5 sample points) using current controller angles.
     */
    public List<Vec3> getCuttingEdgePoints(Vec3 vehiclePos, float vehicleYaw, float vehiclePitch) {
        return computeLipSamplePoints(
                vehiclePos, vehicleYaw, vehiclePitch, this.boomAngle, this.bucketAngle,
                5, this.bucketWidthMeters, this.geometryScale
        );
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

        float bucketTilt = this.bucketAngle - this.boomAngle;
        float previousBucketTilt = this.previousBucketAngle - this.previousBoomAngle;
        boolean isDrivingForward = forwardSpeed > 0.005F;
        boolean isCurlingIn = bucketTilt < previousBucketTilt && bucketTilt < 35.0F;
        boolean isBoomLifting = this.boomAngle > this.previousBoomAngle && this.boomAngle < 25.0F;
        boolean canScoop = !liftSpillGestureActive
                && (isDrivingForward || isCurlingIn || isBoomLifting)
                && bucketTilt <= 25.0F
                && sweep.valid();

        // ── 1. Swept world-space scooping (Supports mixed materials) ───────
        if (canScoop) {
            List<ResolvedSweepContact> resolvedContacts = new ArrayList<>();
            for (LoaderSweptBucketVolume.Contact contact : sweep.contacts()) {
                ContactTarget target = resolveContact(terrain, contact.worldPoint());
                if (target == null) {
                    continue;
                }
                resolvedContacts.add(new ResolvedSweepContact(
                        contact.worldPoint(), target));
            }

            for (int i = 0; i < resolvedContacts.size(); i++) {
                if (composition.totalUnits() >= bucketCapacity
                        || totalExcavated >= MAX_EXCAVATION_PER_TICK) {
                    break;
                }

                ResolvedSweepContact contact = resolvedContacts.get(i);
                ContactTarget target = contact.target();

                int room = bucketCapacity - composition.totalUnits();
                int remainingBudget = MAX_EXCAVATION_PER_TICK - totalExcavated;
                int remainingContacts = resolvedContacts.size() - i;

                int fairShare = Math.max(
                        32,
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

                GranularMaterial targetMat = target.material();
                ExcavationResult result = terrain.excavateAt(
                        contact.worldPoint(),
                        requested,
                        targetMat
                );
                if (!result.success() || result.unitsRemoved() <= 0) {
                    continue;
                }

                totalExcavated += result.unitsRemoved();
                composition.add(result.material(), result.unitsRemoved());
                affected.addAll(result.affectedCells());
                isScooping = true;
            }
        }

        // ── 2. An uncurled load loses one third once when a lift gesture starts ──
        if (pendingLiftSpillUnits > 0 && !composition.isEmpty()) {
            int spilled = depositAtLip(
                    terrain, vehiclePos, vehicleYaw, vehiclePitch, lipCenter,
                    Math.min(pendingLiftSpillUnits, composition.totalUnits()), affected, false
            );
            totalDeposited += spilled;
            isDumping |= spilled > 0;
            pendingLiftSpillUnits = Math.max(0, pendingLiftSpillUnits - spilled);
        }

        // ── 3. Gravity surface search + dumping across bucket width ─────────
        float dumpAngle = bucketAngle - boomAngle;
        if (dumpAngle > DUMP_THRESHOLD_ANGLE && !composition.isEmpty()) {
            float tiltExcess = dumpAngle - DUMP_THRESHOLD_ANGLE;
            int baseFlow = 24 + (int) (tiltExcess * 2.5F);
            int flowRate = Math.min(composition.totalUnits(), Math.min(192, baseFlow));
            int deposited = depositAtLip(
                    terrain, vehiclePos, vehicleYaw, vehiclePitch, lipCenter,
                    flowRate, affected, true
            );
            totalDeposited += deposited;
            isDumping |= deposited > 0;
        }

        this.previousBoomAngle = this.boomAngle;
        this.previousBucketAngle = this.bucketAngle;

        return new BucketTickResult(
                totalExcavated,
                totalDeposited,
                composition.totalUnits(),
                carriedMaterial(),
                isScooping,
                isDumping,
                lipCenter,
                new ArrayList<>(affected),
                composition.copy()
        );
    }

    private int depositAtLip(
            IGranularTerrainAccess terrain,
            Vec3 vehiclePos,
            float vehicleYaw,
            float vehiclePitch,
            Vec3 lipCenter,
            int requestedUnits,
            Set<BlockPos> affected,
            boolean allowContainerTransfer
    ) {
        if (requestedUnits <= 0 || composition.isEmpty()) {
            return 0;
        }

        if (allowContainerTransfer) {
            IGranularTerrainAccess.ContainerTransferResult transfer =
                    terrain.transferToWorldContainer(lipCenter, composition, requestedUnits);
            if (transfer.receiverPresent()) {
                int consumed = Math.min(
                        Math.min(requestedUnits, composition.totalUnits()),
                        Math.max(0, transfer.unitsConsumed())
                );
                return composition.extractProportional(consumed).totalUnits();
            }
        }

        List<Vec3> lipPoints = computeLipSamplePoints(
                vehiclePos, vehicleYaw, vehiclePitch,
                this.boomAngle, this.bucketAngle, 7,
                this.bucketWidthMeters, this.geometryScale
        );
        GranularMaterial dominantMat = carriedMaterial();
        List<BlockPos> dumpColumns = new ArrayList<>();
        Set<Long> visitedColumns = new HashSet<>();
        for (Vec3 point : lipPoints) {
            BlockPos surfacePos = terrain.findDepositSurface(
                    point, dominantMat, DUMP_SURFACE_SEARCH_DEPTH);
            if (surfacePos != null) {
                long columnKey = BlockPos.asLong(surfacePos.getX(), 0, surfacePos.getZ());
                if (visitedColumns.add(columnKey)) {
                    dumpColumns.add(surfacePos);
                }
            }
        }
        if (dumpColumns.isEmpty()) {
            return 0;
        }

        GranularComposition outgoing = composition.extractProportional(requestedUnits);
        int totalDeposited = 0;
        int[] outgoingCounts = outgoing.toArray();
        for (int materialId = 1; materialId < outgoingCounts.length; materialId++) {
            int unitsForMaterial = outgoingCounts[materialId];
            if (unitsForMaterial <= 0) continue;
            GranularMaterial material = GranularMaterialRegistry.byId(materialId);
            if (material == null || material.id() == 0) {
                composition.add(materialId, unitsForMaterial);
                continue;
            }

            for (int columnIndex = 0; columnIndex < dumpColumns.size(); columnIndex++) {
                int quota = unitsForMaterial / dumpColumns.size()
                        + (columnIndex < unitsForMaterial % dumpColumns.size() ? 1 : 0);
                if (quota <= 0) continue;
                BlockPos target = dumpColumns.get(columnIndex);
                int deposited = terrain.deposit(target, material, quota);
                totalDeposited += deposited;
                if (deposited > 0) {
                    affected.add(target);
                    terrain.markSimulate(target);
                }
                if (deposited < quota) {
                    composition.add(materialId, quota - deposited);
                }
            }
        }
        return totalDeposited;
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

    public int capacity() {
        return bucketCapacity;
    }

    public float bucketWidthMeters() {
        return bucketWidthMeters;
    }

    public float geometryScale() {
        return geometryScale;
    }

    public int carriedUnits() {
        return composition.totalUnits();
    }

    public void setCarriedUnits(int carriedUnits) {
        int clamped = Math.clamp(carriedUnits, 0, bucketCapacity);
        if (clamped <= 0) {
            composition.clear();
        } else if (composition.isEmpty()) {
            composition.add(GranularMaterialRegistry.DIRT, clamped);
        } else {
            int current = composition.totalUnits();
            if (clamped < current) {
                composition.extractProportional(current - clamped);
            } else if (clamped > current) {
                int dominant = composition.dominantMaterialId();
                composition.add(dominant > 0 ? dominant : GranularMaterialRegistry.DIRT.id(), clamped - current);
            }
        }
    }

    public GranularMaterial carriedMaterial() {
        int dominant = composition.dominantMaterialId();
        return dominant > 0 ? GranularMaterialRegistry.byId(dominant) : GranularMaterial.EMPTY;
    }

    public void setCarriedMaterial(GranularMaterial carriedMaterial) {
        if (carriedMaterial == null || carriedMaterial == GranularMaterial.EMPTY) {
            composition.clear();
            return;
        }
        int units = composition.totalUnits();
        if (units > 0) {
            composition.clear();
            composition.add(carriedMaterial, units);
        }
    }

    public GranularComposition composition() {
        return composition;
    }

    public void setComposition(GranularComposition other) {
        composition.replaceWith(other != null ? other.toArray() : new int[0]);
        if (composition.totalUnits() > bucketCapacity) {
            composition.extractProportional(composition.totalUnits() - bucketCapacity);
        }
    }

    public void setMaterialUnits(int[] units) {
        composition.replaceWith(units);
        if (composition.totalUnits() > bucketCapacity) {
            composition.extractProportional(composition.totalUnits() - bucketCapacity);
        }
    }

    public int acceptMaterial(GranularMaterial material, int units) {
        if (material == null || material.id() == 0 || units <= 0) return 0;
        int room = bucketCapacity - composition.totalUnits();
        int toAdd = Math.min(units, Math.max(0, room));
        return composition.add(material, toAdd);
    }

    public int acceptComposition(GranularComposition other) {
        if (other == null || other.isEmpty()) return 0;
        int room = bucketCapacity - composition.totalUnits();
        if (room <= 0) return 0;
        GranularComposition copy = other.copy();
        if (copy.totalUnits() > room) {
            copy = copy.extractProportional(room);
        }
        return composition.addAll(copy);
    }

    public int extractMaterial(int maxUnits) {
        if (maxUnits <= 0 || composition.isEmpty()) return 0;
        int dom = composition.dominantMaterialId();
        return composition.remove(dom, maxUnits);
    }

    public GranularComposition extractComposition(int maxUnits) {
        return composition.extractProportional(maxUnits);
    }
}
