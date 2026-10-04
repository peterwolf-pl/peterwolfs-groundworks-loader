package com.piotrek.groundworksloader.client.sound;

import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
import com.piotrek.groundworksloader.vehicle.EngineSoundProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Starts and tracks positional diesel engine audio loops for running wheel loaders.
 */
public final class LoaderEngineSoundController {

    private static final Map<Integer, EngineLoop> ACTIVE = new HashMap<>();
    private static ClientLevel activeLevel;

    private LoaderEngineSoundController() {}

    public static void clientTick(Minecraft client) {
        if (activeLevel != client.level) {
            ACTIVE.values().forEach(EngineLoop::stopNow);
            ACTIVE.clear();
            activeLevel = client.level;
        }
        if (client.level == null) return;

        Iterator<EngineLoop> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isStopped()) iterator.remove();
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof GroundworksLoaderEntity loader)
                    || !loader.isEngineRunning()
                    || ACTIVE.containsKey(loader.getId())) {
                continue;
            }
            EngineLoop sound = new EngineLoop(loader);
            ACTIVE.put(loader.getId(), sound);
            client.getSoundManager().play(sound);
        }
    }

    private static final class EngineLoop extends AbstractTickableSoundInstance {

        private final GroundworksLoaderEntity loader;

        private EngineLoop(GroundworksLoaderEntity loader) {
            super(GroundworksLoaderMod.ENGINE_LOOP, SoundSource.NEUTRAL, RandomSource.create());
            this.loader = loader;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            updateSound();
        }

        @Override
        public void tick() {
            if (loader.isRemoved() || !loader.isEngineRunning()) {
                stop();
                return;
            }
            updateSound();
        }

        private void updateSound() {
            this.x = loader.getX();
            this.y = loader.getY() + 1.2D;
            this.z = loader.getZ();
            boolean isHydraulic = loader.isScooping() || loader.isDumping();
            EngineSoundProfile.Mix mix = EngineSoundProfile.forLoaderState(loader.getForwardSpeed(), isHydraulic);
            this.volume = mix.volume();
            this.pitch = mix.pitch();
        }

        private void stopNow() {
            stop();
        }
    }
}
