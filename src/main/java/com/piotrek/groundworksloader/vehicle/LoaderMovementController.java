package com.piotrek.groundworksloader.vehicle;

import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworks.terrain.storage.GranularWorldStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * High-fidelity 4-wheel drive and articulated steering physics controller for the industrial wheel loader.
 *
 * <p>Includes realistic 2-axle terrain pitch/roll suspension sampling so when only the front wheels
 * drive onto an elevation, the front axle rises first (pitching the vehicle nose up), and only when
 * the rear wheels follow does the rear axle climb and level out the machine.
 */
public class LoaderMovementController {

    public static final float MAX_FORWARD_SPEED = 0.26F;
    public static final float MAX_REVERSE_SPEED = -0.16F;
    public static final float ACCELERATION = 0.022F;
    public static final float BRAKING = 0.038F;
    public static final float FRICTION = 0.014F;

    public static final float MAX_STEER_ANGLE = 38.0F;
    public static final float STEER_SPEED = 4.2F;
    public static final float STEER_RECENTER = 3.2F;

    public static final float WHEEL_RADIUS_METERS = 0.6875F; // 11 model units
    public static final double WHEELBASE = 2.0D;              // Distance between front & rear axles
    public static final double TRACK_GAUGE = 2.125D;          // Width between left & right wheels

    private float forwardSpeed = 0.0F;
    private float steerAngle = 0.0F;
    private float wheelRotation = 0.0F;
    private float vehiclePitch = 0.0F;
    private float vehicleRoll = 0.0F;

    public record StepResult(
            float forwardSpeed,
            float steerAngle,
            float wheelRotation,
            float deltaYaw
    ) {}

    public StepResult step(float throttle, float steer) {
        // 1. Throttle / Speed Dynamics
        if (throttle > 0.05F) {
            if (forwardSpeed < 0.0F) {
                forwardSpeed = Math.min(0.0F, forwardSpeed + BRAKING);
            } else {
                forwardSpeed = Math.min(MAX_FORWARD_SPEED, forwardSpeed + (ACCELERATION * throttle));
            }
        } else if (throttle < -0.05F) {
            if (forwardSpeed > 0.0F) {
                forwardSpeed = Math.max(0.0F, forwardSpeed - BRAKING);
            } else {
                forwardSpeed = Math.max(MAX_REVERSE_SPEED, forwardSpeed + (ACCELERATION * throttle));
            }
        } else {
            // Natural rolling resistance to a stop
            if (forwardSpeed > 0.0F) {
                forwardSpeed = Math.max(0.0F, forwardSpeed - FRICTION);
            } else if (forwardSpeed < 0.0F) {
                forwardSpeed = Math.min(0.0F, forwardSpeed + FRICTION);
            }
        }

        // 2. Articulated Steering Dynamics
        if (steer > 0.05F) {
            steerAngle = Math.min(MAX_STEER_ANGLE, steerAngle + (STEER_SPEED * steer));
        } else if (steer < -0.05F) {
            steerAngle = Math.max(-MAX_STEER_ANGLE, steerAngle + (STEER_SPEED * steer));
        } else {
            // Self-centering articulated joint
            if (steerAngle > 0.0F) {
                steerAngle = Math.max(0.0F, steerAngle - STEER_RECENTER);
            } else if (steerAngle < 0.0F) {
                steerAngle = Math.min(0.0F, steerAngle + STEER_RECENTER);
            }
        }

        // 3. Angular Yaw Rate from Articulation
        float normalizedSteer = steerAngle / MAX_STEER_ANGLE;
        float speedFactor = Math.abs(forwardSpeed) / MAX_FORWARD_SPEED;
        float directionSign = forwardSpeed >= 0.0F ? 1.0F : -1.0F;
        float deltaYaw = normalizedSteer * directionSign * (0.8F + (speedFactor * 3.6F));

        // 4. Wheel Rolling Rotation Animation (degrees)
        float circumference = 2.0F * (float) Math.PI * WHEEL_RADIUS_METERS;
        float rotDelta = (forwardSpeed / circumference) * 360.0F;
        wheelRotation = Mth.wrapDegrees(wheelRotation + rotDelta);

        return new StepResult(forwardSpeed, steerAngle, wheelRotation, deltaYaw);
    }

    public float forwardSpeed() {
        return forwardSpeed;
    }

    public void setForwardSpeed(float forwardSpeed) {
        this.forwardSpeed = forwardSpeed;
    }

    public float steerAngle() {
        return steerAngle;
    }

    public void setSteerAngle(float steerAngle) {
        this.steerAngle = steerAngle;
    }

    public float wheelRotation() {
        return wheelRotation;
    }

    public void setWheelRotation(float wheelRotation) {
        this.wheelRotation = wheelRotation;
    }

    public float vehiclePitch() {
        return vehiclePitch;
    }

    public float vehicleRoll() {
        return vehicleRoll;
    }

    public void setOrientation(float pitch, float roll) {
        this.vehiclePitch = pitch;
        this.vehicleRoll = roll;
    }

