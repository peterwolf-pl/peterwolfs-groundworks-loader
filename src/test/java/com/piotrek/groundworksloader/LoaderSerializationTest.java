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

    @Test
    @DisplayName("LoaderState accurately saves and loads mixed material compositions")
    void testMixedMaterialStatePersistence() {
        int[] materialUnits = new int[GranularMaterialRegistry.count() + 1];
        materialUnits[GranularMaterialRegistry.DIRT.id()] = 250;
        materialUnits[GranularMaterialRegistry.SAND.id()] = 180;
        materialUnits[GranularMaterialRegistry.GRAVEL.id()] = 90;

        LoaderState original = new LoaderState(
                10.0F,
                -5.0F,
                0.0F,
                0.1F,
                50.0F,
                520,
                GranularMaterialRegistry.DIRT.id(),
                0.0F,
                0.0F,
                materialUnits
        );

        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        original.save(output);
        CompoundTag tag = output.buildResult();

        HolderLookup.Provider lookup = HolderLookup.Provider.create(Stream.empty());
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag);
        LoaderState loaded = LoaderState.load(input);

        assertEquals(520, loaded.carriedUnits());
        assertEquals(GranularMaterialRegistry.DIRT.id(), loaded.carriedMaterialId());
        assertEquals(250, loaded.materialUnits()[GranularMaterialRegistry.DIRT.id()]);
        assertEquals(180, loaded.materialUnits()[GranularMaterialRegistry.SAND.id()]);
        assertEquals(90, loaded.materialUnits()[GranularMaterialRegistry.GRAVEL.id()]);
    }
}
