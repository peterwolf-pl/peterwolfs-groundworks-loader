package com.piotrek.groundworksloader.client.sound;

import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
import com.piotrek.groundworksloader.vehicle.EngineSoundProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class LoaderEngineSoundController {

    private static final Map<Integer, EnginePair> ACTIVE = new HashMap<>();
    private static ClientLevel activeLevel;

    private LoaderEngineSoundController() {}

    public static void clientTick(Minecraft client) {
        if (activeLevel != client.level) {
            ACTIVE.values().forEach(EnginePair::stopNow);
            ACTIVE.clear();
            activeLevel = client.level;
        }
        if (client.level == null) return;

        Iterator<EnginePair> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            EnginePair pair = iterator.next();
            if (pair.stopped()) {
                pair.stopNow();
                iterator.remove();
            }
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof GroundworksLoaderEntity loader)
                    || !loader.isEngineRunning()
                    || ACTIVE.containsKey(loader.getId())) {
                continue;
            }
            EnginePair pair = new EnginePair(loader);
            ACTIVE.put(loader.getId(), pair);
            client.getSoundManager().play(pair.idle);
            client.getSoundManager().play(pair.load);
        }
    }

    private record EnginePair(EngineLoop idle, EngineLoop load) {
        private EnginePair(GroundworksLoaderEntity loader) {
            this(
                    new EngineLoop(loader, GroundworksLoaderMod.ENGINE_LOOP, false),
                    new EngineLoop(loader, GroundworksLoaderMod.ENGINE_LOAD, true)
            );
        }

        private boolean stopped() {
            return idle.isStopped() || load.isStopped();
        }

        private void stopNow() {
            idle.stopNow();
            load.stopNow();
        }
    }

    private static final class EngineLoop extends AbstractTickableSoundInstance {

        private final GroundworksLoaderEntity loader;
        private final boolean loadLayer;

        private EngineLoop(GroundworksLoaderEntity loader, SoundEvent sound, boolean loadLayer) {
            super(sound, SoundSource.NEUTRAL, RandomSource.create());
            this.loader = loader;
            this.loadLayer = loadLayer;
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
            boolean hydraulicActive = loader.isScooping() || loader.isDumping();
            EngineSoundProfile.Mix mix =
                    EngineSoundProfile.forLoaderState(loader.getForwardSpeed(), hydraulicActive);
            this.volume = loadLayer ? mix.loadVolume() : mix.volume();
            this.pitch = loadLayer ? mix.loadPitch() : mix.pitch();
        }

        private void stopNow() {
            stop();
        }
    }
}
