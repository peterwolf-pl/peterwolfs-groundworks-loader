package com.piotrek.groundworksloader;

import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
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
        assertTrue(result.unitsExcavated() <= 160,
                "Swept bucket excavation must be locally bounded per tick, not shave full cell layers");
        assertEquals(result.unitsExcavated(), controller.carriedUnits());

        int remainingTerrainUnits = terrain.countTotalWorldUnits();

        // Volume conservation: initial world units = remaining world units + loader carried units
        assertEquals(initialTerrainUnits, remainingTerrainUnits + controller.carriedUnits());
    }

    @Test
    @DisplayName("Curled bucket retains material without dumping until opened")
    void testCurledBucketDoesNotDump() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        LoaderBucketController controller = new LoaderBucketController();

        // Pre-fill bucket with 150 units of dirt
        controller.setCarriedUnits(150);
        controller.setCarriedMaterial(GranularMaterialRegistry.DIRT);

        // Curled closed bucket (-20 deg)
        controller.setBoomAngle(20.0F);
        controller.setBucketAngle(-20.0F);

        Vec3 loaderPos = new Vec3(0.0, 64.0, 0.0);
        BucketTickResult result = controller.tick(terrain, loaderPos, 0.0F, 0.0F, 0.0F);

        assertFalse(result.isDumping());
        assertEquals(0, result.unitsDeposited());
        assertEquals(150, controller.carriedUnits());

        // Now open / dump bucket (+35 deg). Give the gravity search a real floor.
        controller.setBucketAngle(35.0F);
        Vec3 dumpLip = controller.getCuttingEdgePoints(loaderPos, 0.0F, 0.0F).get(2);
        terrain.createSolidBlock(new BlockPos(
                (int) Math.floor(dumpLip.x), 63, (int) Math.floor(dumpLip.z)));

        BucketTickResult dumpResult = controller.tick(terrain, loaderPos, 0.0F, 0.0F, 0.0F);

        assertTrue(dumpResult.isDumping());
        assertTrue(dumpResult.unitsDeposited() > 0);
        assertEquals(150, controller.carriedUnits() + terrain.countTotalWorldUnits());
    }

    @Test
    @DisplayName("Boom lowered below grade into trench excavates deep block microvoxels")
    void testBelowGradeTrenchExcavation() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        LoaderBucketController controller = new LoaderBucketController();

        // Place dirt 1 block below surface level (Y = 63, Z = 3)
        BlockPos belowPos = new BlockPos(0, 63, 3);
        terrain.createFullCell(belowPos, GranularMaterialRegistry.DIRT);

        assertEquals(512, terrain.countTotalWorldUnits());

        // Lower boom deeply (-25 deg)
        controller.setBoomAngle(-25.0F);
        controller.setBucketAngle(0.0F);

        // Position loader so cutting edge (teethRelZ = 2.7m) reaches into block Z = 3
        Vec3 loaderPos = new Vec3(0.0, 64.0, 0.5);
        BucketTickResult result = controller.tick(terrain, loaderPos, 0.0F, 0.0F, 0.15F);

        assertTrue(result.isScooping());
        assertTrue(result.unitsExcavated() > 0);
        assertEquals(512, controller.carriedUnits() + terrain.countTotalWorldUnits());
    }

    @Test
    @DisplayName("Raised bucket hovering in air does not scoop ground floor material")
    void testRaisedBoomDoesNotScoopFloor() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        LoaderBucketController controller = new LoaderBucketController();

        // Place floor dirt block at Y = 63 (so surface is at 64.0)
        BlockPos floorPos = new BlockPos(0, 63, 3);
        terrain.createFullCell(floorPos, GranularMaterialRegistry.DIRT);

        // Raise boom into driving transport position (+15 deg)
        controller.setBoomAngle(15.0F);
        controller.setBucketAngle(0.0F);

        Vec3 loaderPos = new Vec3(0.0, 64.0, 0.0);
        BucketTickResult result = controller.tick(terrain, loaderPos, 0.0F, 0.0F, 0.20F);

        // Bucket is high in the air above Y = 64.0, must NOT scoop or alter floor
        assertFalse(result.isScooping());
        assertEquals(0, result.unitsExcavated());
        assertEquals(0, controller.carriedUnits());
        assertEquals(512, terrain.countTotalWorldUnits());
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

        // Raise the bucket high enough that depositing at the lip would visibly float.
        controller.setBoomAngle(45.0F);
        controller.setBucketAngle(40.0F);

        Vec3 loaderPos = new Vec3(0.0, 64.0, 0.0);
        Vec3 dumpLip = controller.getCuttingEdgePoints(loaderPos, 0.0F, 0.0F).get(2);
        BlockPos floor = new BlockPos(
                (int) Math.floor(dumpLip.x), 63, (int) Math.floor(dumpLip.z));
        terrain.createSolidBlock(floor);

        BucketTickResult result = controller.tick(terrain, loaderPos, 0.0F, 0.0F, 0.0F);

        assertTrue(result.isDumping());
        assertTrue(result.unitsDeposited() > 0);

        int finalCarried = controller.carriedUnits();
        int finalTerrain = terrain.countTotalWorldUnits();

        // Volume conservation: initial carried = final carried + final deposited in terrain
        assertEquals(initialCarried, finalCarried + finalTerrain);

        BlockPos expectedSurfaceCell = floor.above();
        assertNotNull(terrain.getCell(expectedSurfaceCell),
                "Dump must land on the searched ground surface, not remain at the raised bucket lip");
        assertTrue(BlockPos.containing(dumpLip).getY() > expectedSurfaceCell.getY(),
                "Test setup must keep the bucket lip clearly above the receiving surface");
    }
}
