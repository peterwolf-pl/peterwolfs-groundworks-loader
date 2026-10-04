package com.piotrek.groundworksloader.bucket;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworksloader.integration.groundworks.IGranularTerrainAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kinematics and terrain interaction controller for the wheel loader's boom and bucket.
 */
public class LoaderBucketController {

    public static final float MIN_BOOM_ANGLE = -10.0F; // Down on ground level
    public static final float MAX_BOOM_ANGLE = 55.0F;  // High loading dump position
    public static final float BOOM_SPEED = 1.6F;       // Degrees per tick

    public static final float MIN_BUCKET_ANGLE = -35.0F; // Closed / curled up (transport / retention)
    public static final float MAX_BUCKET_ANGLE = 60.0F;  // Fully opened / dumped down
    public static final float BUCKET_SPEED = 2.2F;       // Degrees per tick

    public static final float DUMP_THRESHOLD_ANGLE = 15.0F; // Tilt angle where material flows out
    public static final int BUCKET_CAPACITY = 768;          // 1.5 blocks of granular material
    public static final float BUCKET_WIDTH_METERS = 2.8F;

    private float boomAngle = 0.0F;
    private float bucketAngle = 0.0F;
    private int carriedUnits = 0;
    private GranularMaterial carriedMaterial = GranularMaterial.EMPTY;

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

    public void updateAngles(float boomInput, float bucketInput) {
        // Boom Lift (Arrow Up / Down)
        if (boomInput > 0.05F) {
            boomAngle = Math.min(MAX_BOOM_ANGLE, boomAngle + (BOOM_SPEED * boomInput));
        } else if (boomInput < -0.05F) {
            boomAngle = Math.max(MIN_BOOM_ANGLE, boomAngle + (BOOM_SPEED * boomInput));
        }

        // Bucket Tilt (Arrow Left = curl / -1.0, Arrow Right = dump / +1.0)
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
        float yawRad = (float) Math.toRadians(vehicleYaw);
        float pitchRad = (float) Math.toRadians(vehiclePitch);

        // Boom arm rest geometry (-26 degrees puts bucket level on the ground at boomAngle = 0)
        float armAngleRad = (float) Math.toRadians(boomAngle - 26.0F);
        float armLength = 2.25F;
        float armPivotY = 1.25F;
        float armPivotZ = 0.50F;

        // Bucket pivot relative to vehicle base
        float bPivotY = armPivotY + ((float) Math.sin(armAngleRad) * armLength);
        float bPivotZ = armPivotZ + ((float) Math.cos(armAngleRad) * armLength);

        // Bucket lip relative to bucket pivot
        float totalTiltRad = (float) Math.toRadians(bucketAngle);
        float bucketLength = 0.95F;
        float lipRelY = bPivotY - ((float) Math.sin(totalTiltRad) * bucketLength * 0.4F);
        float lipRelZ = bPivotZ + ((float) Math.cos(totalTiltRad) * bucketLength);

        List<Vec3> points = new ArrayList<>(5);
        float halfW = BUCKET_WIDTH_METERS * 0.5F;
        float[] xOffsets = new float[]{-halfW, -halfW * 0.5F, 0.0F, halfW * 0.5F, halfW};

        for (float x : xOffsets) {
            // Rotate around vehicle axes
            // Pitch (around X)
            double cosP = Math.cos(pitchRad);
            double sinP = Math.sin(pitchRad);
            double py = (lipRelY * cosP) - (lipRelZ * sinP);
            double pz = (lipRelY * sinP) + (lipRelZ * cosP);

            // Yaw (around Y)
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
        Vec3 lipCenter = edgePoints.get(2); // Center point

        int totalExcavated = 0;
        int totalDeposited = 0;
        Set<BlockPos> affected = new HashSet<>();
        boolean isScooping = false;
        boolean isDumping = false;

        // ── 1. Scooping / Digging (driving forward into ground with lowered bucket) ──
        if (forwardSpeed > 0.01F && boomAngle < 10.0F && bucketAngle < 20.0F) {
            Set<BlockPos> processed = new HashSet<>();
            for (Vec3 pt : edgePoints) {
                BlockPos pos = BlockPos.containing(pt.x, pt.y, pt.z);
                BlockPos below = pos.below();

                for (BlockPos target : List.of(pos, below)) {
                    if (processed.add(target) && terrain.isDiggable(target)) {
                        int room = BUCKET_CAPACITY - carriedUnits;
                        if (room > 0) {
                            GranularCell cell = terrain.getOrConvert(target);
                            int cellMatId = (cell != null && !cell.isEmpty()) ? cell.materialId() : 0;

                            int toRemove = Math.min(room, 48);
                            int removed = terrain.excavateMicrovoxelsAbove(target, pt.y, toRemove);
                            if (removed > 0) {
                                totalExcavated += removed;
                                carriedUnits += removed;
                                affected.add(target);
                                isScooping = true;

                                if (carriedMaterial == GranularMaterial.EMPTY && cellMatId != 0) {
                                    carriedMaterial = GranularMaterialRegistry.byId(cellMatId);
                                }
                                if (carriedMaterial == GranularMaterial.EMPTY) {
                                    carriedMaterial = GranularMaterialRegistry.DIRT;
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 2. Pouring / Dumping (bucket tilted down past threshold) ──
        if (bucketAngle > DUMP_THRESHOLD_ANGLE && carriedUnits > 0 && carriedMaterial != GranularMaterial.EMPTY) {
            float tiltExcess = bucketAngle - DUMP_THRESHOLD_ANGLE;
            int flowRate = Math.min(carriedUnits, 8 + (int) (tiltExcess * 0.75F));
            flowRate = Math.min(flowRate, 36);

            BlockPos dumpTarget = BlockPos.containing(lipCenter.x, lipCenter.y - 0.25D, lipCenter.z);
            int deposited = terrain.deposit(dumpTarget, carriedMaterial, flowRate);
            if (deposited > 0) {
                totalDeposited += deposited;
                carriedUnits -= deposited;
                affected.add(dumpTarget);
                terrain.markSimulate(dumpTarget);
                isDumping = true;
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
        this.carriedMaterial = carriedMaterial != null ? carriedMaterial : GranularMaterial.EMPTY;
    }
}
