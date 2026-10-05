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

    public static final float MIN_BOOM_ANGLE = -35.0F; // Deep trenching / excavation below grade
    public static final float MAX_BOOM_ANGLE = 55.0F;  // High loading dump position
    public static final float BOOM_SPEED = 2.0F;       // Smooth hydraulic travel per tick

    public static final float MIN_BUCKET_ANGLE = -40.0F; // Closed / curled up (transport / retention)
    public static final float MAX_BUCKET_ANGLE = 90.0F;  // Fully opened / dumped down (steep discharge angle)
    public static final float BUCKET_SPEED = 2.8F;       // Fast hydraulic tilt speed

    public static final float DUMP_THRESHOLD_ANGLE = 15.0F; // Tilt angle where material flows out
    public static final int BUCKET_CAPACITY = 1664;         // 3.25 blocks of granular material (over 3 full blocks)
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

        // 1:1 match with LoaderModel 3D kinematics:
        // liftArms.xRot = toRadians(boomAngle)
        // bucket.xRot = toRadians(-bucketAngle)
        float boomRad = (float) Math.toRadians(boomAngle);
        float bucketRad = (float) Math.toRadians(-bucketAngle);

        // 1. Arm pivot in vehicle coordinates (meters above ground contact)
        double armPivotY = 1.8125D;
        double armPivotZ = 0.50D;

        // 2. Bucket pivot relative to arm pivot
        double bRelY = -1.21875D;
        double bRelZ = 2.125D;

        double cosB = Math.cos(boomRad);
        double sinB = Math.sin(boomRad);
        double bucketPivotY = armPivotY + (bRelY * cosB + bRelZ * sinB);
        double bucketPivotZ = armPivotZ + (-bRelY * sinB + bRelZ * cosB);

        // 3. Teeth relative to bucket pivot
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
        if (forwardSpeed > 0.01F && bucketAngle < 20.0F) {
            Set<BlockPos> processed = new HashSet<>();
            for (Vec3 pt : edgePoints) {
                BlockPos pos = BlockPos.containing(pt.x, pt.y, pt.z);
                List<BlockPos> targets = List.of(pos.above(), pos, pos.below(), pos.below(2));

                for (BlockPos target : targets) {
                    if (processed.add(target) && terrain.isDiggable(target)) {
                        GranularCell cell = terrain.getCell(target);
                        double surfaceY = target.getY() + 1.0D;
                        if (cell != null && !cell.isEmpty()) {
                            int localX = (int) Math.floor((pt.x - target.getX()) * GranularCell.RESOLUTION);
                            int localZ = (int) Math.floor((pt.z - target.getZ()) * GranularCell.RESOLUTION);
                            localX = Mth.clamp(localX, 0, GranularCell.RESOLUTION - 1);
                            localZ = Mth.clamp(localZ, 0, GranularCell.RESOLUTION - 1);
                            int colH = cell.getColumnHeight(localX, localZ);
                            if (colH >= 0) {
                                surfaceY = target.getY() + ((colH + 1) / (double) GranularCell.RESOLUTION);
                            } else {
                                surfaceY = target.getY();
                            }
                        }

                        // Strict physical contact gate: teeth must be AT OR BELOW the surface of the material!
                        // If teeth are visually hovering above the ground, it will NEVER scoop!
                        if (pt.y < surfaceY + 0.05D) {
                            int room = BUCKET_CAPACITY - carriedUnits;
                            if (room > 0) {
                                GranularCell targetCell = terrain.getOrConvert(target);
                                int cellMatId = (targetCell != null && !targetCell.isEmpty()) ? targetCell.materialId() : 0;

                                int toRemove = Math.min(room, 64);
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
        }

        // ── 2. Pouring / Dumping (bucket tilted down past threshold) ──
        if (bucketAngle > DUMP_THRESHOLD_ANGLE && carriedUnits > 0 && carriedMaterial != GranularMaterial.EMPTY) {
            float tiltExcess = bucketAngle - DUMP_THRESHOLD_ANGLE;
            int flowRate = Math.min(carriedUnits, 16 + (int) (tiltExcess * 1.5F));
            flowRate = Math.min(flowRate, 96);

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
