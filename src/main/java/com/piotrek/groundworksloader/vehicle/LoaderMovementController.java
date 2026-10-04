package com.piotrek.groundworksloader.vehicle;

import net.minecraft.util.Mth;

/**
 * High-fidelity 4-wheel drive and articulated steering physics controller for the industrial wheel loader.
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
}
