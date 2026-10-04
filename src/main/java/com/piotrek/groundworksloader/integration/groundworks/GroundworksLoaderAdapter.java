package com.piotrek.groundworksloader.integration.groundworks;

import com.piotrek.groundworks.GroundworksMod;
import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.deposit.DepositResult;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.block.entity.GranularBlockEntity;
import com.piotrek.groundworks.networking.GranularSyncHandler;
import com.piotrek.groundworks.terrain.cell.DirtyFlags;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworks.terrain.conversion.BlockConverter;
import com.piotrek.groundworks.terrain.storage.GranularWorldStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-side adapter bridging the Wheel Loader to Peterwolf's Groundworks engine.
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
        GranularCell cell = GroundworksApi.queryCell(level, pos);
        if (cell != null && !cell.isEmpty()) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        return BlockConverter.isConvertible(state);
    }

    @Override
    public GranularCell getCell(BlockPos pos) {
        return GroundworksApi.queryCell(level, pos);
    }

    @Override
    public GranularCell getOrConvert(BlockPos pos) {
        GranularWorldStorage storage = GranularWorldStorage.get(level);
        return storage.getOrConvert(pos);
    }

    @Override
    public int excavateMicrovoxelsAbove(BlockPos pos, double worldCutY, int maxUnits) {
        if (maxUnits <= 0) {
            return 0;
        }

        GranularWorldStorage storage = GranularWorldStorage.get(level);
        GranularCell cell = storage.getOrConvert(pos);
        if (cell == null || cell.isEmpty()) {
            return 0;
        }

        double localCutY = (worldCutY - pos.getY()) * GranularCell.RESOLUTION;
        if (localCutY >= GranularCell.RESOLUTION) {
            return 0;
        }

        int removed = 0;
        int startY = Math.max(0, (int) Math.floor(localCutY));

        for (int y = GranularCell.RESOLUTION - 1; y >= startY && removed < maxUnits; y--) {
            if (y < localCutY) {
                continue;
            }
            for (int z = 0; z < GranularCell.RESOLUTION && removed < maxUnits; z++) {
                for (int x = 0; x < GranularCell.RESOLUTION && removed < maxUnits; x++) {
                    if (cell.clear(x, y, z)) {
                        removed++;
                    }
                }
            }
        }

        if (removed > 0) {
            syncCell(storage, pos, cell);
        }

        return removed;
    }

    @Override
    public int deposit(BlockPos pos, GranularMaterial material, int units) {
        if (units <= 0 || material == null || material.id() == 0) {
            return 0;
        }
        DepositResult result = GroundworksApi.depositWithOverflow(level, pos, material, units);
        return result.unitsDeposited();
    }

    @Override
    public void markSimulate(BlockPos pos) {
        GranularWorldStorage storage = GranularWorldStorage.get(level);
        GranularCell cell = storage.getCell(pos);
        if (cell != null) {
            cell.markDirty(DirtyFlags.SIMULATE);
            storage.enqueueDirty(pos);
            storage.setDirty();
        }
    }

    private void syncCell(GranularWorldStorage storage, BlockPos pos, GranularCell cell) {
        cell.markDirty(DirtyFlags.SYNC | DirtyFlags.MESH | DirtyFlags.SIMULATE);
        storage.enqueueDirty(pos);
        storage.setDirty();

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof GranularBlockEntity gbe) {
            gbe.setMaterialId(cell.materialId());
            gbe.setChanged();
            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }

        GranularSyncHandler.sendCellUpdate(level, pos, cell);

        if (cell.isEmpty()) {
            storage.removeCell(pos);
            level.removeBlock(pos, false);
        }
    }
}
