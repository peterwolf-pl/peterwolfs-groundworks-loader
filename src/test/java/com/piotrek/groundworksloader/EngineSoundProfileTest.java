package com.piotrek.groundworksloader;

import com.piotrek.groundworksloader.vehicle.EngineSoundProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineSoundProfileTest {

    @Test
    @DisplayName("Engine sound profile modulates volume and pitch with speed and hydraulics")
    void testEngineSoundMix() {
        EngineSoundProfile.Mix idle = EngineSoundProfile.forLoaderState(0.0F, false);
        EngineSoundProfile.Mix moving = EngineSoundProfile.forLoaderState(0.20F, false);
        EngineSoundProfile.Mix hydraulic = EngineSoundProfile.forLoaderState(0.0F, true);

        assertTrue(moving.volume() > idle.volume());
        assertTrue(moving.pitch() > idle.pitch());

        assertTrue(hydraulic.volume() > idle.volume());
        assertTrue(hydraulic.pitch() > idle.pitch());
    }
}
