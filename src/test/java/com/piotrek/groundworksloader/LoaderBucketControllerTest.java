package com.piotrek.groundworksloader;

import com.piotrek.groundworksloader.bucket.LoaderBucketController;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoaderBucketControllerTest {

    @Test
    @DisplayName("Boom elevation responds to Arrow Up and Arrow Down within physical limits")
    void testBoomLimits() {
        LoaderBucketController controller = new LoaderBucketController();

        // Raise boom (Arrow Up)
        for (int i = 0; i < 50; i++) {
            controller.updateAngles(1.0F, 0.0F);
        }
        assertEquals(LoaderBucketController.MAX_BOOM_ANGLE, controller.boomAngle(), 0.01F);

        // Lower boom (Arrow Down)
        for (int i = 0; i < 80; i++) {
            controller.updateAngles(-1.0F, 0.0F);
        }
        assertEquals(LoaderBucketController.MIN_BOOM_ANGLE, controller.boomAngle(), 0.01F);
    }

    @Test
    @DisplayName("Bucket curl and dump respond within physical limits")
    void testBucketLimits() {
        LoaderBucketController controller = new LoaderBucketController();

        // Curl bucket (Arrow Left / -1.0)
        for (int i = 0; i < 30; i++) {
            controller.updateAngles(0.0F, -1.0F);
        }
        assertEquals(LoaderBucketController.MIN_BUCKET_ANGLE, controller.bucketAngle(), 0.01F);

        // Dump bucket (Arrow Right / +1.0)
        for (int i = 0; i < 60; i++) {
            controller.updateAngles(0.0F, 1.0F);
        }
        assertEquals(LoaderBucketController.MAX_BUCKET_ANGLE, controller.bucketAngle(), 0.01F);
    }

    @Test
    @DisplayName("Cutting edge produces 5 points spanning bucket width")
    void testCuttingEdgePoints() {
        LoaderBucketController controller = new LoaderBucketController();
        Vec3 pos = new Vec3(10.0, 64.0, 10.0);

        List<Vec3> points = controller.getCuttingEdgePoints(pos, 0.0F, 0.0F);
        assertEquals(5, points.size());

        // Leftmost and rightmost span approx 2.8 meters
        Vec3 left = points.get(0);
        Vec3 right = points.get(4);
        double span = left.distanceTo(right);
        assertEquals(LoaderBucketController.BUCKET_WIDTH_METERS, span, 0.15D);
    }
}
