package com.piotrek.groundworksloader.gametest;

import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.material.GranularMaterial;
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
        BlockPos contactCell = BlockPos.containing(
                currentLip.x, currentLip.y - 0.10D, currentLip.z).below();

        // The exact candidate may be the direct or below cell depending on the
        // cutting edge's fractional Y. Query the known floor cell at Z=3.
        BlockPos floorCell = new BlockPos(
                (int) Math.floor(currentLip.x), BASE_Y - 1, 3);
        double before = GroundworksApi.getSurfaceWorldY(
                level, floorCell, currentLip.x, currentLip.z);

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

        double after = GroundworksApi.getSurfaceWorldY(
                level, floorCell, currentLip.x, currentLip.z);
        if (!(after < before)) {
            throw new AssertionError(
                    "Groundworks surface should be locally cut by the swept bucket: before="
                            + before + ", after=" + after);
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
}
