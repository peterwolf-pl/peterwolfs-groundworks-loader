package com.piotrek.groundworksloader.integration.groundworks;

import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.container.GranularContainerTransferApi;
import com.piotrek.groundworks.api.container.IWorldGranularContainer;
import com.piotrek.groundworks.api.deposit.DepositResult;
import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Thin server-side bridge from the Wheel Loader to the public Groundworks API.
 */
public class GroundworksLoaderAdapter implements IGranularTerrainAccess {

    private final ServerLevel level;
    @Nullable
    private final Entity source;

    public GroundworksLoaderAdapter(ServerLevel level) {
        this(level, null);
    }

    public GroundworksLoaderAdapter(ServerLevel level, @Nullable Entity source) {
        this.level = level;
        this.source = source;
    }

    public static GroundworksLoaderAdapter of(ServerLevel level) {
        return new GroundworksLoaderAdapter(level, null);
    }

    public static GroundworksLoaderAdapter of(ServerLevel level, Entity source) {
        return new GroundworksLoaderAdapter(level, source);
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
    public ExcavationResult excavateAt(
            Vec3 worldPoint,
            int maxUnits,
            GranularMaterial requiredMaterial
    ) {
        if (maxUnits <= 0) {
            return ExcavationResult.NONE;
        }
        return GroundworksApi.excavateAt(
                level,
                worldPoint,
                maxUnits,
                requiredMaterial
        );
    }

    @Override
    @Nullable
    public BlockPos findDepositSurface(Vec3 lip, GranularMaterial material, int maxDropBlocks) {
        BlockPos start = BlockPos.containing(lip.x, lip.y, lip.z);
        int lipY = start.getY();
        int minY = Math.max(level.getMinY(), lipY - Math.max(1, maxDropBlocks));

        for (int y = lipY; y >= minY; y--) {
            BlockPos checkPos = new BlockPos(start.getX(), y, start.getZ());
            GranularMaterial terrainMaterial = getMaterial(checkPos);

            if (terrainMaterial != GranularMaterial.EMPTY) {
                return terrainMaterial.id() == material.id()
                        ? checkPos
                        : checkPos.above();
            }

            BlockState state = level.getBlockState(checkPos);
            if (!state.isAir()) {
                return checkPos.above();
            }
        }

        return null;
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
    public ContainerTransferResult transferToWorldContainer(
            Vec3 lip,
            GranularMaterial material,
            int units
    ) {
        if (units <= 0 || material == null || material.id() == 0) {
            return ContainerTransferResult.NONE;
        }

        IWorldGranularContainer receiver =
                GranularContainerTransferApi.findReceiver(
                        level,
                        lip,
                        source,
                        4.0D
                );

        if (receiver == null) {
            return ContainerTransferResult.NONE;
        }

        int consumed = receiver.receiveMaterialAt(
                level,
                lip,
                material,
                units
        );

        return new ContainerTransferResult(
                true,
                Math.clamp(consumed, 0, units)
        );
    }

    @Override
    public void markSimulate(BlockPos pos) {
        GroundworksApi.markForSimulation(level, pos);
    }
}
