package com.piotrek.groundworksloader;

import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworksloader.integration.groundworks.IGranularTerrainAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory test implementation of {@link IGranularTerrainAccess}.
 */
public class TestGranularTerrain implements IGranularTerrainAccess {

    private static final double DEFAULT_EXCAVATION_RADIUS = 0.4D;

    private final Map<BlockPos, GranularCell> cells = new HashMap<>();
    private final Map<BlockPos, GranularMaterial> convertibleBlocks = new HashMap<>();
    private final Set<BlockPos> solidBlocks = new HashSet<>();
    private final Set<BlockPos> simulatedPositions = new HashSet<>();

    private record VoxelCandidate(
            BlockPos pos,
            GranularCell cell,
            int x,
            int y,
            int z,
            double distanceSq
    ) {}

    public void putCell(BlockPos pos, GranularCell cell) {
        cells.put(pos.immutable(), cell);
    }

    public GranularCell getCell(BlockPos pos) {
        return cells.get(pos);
    }

    public void createFullCell(BlockPos pos, GranularMaterial material) {
        cells.put(pos.immutable(), GranularCell.full(material));
    }

    public void createConvertibleBlock(BlockPos pos, GranularMaterial material) {
        convertibleBlocks.put(pos.immutable(), material);
    }

    public void createSolidBlock(BlockPos pos) {
        solidBlocks.add(pos.immutable());
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
    public ExcavationResult excavateAt(Vec3 worldPoint, int maxUnits) {
        if (maxUnits <= 0) {
            return ExcavationResult.NONE;
        }

        double radius = DEFAULT_EXCAVATION_RADIUS;
        double radiusSq = radius * radius;

        int minBlockX = (int) Math.floor(worldPoint.x - radius);
        int maxBlockX = (int) Math.floor(worldPoint.x + radius);
        int minBlockY = (int) Math.floor(worldPoint.y - radius);
        int maxBlockY = (int) Math.floor(worldPoint.y + radius);
        int minBlockZ = (int) Math.floor(worldPoint.z - radius);
        int maxBlockZ = (int) Math.floor(worldPoint.z + radius);

        List<VoxelCandidate> candidates = new ArrayList<>();

        for (int bx = minBlockX; bx <= maxBlockX; bx++) {
            for (int by = minBlockY; by <= maxBlockY; by++) {
                for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    GranularCell cell = getOrConvert(pos);
                    if (cell == null || cell.isEmpty()) {
                        continue;
                    }

                    for (int y = 0; y < GranularCell.RESOLUTION; y++) {
                        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
                            for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                                if (!cell.get(x, y, z)) {
                                    continue;
                                }

                                double vx = bx + (x + 0.5D) / GranularCell.RESOLUTION;
                                double vy = by + (y + 0.5D) / GranularCell.RESOLUTION;
                                double vz = bz + (z + 0.5D) / GranularCell.RESOLUTION;

                                double dx = vx - worldPoint.x;
                                double dy = vy - worldPoint.y;
                                double dz = vz - worldPoint.z;
                                double distanceSq = dx * dx + dy * dy + dz * dz;

                                if (distanceSq <= radiusSq) {
                                    candidates.add(new VoxelCandidate(
                                            pos.immutable(), cell, x, y, z, distanceSq));
                                }
                            }
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return ExcavationResult.NONE;
        }

        candidates.sort(Comparator.comparingDouble(VoxelCandidate::distanceSq));
        GranularMaterial material = candidates.getFirst().cell().material();
        int materialId = material.id();

        int removed = 0;
        Set<BlockPos> affected = new HashSet<>();

        for (VoxelCandidate candidate : candidates) {
            if (removed >= maxUnits) {
                break;
            }
            if (candidate.cell().materialId() != materialId) {
                continue;
            }
            if (candidate.cell().clear(candidate.x(), candidate.y(), candidate.z())) {
                removed++;
                affected.add(candidate.pos());
            }
        }

        for (BlockPos pos : affected) {
            GranularCell cell = cells.get(pos);
            if (cell != null && cell.isEmpty()) {
                cells.remove(pos);
            }
        }

        return removed > 0
                ? new ExcavationResult(material, removed, List.copyOf(affected))
                : ExcavationResult.NONE;
    }

    @Override
    @Nullable
    public BlockPos findDepositSurface(
            Vec3 lip,
            GranularMaterial material,
            int maxDropBlocks
    ) {
        BlockPos start = BlockPos.containing(lip);
        int minY = start.getY() - Math.max(1, maxDropBlocks);

        for (int y = start.getY(); y >= minY; y--) {
            BlockPos check = new BlockPos(start.getX(), y, start.getZ());
            GranularMaterial terrainMaterial = getMaterial(check);

            if (terrainMaterial != GranularMaterial.EMPTY) {
                return terrainMaterial.id() == material.id()
                        ? check
                        : check.above();
            }

            if (solidBlocks.contains(check)) {
                return check.above();
            }
        }

        return null;
    }

    @Override
    public int deposit(BlockPos pos, GranularMaterial material, int units) {
        if (units <= 0 || material == null || material.id() == 0) {
            return 0;
        }

        int remaining = units;
        int totalAdded = 0;
        BlockPos current = pos;

        for (int attempt = 0; attempt < 8 && remaining > 0; attempt++) {
            if (solidBlocks.contains(current)) {
                current = current.above();
                continue;
            }

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
