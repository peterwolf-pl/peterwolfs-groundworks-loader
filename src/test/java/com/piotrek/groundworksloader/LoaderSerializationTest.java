package com.piotrek.groundworksloader;

import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksloader.entity.LoaderState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoaderSerializationTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("LoaderState accurately saves and loads all physical and operational parameters")
    void testStatePersistence() {
        LoaderState original = new LoaderState(
                24.5F,  // boomAngle
                -15.0F, // bucketAngle
                12.4F,  // steerAngle
                0.18F,  // forwardSpeed
                184.2F, // wheelRotation
                340,    // carriedUnits
                GranularMaterialRegistry.DIRT.id(), // carriedMaterialId (dirt)
                3.2F,   // vehiclePitch
                -1.1F   // vehicleRoll
        );

        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        original.save(output);
        CompoundTag tag = output.buildResult();

        HolderLookup.Provider lookup = HolderLookup.Provider.create(Stream.empty());
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag);
        LoaderState loaded = LoaderState.load(input);

        assertEquals(original.boomAngle(), loaded.boomAngle(), 1e-4F);
        assertEquals(original.bucketAngle(), loaded.bucketAngle(), 1e-4F);
        assertEquals(original.steerAngle(), loaded.steerAngle(), 1e-4F);
        assertEquals(original.forwardSpeed(), loaded.forwardSpeed(), 1e-4F);
        assertEquals(original.wheelRotation(), loaded.wheelRotation(), 1e-4F);
        assertEquals(original.carriedUnits(), loaded.carriedUnits());
        assertEquals(original.carriedMaterialId(), loaded.carriedMaterialId());
        assertEquals(original.vehiclePitch(), loaded.vehiclePitch(), 1e-4F);
        assertEquals(original.vehicleRoll(), loaded.vehicleRoll(), 1e-4F);
    }
}
