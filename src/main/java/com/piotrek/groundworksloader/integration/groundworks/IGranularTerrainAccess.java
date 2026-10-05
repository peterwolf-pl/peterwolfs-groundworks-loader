package com.piotrek.groundworksloader.integration.groundworks;

import com.piotrek.groundworks.api.material.GranularMaterial;
import net.minecraft.core.BlockPos;

/**
 * Testable machine-facing subset of the public Groundworks terrain API.
 */
public interface IGranularTerrainAccess {

    boolean isDiggable(BlockPos pos);

    GranularMaterial getMaterial(BlockPos pos);

    double getSurfaceWorldY(BlockPos pos, double worldX, double worldZ);

    int excavateMicrovoxelsAbove(BlockPos pos, double worldCutY, int maxUnits);

    int deposit(BlockPos pos, GranularMaterial material, int units);

    void markSimulate(BlockPos pos);
}
