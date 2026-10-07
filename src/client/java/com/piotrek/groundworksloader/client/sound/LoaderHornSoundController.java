package com.piotrek.groundworksloader.client.sound;

import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
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

public final class LoaderHornSoundController {

    private static final Map<Integer, HornLoop> ACTIVE = new HashMap<>();
    private static ClientLevel activeLevel;

    private LoaderHornSoundController() {}

    public static void clientTick(Minecraft client) {
        if (activeLevel != client.level) {
            ACTIVE.values().forEach(HornLoop::stopNow);
            ACTIVE.clear();
            activeLevel = client.level;
        }
        if (client.level == null) return;

        Iterator<HornLoop> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            HornLoop horn = iterator.next();
            if (horn.isStopped()) iterator.remove();
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof GroundworksLoaderEntity loader)
                    || !loader.isHornHeld()
                    || ACTIVE.containsKey(loader.getId())) {
                continue;
            }
            HornLoop horn = new HornLoop(loader);
            ACTIVE.put(loader.getId(), horn);
            client.getSoundManager().play(horn);
        }
    }

    private static final class HornLoop extends AbstractTickableSoundInstance {

        private final GroundworksLoaderEntity loader;

        private HornLoop(GroundworksLoaderEntity loader) {
            super(GroundworksLoaderMod.TRUCK_HORN_LOOP, SoundSource.NEUTRAL, RandomSource.create());
            this.loader = loader;
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            follow();
        }

        @Override
        public void tick() {
            if (loader.isRemoved() || !loader.isHornHeld()) {
                stop();
                return;
            }
            follow();
        }

        private void follow() {
            this.x = loader.getX();
            this.y = loader.getY() + 1.5D;
            this.z = loader.getZ();
        }

        private void stopNow() {
            stop();
        }
    }
}
