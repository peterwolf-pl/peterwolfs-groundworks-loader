package com.piotrek.groundworksloader;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworksloader.integration.groundworks.IGranularTerrainAccess;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory test implementation of {@link IGranularTerrainAccess}.
 */
public class TestGranularTerrain implements IGranularTerrainAccess {

    private static final double EPSILON = 1.0E-9D;

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
    public GranularMaterial getMaterial(BlockPos pos) {
        GranularCell cell = cells.get(pos);
        if (cell != null && !cell.isEmpty()) {
            return cell.material();
        }
        GranularMaterial convertible = convertibleBlocks.get(pos);
        return convertible != null ? convertible : GranularMaterial.EMPTY;
    }

    @Override
    public double getSurfaceWorldY(BlockPos pos, double worldX, double worldZ) {
        GranularCell cell = cells.get(pos);
        if (cell != null) {
            int x = microCoordinate(worldX - pos.getX());
            int z = microCoordinate(worldZ - pos.getZ());
            int top = cell.getColumnHeight(x, z);
            return top < 0
                    ? Double.NEGATIVE_INFINITY
                    : pos.getY() + (top + 1) / (double) GranularCell.RESOLUTION;
        }
        return convertibleBlocks.containsKey(pos)
                ? pos.getY() + 1.0D
                : Double.NEGATIVE_INFINITY;
    }

    @Override
    public int excavateMicrovoxelsAbove(BlockPos pos, double worldCutY, int maxUnits) {
        if (maxUnits <= 0) return 0;

        GranularCell cell = getOrConvert(pos);
        if (cell == null || cell.isEmpty()) return 0;

        double localCutY = (worldCutY - pos.getY()) * GranularCell.RESOLUTION;
        int startY = localCutY <= 0.0D
                ? 0
                : (int) Math.ceil(localCutY - EPSILON);
        if (startY >= GranularCell.RESOLUTION) return 0;

        int removed = 0;
        for (int y = GranularCell.RESOLUTION - 1; y >= startY && removed < maxUnits; y--) {
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

        for (int attempt = 0; attempt < 8 && remaining > 0; attempt++) {
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

    private GranularCell getOrConvert(BlockPos pos) {
        BlockPos key = pos.immutable();
        GranularCell existing = cells.get(key);
        if (existing != null) {
            return existing;
        }

        GranularMaterial material = convertibleBlocks.remove(key);
        if (material == null) {
            return null;
        }

        GranularCell converted = GranularCell.full(material);
        cells.put(key, converted);
        return converted;
    }

    private static int microCoordinate(double localCoordinate) {
        return Math.max(0, Math.min(
                GranularCell.RESOLUTION - 1,
                (int) Math.floor(localCoordinate * GranularCell.RESOLUTION)
        ));
    }
}