    /**
     * Samples terrain height underneath each of the 4 wheels and computes realistic 2-axle pitch and roll.
     */
    public void updateTerrainOrientation(Level level, Vec3 pos, float yaw) {
        double yawRad = Math.toRadians(yaw);
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rgtX = Math.cos(yawRad);
        double rgtZ = Math.sin(yawRad);

        double halfL = WHEELBASE * 0.5D;
        double halfW = TRACK_GAUGE * 0.5D;

        double flX = pos.x + (fwdX * halfL) - (rgtX * halfW);
        double flZ = pos.z + (fwdZ * halfL) - (rgtZ * halfW);

        double frX = pos.x + (fwdX * halfL) + (rgtX * halfW);
        double frZ = pos.z + (fwdZ * halfL) + (rgtZ * halfW);

        double rlX = pos.x - (fwdX * halfL) - (rgtX * halfW);
        double rlZ = pos.z - (fwdZ * halfL) - (rgtZ * halfW);

        double rrX = pos.x - (fwdX * halfL) + (rgtX * halfW);
        double rrZ = pos.z - (fwdZ * halfL) + (rgtZ * halfW);

        double flY = sampleGroundHeight(level, flX, pos.y, flZ);
        double frY = sampleGroundHeight(level, frX, pos.y, frZ);
        double rlY = sampleGroundHeight(level, rlX, pos.y, rlZ);
        double rrY = sampleGroundHeight(level, rrX, pos.y, rrZ);

        double frontAxleY = (flY + frY) * 0.5D;
        double rearAxleY = (rlY + rrY) * 0.5D;
        double leftSideY = (flY + rlY) * 0.5D;
        double rightSideY = (frY + rrY) * 0.5D;

        // Front axle higher than rear axle => pitch nose UP (negative angle in renderer)
        double targetPitch = Math.toDegrees(Math.atan2(rearAxleY - frontAxleY, WHEELBASE));
        double targetRoll = Math.toDegrees(Math.atan2(leftSideY - rightSideY, TRACK_GAUGE));

        targetPitch = Mth.clamp(targetPitch, -32.0D, 32.0D);
        targetRoll = Mth.clamp(targetRoll, -20.0D, 20.0D);

        // Smooth suspension damping
        this.vehiclePitch = (float) Mth.lerp(0.20D, this.vehiclePitch, targetPitch);
        this.vehicleRoll = (float) Mth.lerp(0.20D, this.vehicleRoll, targetRoll);
    }

    public double getAverageGroundY(Level level, Vec3 pos, float yaw) {
        double yawRad = Math.toRadians(yaw);
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rgtX = Math.cos(yawRad);
        double rgtZ = Math.sin(yawRad);

        double halfL = WHEELBASE * 0.5D;
        double halfW = TRACK_GAUGE * 0.5D;

        double flY = sampleGroundHeight(level, pos.x + (fwdX * halfL) - (rgtX * halfW), pos.y, pos.z + (fwdZ * halfL) - (rgtZ * halfW));
        double frY = sampleGroundHeight(level, pos.x + (fwdX * halfL) + (rgtX * halfW), pos.y, pos.z + (fwdZ * halfL) + (rgtZ * halfW));
        double rlY = sampleGroundHeight(level, pos.x - (fwdX * halfL) - (rgtX * halfW), pos.y, pos.z - (fwdZ * halfL) - (rgtZ * halfW));
        double rrY = sampleGroundHeight(level, pos.x - (fwdX * halfL) + (rgtX * halfW), pos.y, pos.z - (fwdZ * halfL) + (rgtZ * halfW));

        return (flY + frY + rlY + rrY) * 0.25D;
    }

    public static double sampleGroundHeight(Level level, double x, double vehicleY, double z) {
        BlockPos bp = BlockPos.containing(x, vehicleY + 0.8D, z);

        GranularWorldStorage storage = null;
        if (level instanceof ServerLevel sl) {
            storage = GranularWorldStorage.get(sl);
        }

        for (int dy = 0; dy <= 4; dy++) {
            BlockPos check = bp.below(dy);
            if (storage != null) {
                GranularCell cell = storage.getCell(check);
                if (cell != null && !cell.isEmpty()) {
                    int localX = (int) Math.floor((x - check.getX()) * GranularCell.RESOLUTION);
                    int localZ = (int) Math.floor((z - check.getZ()) * GranularCell.RESOLUTION);
                    localX = Mth.clamp(localX, 0, GranularCell.RESOLUTION - 1);
                    localZ = Mth.clamp(localZ, 0, GranularCell.RESOLUTION - 1);
                    int colH = cell.getColumnHeight(localX, localZ);
                    if (colH >= 0) {
                        return check.getY() + ((colH + 1) / (double) GranularCell.RESOLUTION);
                    }
                }
            }
            BlockState state = level.getBlockState(check);
            if (!state.isAir()) {
                VoxelShape shape = state.getCollisionShape(level, check);
                if (!shape.isEmpty()) {
                    return check.getY() + shape.max(Direction.Axis.Y);
                } else if (state.isSolid()) {
                    return check.getY() + 1.0D;
                }
            }
        }
        return vehicleY;
    }
}
