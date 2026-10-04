package com.piotrek.groundworksloader.item;

import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Item used to deploy the industrial wheel loader vehicle in the world.
 */
public class LoaderItem extends Item {

    public LoaderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos spawnPos = clickedPos.relative(clickedFace);

        Vec3 spawnVec = new Vec3(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D
        );

        AABB bounds = GroundworksLoaderMod.LOADER.getDimensions().makeBoundingBox(spawnVec);
        if (!level.noCollision(bounds)) {
            bounds = bounds.move(0.0D, 0.5D, 0.0D);
            if (!level.noCollision(bounds)) {
                return InteractionResult.FAIL;
            }
            spawnVec = spawnVec.add(0.0D, 0.5D, 0.0D);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        GroundworksLoaderEntity loader = GroundworksLoaderMod.LOADER.create(
                serverLevel,
                EntitySpawnReason.SPAWN_ITEM_USE
        );

        if (loader != null) {
            float playerYaw = context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F;
            loader.snapTo(spawnVec.x, spawnVec.y, spawnVec.z, playerYaw, 0.0F);
            loader.setYHeadRot(playerYaw);
            loader.setYBodyRot(playerYaw);

            serverLevel.addFreshEntity(loader);

            ItemStack itemStack = context.getItemInHand();
            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }
}
