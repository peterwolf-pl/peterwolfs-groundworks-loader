package com.piotrek.groundworksloader.gametest;

import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksloader.bucket.LoaderBucketController;
import com.piotrek.groundworksloader.integration.groundworks.GroundworksLoaderAdapter;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Real Groundworks integration checks for the wheel loader.
 */
public final class LoaderGroundworksGameTest implements FabricClientGameTest {

    private static final int BASE_Y = 180;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .create()) {

            TestServerContext server = singleplayer.getServer();

            server.runOnServer(minecraftServer -> {
                ServerLevel level = minecraftServer.overworld();
                testSweptBucketExcavatesRealGroundworks(level);
                testMaterialAwareAdapterBoundary(level);
            });

            context.waitTicks(5);
        }
    }

    private static void testSweptBucketExcavatesRealGroundworks(ServerLevel level) {
        LoaderBucketController bucket = new LoaderBucketController();
        bucket.setBoomAngle(-5.0F);
        bucket.setBucketAngle(0.0F);

        for (int x = -2; x <= 1; x++) {
            level.setBlock(
                    new BlockPos(x, BASE_Y - 1, 3),
                    Blocks.DIRT.defaultBlockState(),
                    3);
        }

        Vec3 previousVehicle = new Vec3(0.0D, BASE_Y, 0.0D);
        Vec3 currentVehicle = new Vec3(0.0D, BASE_Y, 0.35D);

        // Seed real previous bucket geometry without digging.
        bucket.tick(
                GroundworksLoaderAdapter.of(level),
                previousVehicle,
                0.0F,
                0.0F,
                0.0F
        );

        Vec3 currentLip = bucket.getCuttingEdgePoints(
                currentVehicle, 0.0F, 0.0F).get(2);

        // Measure the whole working strip, not one exact X/Z column. A spherical
        // world-space brush can legitimately remove nearby microvoxels while the
        // top voxel in one sampled column remains unchanged.
        int terrainUnitsBefore = 0;
        for (int x = -2; x <= 1; x++) {
            terrainUnitsBefore += effectiveGranularUnits(
                    level, new BlockPos(x, BASE_Y - 1, 3));
        }

        var scoop = bucket.tick(
                GroundworksLoaderAdapter.of(level),
                currentVehicle,
                0.0F,
                0.0F,
                0.35F
        );

        if (!scoop.isScooping() || scoop.unitsExcavated() <= 0) {
            throw new AssertionError(
                    "Swept production loader bucket did not excavate real Groundworks terrain");
        }
        if (scoop.unitsExcavated() > 160) {
            throw new AssertionError(
                    "Loader excavated more than the swept per-tick volume cap: "
                            + scoop.unitsExcavated());
        }
        if (bucket.carriedUnits() != scoop.unitsExcavated()) {
            throw new AssertionError("Excavated units must be conserved in the loader bucket");
        }

        int terrainUnitsAfter = 0;
        for (int x = -2; x <= 1; x++) {
            terrainUnitsAfter += effectiveGranularUnits(
                    level, new BlockPos(x, BASE_Y - 1, 3));
        }

        int removedFromTerrain = terrainUnitsBefore - terrainUnitsAfter;
        if (removedFromTerrain != scoop.unitsExcavated()) {
            throw new AssertionError(
                    "Real Groundworks terrain volume must decrease exactly by loader intake: "
                            + "terrainRemoved=" + removedFromTerrain
                            + ", bucketIntake=" + scoop.unitsExcavated());
        }

        long affectedColumns = scoop.affectedPositions().stream()
                .map(BlockPos::getX)
                .distinct()
                .count();
        if (affectedColumns < 2) {
            throw new AssertionError(
                    "Swept bucket should distribute excavation across multiple cutting-edge columns; "
                            + "affected=" + scoop.affectedPositions());
        }

        // Now verify the Excavator-style gravity surface search using real world
        // blocks: high bucket lip, solid floor several blocks below.
        bucket.setBoomAngle(45.0F);
        bucket.setBucketAngle(40.0F);
        bucket.resetSweepHistory();

        Vec3 dumpLip = bucket.getCuttingEdgePoints(
                currentVehicle, 0.0F, 0.0F).get(2);
        BlockPos solidFloor = new BlockPos(
                (int) Math.floor(dumpLip.x),
                BASE_Y - 1,
                (int) Math.floor(dumpLip.z)
        );
        level.setBlock(solidFloor, Blocks.SMOOTH_STONE.defaultBlockState(), 3);

        int carriedBeforeDump = bucket.carriedUnits();
        var dump = bucket.tick(
                GroundworksLoaderAdapter.of(level),
                currentVehicle,
                0.0F,
                0.0F,
                0.0F
        );

        if (!dump.isDumping() || dump.unitsDeposited() <= 0) {
            throw new AssertionError("Raised loader bucket did not dump onto searched ground surface");
        }
        if (bucket.carriedUnits() + dump.unitsDeposited() != carriedBeforeDump) {
            throw new AssertionError("Loader dump violated material conservation");
        }

        BlockPos receivingCell = solidFloor.above();
        GranularMaterial deposited = GroundworksApi.getMaterial(level, receivingCell);
        if (deposited == null || deposited == GranularMaterial.EMPTY) {
            throw new AssertionError(
                    "Gravity surface search should deposit Groundworks material directly above the floor");
        }

        if (!(dumpLip.y > receivingCell.getY() + 1.0D)) {
            throw new AssertionError(
                    "GameTest setup must keep the lip well above the receiving surface");
        }
    }

    private static void testMaterialAwareAdapterBoundary(ServerLevel level) {
        BlockPos dirtPos = new BlockPos(10, BASE_Y - 1, 3);
        BlockPos sandPos = new BlockPos(11, BASE_Y - 1, 3);
        level.setBlock(dirtPos, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(sandPos, Blocks.SAND.defaultBlockState(), 3);

        var result = GroundworksLoaderAdapter.of(level).excavateAt(
                new Vec3(11.0D, BASE_Y - 0.5D, 3.5D),
                128,
                GranularMaterialRegistry.DIRT
        );

        if (!result.success()
                || result.material().id() != GranularMaterialRegistry.DIRT.id()) {
            throw new AssertionError("Loader adapter did not excavate requested dirt");
        }
        if (effectiveGranularUnits(level, dirtPos) >= 512) {
            throw new AssertionError("Loader adapter did not remove dirt at mixed boundary");
        }
        if (effectiveGranularUnits(level, sandPos) != 512) {
            throw new AssertionError("Loader adapter modified adjacent sand at mixed boundary");
        }
    }

    private static int effectiveGranularUnits(ServerLevel level, BlockPos pos) {
        var cell = GroundworksApi.queryCell(level, pos);
        if (cell != null) {
            return cell.unitCount();
        }

        GranularMaterial material = GroundworksApi.getMaterial(level, pos);
        return material != null && material != GranularMaterial.EMPTY ? 512 : 0;
    }
}
