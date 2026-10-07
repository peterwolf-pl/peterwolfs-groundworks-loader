package com.piotrek.groundworksloader.integration.groundworks;

import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Testable machine-facing subset of the public Groundworks terrain API.
 */
public interface IGranularTerrainAccess {

    boolean isDiggable(BlockPos pos);

    GranularMaterial getMaterial(BlockPos pos);

    double getSurfaceWorldY(BlockPos pos, double worldX, double worldZ);

    /**
     * Removes only material intersecting the Groundworks world-space excavation brush.
     */
    default ExcavationResult excavateAt(Vec3 worldPoint, int maxUnits) {
        return excavateAt(worldPoint, maxUnits, GranularMaterial.EMPTY);
    }

    /**
     * Removes only the requested material. EMPTY means that the terrain API may
     * select the first material touched by the brush.
     */
    ExcavationResult excavateAt(
            Vec3 worldPoint,
            int maxUnits,
            GranularMaterial requiredMaterial
    );

    /**
     * Finds the receiving terrain cell directly below a dumping lip.
     *
     * <p>This mirrors the excavator gravity search: same material may continue
     * filling a partial cell, while other material or vanilla terrain receives
     * the dump in the cell above.
     */
    @Nullable
    BlockPos findDepositSurface(Vec3 lip, GranularMaterial material, int maxDropBlocks);

    int deposit(BlockPos pos, GranularMaterial material, int units);

    /**
     * Attempt to transfer material into a world-space machine/container under
     * the bucket lip before falling back to terrain deposition.
     *
     * <p>receiverPresent=true with unitsConsumed=0 is meaningful: a receiver
     * exists but could not accept or overflow this material, so the source must
     * keep it instead of silently dumping elsewhere.</p>
     */
    default ContainerTransferResult transferToWorldContainer(
            Vec3 lip,
            GranularMaterial material,
            int units
    ) {
        return ContainerTransferResult.NONE;
    }

    record ContainerTransferResult(
            boolean receiverPresent,
            int unitsConsumed
    ) {
        public static final ContainerTransferResult NONE =
                new ContainerTransferResult(false, 0);
    }

    void markSimulate(BlockPos pos);
}
