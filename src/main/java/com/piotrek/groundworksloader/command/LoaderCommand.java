package com.piotrek.groundworksloader.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Debug and operator command for Peterwolf's Groundworks Wheel Loader.
 */
public final class LoaderCommand {

    private LoaderCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("loader")
                        .then(Commands.literal("spawn")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerPlayer player = source.getPlayerOrException();
                                    GroundworksLoaderEntity loader = GroundworksLoaderMod.LOADER.create(
                                            source.getLevel(), EntitySpawnReason.COMMAND
                                    );
                                    if (loader != null) {
                                        loader.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
                                        source.getLevel().addFreshEntity(loader);
                                        player.startRiding(loader);
                                        source.sendSuccess(() -> Component.literal("§a[Loader] Spawned and boarded wheel loader."), true);
                                        return 1;
                                    }
                                    return 0;
                                })
                        )
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerPlayer player = source.getPlayerOrException();
                                    if (player.getVehicle() instanceof GroundworksLoaderEntity loader) {
                                        loader.discard();
                                        source.sendSuccess(() -> Component.literal("§a[Loader] Loader removed."), false);
                                        return 1;
                                    }
                                    source.sendFailure(Component.literal("§cMust be driving a wheel loader."));
                                    return 0;
                                })
                        )
        );
    }
}
