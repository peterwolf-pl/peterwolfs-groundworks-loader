package com.piotrek.groundworksloader.integration.groundworks;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import net.minecraft.core.BlockPos;

/**
 * Common abstraction over Groundworks granular terrain.
 */
public interface IGranularTerrainAccess {

    boolean isDiggable(BlockPos pos);

    GranularCell getCell(BlockPos pos);

    GranularCell getOrConvert(BlockPos pos);

    int excavateMicrovoxelsAbove(BlockPos pos, double worldCutY, int maxUnits);

    int deposit(BlockPos pos, GranularMaterial material, int units);

    void markSimulate(BlockPos pos);
}
