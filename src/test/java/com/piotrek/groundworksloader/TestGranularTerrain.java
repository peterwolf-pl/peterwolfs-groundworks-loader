package com.piotrek.groundworksloader;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworksloader.integration.groundworks.IGranularTerrainAccess;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory test implementation of {@link IGranularTerrainAccess} for deterministic unit testing.
 */
public class TestGranularTerrain implements IGranularTerrainAccess {

    private final Map<BlockPos, GranularCell> cells = new HashMap<>();
    private final Map<BlockPos, GranularMaterial> convertibleBlocks = new HashMap<>();
    private final Set<BlockPos> simulatedPositions = new HashSet<>();

    public void putCell(BlockPos pos, GranularCell cell) {
        cells.put(pos.immutable(), cell);
    }

    public void createFullCell(BlockPos pos, GranularMaterial material) {
        cells.put(pos.immutable(), GranularCell.full(material));
    }

    public void createConvertibleBlock(BlockPos pos, GranularMaterial material) {
        convertibleBlocks.put(pos.immutable(), material);
    }

    public int countTotalWorldUnits() {
        int total = 0;
        for (GranularCell cell : cells.values()) {
            total += cell.unitCount();
        }
        return total;
    }

    public Set<BlockPos> simulatedPositions() {
        return simulatedPositions;
    }

    @Override
    public boolean isDiggable(BlockPos pos) {
        GranularCell cell = cells.get(pos);
        return (cell != null && !cell.isEmpty()) || convertibleBlocks.containsKey(pos);
    }

    @Override
    public GranularCell getCell(BlockPos pos) {
        return cells.get(pos);
    }

    @Override
    public GranularCell getOrConvert(BlockPos pos) {
        BlockPos key = pos.immutable();
        GranularCell existing = cells.get(key);
        if (existing != null) return existing;
        GranularMaterial material = convertibleBlocks.remove(key);
        GranularCell converted = material != null
                ? GranularCell.full(material)
                : GranularCell.empty();
        if (material == null) converted.setMaterialId(GranularMaterialRegistry.DIRT.id());
        cells.put(key, converted);
        return converted;
    }

    @Override
    public int excavateMicrovoxelsAbove(BlockPos pos, double worldCutY, int maxUnits) {
        if (maxUnits <= 0) return 0;
        GranularCell cell = cells.get(pos);
        if (cell == null || cell.isEmpty()) return 0;

        double localCutY = (worldCutY - pos.getY()) * GranularCell.RESOLUTION;
        if (localCutY >= GranularCell.RESOLUTION) return 0;

        int removed = 0;
        int startY = Math.max(0, (int) Math.floor(localCutY));

        for (int y = GranularCell.RESOLUTION - 1; y >= startY && removed < maxUnits; y--) {
            if (y < localCutY) continue;
            for (int z = 0; z < GranularCell.RESOLUTION && removed < maxUnits; z++) {
                for (int x = 0; x < GranularCell.RESOLUTION && removed < maxUnits; x++) {
                    if (cell.clear(x, y, z)) {
                        removed++;
                    }
                }
            }
        }

        if (cell.isEmpty()) {
            cells.remove(pos);
        }

        return removed;
    }

    @Override
    public int deposit(BlockPos pos, GranularMaterial material, int units) {
        if (units <= 0 || material == null || material.id() == 0) return 0;

        int remaining = units;
        int totalAdded = 0;
        BlockPos current = pos;

        for (int attempt = 0; attempt < 4 && remaining > 0; attempt++) {
            GranularCell cell = cells.computeIfAbsent(current.immutable(), p -> {
                GranularCell c = GranularCell.empty();
                c.setMaterialId(material.id());
                return c;
            });

            if (cell.isEmpty()) {
                cell.setMaterialId(material.id());
            }

            if (cell.materialId() == material.id()) {
                int added = cell.addFromBottom(remaining);
                totalAdded += added;
                remaining -= added;
            }

            if (remaining > 0) {
                current = current.above();
            }
        }

        return totalAdded;
    }

    @Override
    public void markSimulate(BlockPos pos) {
        simulatedPositions.add(pos.immutable());
    }
}
