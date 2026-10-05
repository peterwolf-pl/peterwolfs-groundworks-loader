package com.piotrek.groundworksloader.integration.groundworks;

import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.deposit.DepositResult;
import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Thin server-side bridge from the Wheel Loader to the public Groundworks API.
 */
public class GroundworksLoaderAdapter implements IGranularTerrainAccess {

    private final ServerLevel level;

    public GroundworksLoaderAdapter(ServerLevel level) {
        this.level = level;
    }

    public static GroundworksLoaderAdapter of(ServerLevel level) {
        return new GroundworksLoaderAdapter(level);
    }

    @Override
    public boolean isDiggable(BlockPos pos) {
        return GroundworksApi.isDiggable(level, pos);
    }

    @Override
    public GranularMaterial getMaterial(BlockPos pos) {
        GranularMaterial material = GroundworksApi.getMaterial(level, pos);
        return material != null ? material : GranularMaterial.EMPTY;
    }

    @Override
    public double getSurfaceWorldY(BlockPos pos, double worldX, double worldZ) {
        return GroundworksApi.getSurfaceWorldY(level, pos, worldX, worldZ);
    }

    @Override
    public int excavateMicrovoxelsAbove(BlockPos pos, double worldCutY, int maxUnits) {
        if (maxUnits <= 0) {
            return 0;
        }
        ExcavationResult result =
                GroundworksApi.excavateAbove(level, pos, worldCutY, maxUnits);
        return result.unitsRemoved();
    }

    @Override
    public int deposit(BlockPos pos, GranularMaterial material, int units) {
        if (units <= 0 || material == null || material.id() == 0) {
            return 0;
        }
        DepositResult result =
                GroundworksApi.depositWithOverflow(level, pos, material, units);
        return result.unitsDeposited();
    }

    @Override
    public void markSimulate(BlockPos pos) {
        GroundworksApi.markForSimulation(level, pos);
    }
}
