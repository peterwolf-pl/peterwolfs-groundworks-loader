package com.piotrek.groundworksloader;

import com.piotrek.groundworksloader.vehicle.LoaderMovementController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoaderMovementControllerTest {

    @Test
    @DisplayName("Forward throttle accelerates up to maximum forward speed")
    void testForwardAcceleration() {
        LoaderMovementController controller = new LoaderMovementController();
        assertEquals(0.0F, controller.forwardSpeed());

        // Hold W for 20 ticks
        for (int i = 0; i < 20; i++) {
            controller.step(1.0F, 0.0F);
        }

        assertTrue(controller.forwardSpeed() > 0.20F);
        assertTrue(controller.forwardSpeed() <= LoaderMovementController.MAX_FORWARD_SPEED);
    }

    @Test
    @DisplayName("Reverse throttle decelerates and reverses up to maximum reverse speed")
    void testReverseAcceleration() {
        LoaderMovementController controller = new LoaderMovementController();

        // Hold S for 20 ticks
        for (int i = 0; i < 20; i++) {
            controller.step(-1.0F, 0.0F);
        }

        assertTrue(controller.forwardSpeed() < 0.0F);
        assertTrue(controller.forwardSpeed() >= LoaderMovementController.MAX_REVERSE_SPEED);
    }

    @Test
    @DisplayName("Steering left and right rotates articulation angle and self-centers")
    void testSteeringAndCentering() {
        LoaderMovementController controller = new LoaderMovementController();

        // Steer left (A)
        for (int i = 0; i < 15; i++) {
            controller.step(0.0F, -1.0F);
        }
        assertTrue(controller.steerAngle() < -30.0F);

        // Release steer: self centers towards 0
        for (int i = 0; i < 15; i++) {
            controller.step(0.0F, 0.0F);
        }
        assertEquals(0.0F, controller.steerAngle(), 0.1F);

        // Steer right (D)
        for (int i = 0; i < 15; i++) {
            controller.step(0.0F, 1.0F);
        }
        assertTrue(controller.steerAngle() > 30.0F);
    }

    @Test
    @DisplayName("Rolling wheel rotation advances with movement")
    void testWheelRotation() {
        LoaderMovementController controller = new LoaderMovementController();
        assertEquals(0.0F, controller.wheelRotation());

        controller.step(1.0F, 0.0F);
        assertTrue(controller.wheelRotation() > 0.0F);
    }

    @Test
    @DisplayName("Two-axle terrain pitch angles nose up when front axle climbs elevation")
    void testTwoAxlePitchOrientation() {
        LoaderMovementController controller = new LoaderMovementController();
        assertEquals(0.0F, controller.vehiclePitch());

        // Front axle at Y = 65.0, rear axle at Y = 64.0 (1 block elevation on front wheels)
        double frontY = 65.0D;
        double rearY = 64.0D;
        double expectedTargetPitch = Math.toDegrees(Math.atan2(rearY - frontY, LoaderMovementController.WHEELBASE));

        // Expected pitch is negative (-18.43 deg with 3.0m wheelbase), tilting the front axle up
        assertTrue(expectedTargetPitch < -15.0D);
        assertTrue(expectedTargetPitch > -25.0D);

        controller.setOrientation((float) expectedTargetPitch, 0.0F);
        assertEquals((float) expectedTargetPitch, controller.vehiclePitch(), 1e-4F);
    }
}
