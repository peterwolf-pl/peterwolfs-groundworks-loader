package com.piotrek.groundworksloader;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworksloader.bucket.LoaderBucketController;
import com.piotrek.groundworksloader.bucket.LoaderBucketController.BucketTickResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoaderTerrainInteractionTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Lowered bucket scooping excavated material into bucket and conserves volume")
    void testScoopingVolumeConservation() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        LoaderBucketController controller = new LoaderBucketController();

        // Place a dirt pile at (0, 64, 3) where cutting edge will pass
        BlockPos dirtPos = new BlockPos(0, 64, 3);
        terrain.createFullCell(dirtPos, GranularMaterialRegistry.DIRT);

        int initialTerrainUnits = terrain.countTotalWorldUnits();
        assertEquals(512, initialTerrainUnits);
        assertEquals(0, controller.carriedUnits());

        // Lower boom to dig height (-5 deg) and drive forward
        controller.setBoomAngle(-5.0F);
        controller.setBucketAngle(0.0F);

        Vec3 loaderPos = new Vec3(0.0, 64.0, 0.0);
        float forwardSpeed = 0.20F;

        BucketTickResult result = controller.tick(terrain, loaderPos, 0.0F, 0.0F, forwardSpeed);

        assertTrue(result.isScooping());
        assertTrue(result.unitsExcavated() > 0);
        assertEquals(result.unitsExcavated(), controller.carriedUnits());

        int remainingTerrainUnits = terrain.countTotalWorldUnits();

        // Volume conservation: initial world units = remaining world units + loader carried units
        assertEquals(initialTerrainUnits, remainingTerrainUnits + controller.carriedUnits());
    }

    @Test
    @DisplayName("Tilted bucket dumps material to ground and conserves volume")
    void testDumpingVolumeConservation() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        LoaderBucketController controller = new LoaderBucketController();

        // Pre-fill bucket with 200 units of sand
        controller.setCarriedUnits(200);
        controller.setCarriedMaterial(GranularMaterialRegistry.SAND);

        int initialCarried = controller.carriedUnits();
        int initialTerrain = terrain.countTotalWorldUnits();
        assertEquals(200, initialCarried);
        assertEquals(0, initialTerrain);

        // Raise boom slightly and tilt bucket down into dump position (+40 deg)
        controller.setBoomAngle(15.0F);
        controller.setBucketAngle(40.0F);

        Vec3 loaderPos = new Vec3(0.0, 64.0, 0.0);

        BucketTickResult result = controller.tick(terrain, loaderPos, 0.0F, 0.0F, 0.0F);

        assertTrue(result.isDumping());
        assertTrue(result.unitsDeposited() > 0);

        int finalCarried = controller.carriedUnits();
        int finalTerrain = terrain.countTotalWorldUnits();

        // Volume conservation: initial carried = final carried + final deposited in terrain
        assertEquals(initialCarried, finalCarried + finalTerrain);
    }
}
