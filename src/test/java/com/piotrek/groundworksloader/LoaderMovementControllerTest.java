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
}
