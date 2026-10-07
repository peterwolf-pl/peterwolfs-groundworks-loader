package com.piotrek.groundworksloader.vehicle;

import com.piotrek.groundworks.api.GroundworksApi;
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
    public static final double FRONT_AXLE_OFFSET = 1.0D;     // Front axle 1.0m ahead of center (Z = +16 units)
    public static final double REAR_AXLE_OFFSET = 2.0D;      // Rear axle 2.0m behind center (Z = -32 units, shifted back 1 block)
    public static final double WHEELBASE = 3.0D;             // Extended wheelbase: 3.0 meters total
    public static final double TRACK_GAUGE = 2.125D;         // Width between left & right wheels

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
        //
        // A real articulated loader can bend its center joint while stationary, but
        // that hydraulic articulation must not rotate the entire vehicle in place.
        // While stopped and the steering key is released, keep the selected bend.
        // Once the machine starts moving with neutral steering, let the joint return
        // toward center. Pressing the opposite direction always moves the joint back.
        boolean moving = Math.abs(forwardSpeed) > 0.01F;
        if (steer > 0.05F) {
            steerAngle = Math.min(MAX_STEER_ANGLE, steerAngle + (STEER_SPEED * steer));
        } else if (steer < -0.05F) {
            steerAngle = Math.max(-MAX_STEER_ANGLE, steerAngle + (STEER_SPEED * steer));
        } else if (moving) {
            if (steerAngle > 0.0F) {
                steerAngle = Math.max(0.0F, steerAngle - STEER_RECENTER);
            } else if (steerAngle < 0.0F) {
                steerAngle = Math.min(0.0F, steerAngle + STEER_RECENTER);
            }
        }

        // 3. Angular Yaw Rate from Articulation
        // No translation means no heading change. Yaw scales continuously from zero
        // with wheel speed so there is no artificial pivot-turn at a standstill.
        float normalizedSteer = steerAngle / MAX_STEER_ANGLE;
        float speedAbs = Math.abs(forwardSpeed);
        float speedFactor = Mth.clamp(speedAbs / MAX_FORWARD_SPEED, 0.0F, 1.0F);
        float directionSign = Math.signum(forwardSpeed);
        float deltaYaw = speedAbs <= 0.01F
                ? 0.0F
                : normalizedSteer * directionSign * speedFactor * 4.4F;

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

        double halfW = TRACK_GAUGE * 0.5D;

        double flX = pos.x + (fwdX * FRONT_AXLE_OFFSET) - (rgtX * halfW);
        double flZ = pos.z + (fwdZ * FRONT_AXLE_OFFSET) - (rgtZ * halfW);

        double frX = pos.x + (fwdX * FRONT_AXLE_OFFSET) + (rgtX * halfW);
        double frZ = pos.z + (fwdZ * FRONT_AXLE_OFFSET) + (rgtZ * halfW);

        double rlX = pos.x - (fwdX * REAR_AXLE_OFFSET) - (rgtX * halfW);
        double rlZ = pos.z - (fwdZ * REAR_AXLE_OFFSET) - (rgtZ * halfW);

        double rrX = pos.x - (fwdX * REAR_AXLE_OFFSET) + (rgtX * halfW);
        double rrZ = pos.z - (fwdZ * REAR_AXLE_OFFSET) + (rgtZ * halfW);

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

        double halfW = TRACK_GAUGE * 0.5D;

        double flY = sampleGroundHeight(level, pos.x + (fwdX * FRONT_AXLE_OFFSET) - (rgtX * halfW), pos.y, pos.z + (fwdZ * FRONT_AXLE_OFFSET) - (rgtZ * halfW));
        double frY = sampleGroundHeight(level, pos.x + (fwdX * FRONT_AXLE_OFFSET) + (rgtX * halfW), pos.y, pos.z + (fwdZ * FRONT_AXLE_OFFSET) + (rgtZ * halfW));
        double rlY = sampleGroundHeight(level, pos.x - (fwdX * REAR_AXLE_OFFSET) - (rgtX * halfW), pos.y, pos.z - (fwdZ * REAR_AXLE_OFFSET) - (rgtZ * halfW));
        double rrY = sampleGroundHeight(level, pos.x - (fwdX * REAR_AXLE_OFFSET) + (rgtX * halfW), pos.y, pos.z - (fwdZ * REAR_AXLE_OFFSET) + (rgtZ * halfW));

        return (flY + frY + rlY + rrY) * 0.25D;
    }

    public static double sampleGroundHeight(Level level, double x, double vehicleY, double z) {
        BlockPos bp = BlockPos.containing(x, vehicleY + 0.8D, z);

        for (int dy = 0; dy <= 4; dy++) {
            BlockPos check = bp.below(dy);
            if (level instanceof ServerLevel serverLevel) {
                double surfaceY = GroundworksApi.getSurfaceWorldY(
                        serverLevel, check, x, z);
                if (Double.isFinite(surfaceY)) {
                    return surfaceY;
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
