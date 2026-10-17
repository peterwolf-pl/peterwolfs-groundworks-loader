package com.piotrek.groundworksloader.entity;

import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Encapsulated serialization record for wheel loader persistence.
 */
public record LoaderState(
        float boomAngle,
        float bucketAngle,
        float steerAngle,
        float forwardSpeed,
        float wheelRotation,
        int carriedUnits,
        int carriedMaterialId,
        float vehiclePitch,
        float vehicleRoll,
        int[] materialUnits
) {

    public LoaderState(
            float boomAngle,
            float bucketAngle,
            float steerAngle,
            float forwardSpeed,
            float wheelRotation,
            int carriedUnits,
            int carriedMaterialId,
            float vehiclePitch,
            float vehicleRoll
    ) {
        this(
                boomAngle,
                bucketAngle,
                steerAngle,
                forwardSpeed,
                wheelRotation,
                carriedUnits,
                carriedMaterialId,
                vehiclePitch,
                vehicleRoll,
                createLegacyUnits(carriedUnits, carriedMaterialId)
        );
    }

    private static int[] createLegacyUnits(int carriedUnits, int carriedMaterialId) {
        int count = Math.max(GranularMaterialRegistry.count(), carriedMaterialId + 1);
        int[] units = new int[count];
        if (carriedUnits > 0 && carriedMaterialId > 0 && carriedMaterialId < units.length) {
            units[carriedMaterialId] = carriedUnits;
        }
        return units;
    }

    public void save(ValueOutput output) {
        output.putFloat("BoomAngle", boomAngle);
        output.putFloat("BucketAngle", bucketAngle);
        output.putFloat("SteerAngle", steerAngle);
        output.putFloat("ForwardSpeed", forwardSpeed);
        output.putFloat("WheelRotation", wheelRotation);
        output.putInt("CarriedUnits", carriedUnits);
        output.putInt("CarriedMaterialId", carriedMaterialId);
        output.putFloat("VehiclePitch", vehiclePitch);
        output.putFloat("VehicleRoll", vehicleRoll);

        int[] units = materialUnits != null ? materialUnits : new int[0];
        for (int id = 1; id < units.length; id++) {
            if (units[id] > 0) {
                output.putInt("LoadMaterial_" + id, units[id]);
            }
        }
    }

    public static LoaderState load(ValueInput input) {
        int count = Math.max(GranularMaterialRegistry.count(), 1);
        int[] units = new int[count];
        for (int id = 1; id < count; id++) {
            units[id] = Math.max(0, input.getIntOr("LoadMaterial_" + id, 0));
        }

        int legacyUnits = input.getIntOr("CarriedUnits", 0);
        int legacyMaterialId = input.getIntOr("CarriedMaterialId", 0);

        boolean hasSpecific = false;
        for (int u : units) {
            if (u > 0) {
                hasSpecific = true;
                break;
            }
        }
        if (!hasSpecific && legacyUnits > 0 && legacyMaterialId > 0 && legacyMaterialId < units.length) {
            units[legacyMaterialId] = legacyUnits;
        }

        int totalUnits = 0;
        int dominantId = legacyMaterialId;
        int maxU = 0;
        for (int id = 1; id < units.length; id++) {
            totalUnits += units[id];
            if (units[id] > maxU) {
                maxU = units[id];
                dominantId = id;
            }
        }
        if (totalUnits == 0 && legacyUnits > 0) {
            totalUnits = legacyUnits;
        }

        return new LoaderState(
                input.getFloatOr("BoomAngle", 0.0F),
                input.getFloatOr("BucketAngle", 0.0F),
                input.getFloatOr("SteerAngle", 0.0F),
                input.getFloatOr("ForwardSpeed", 0.0F),
                input.getFloatOr("WheelRotation", 0.0F),
                totalUnits,
                dominantId,
                input.getFloatOr("VehiclePitch", 0.0F),
                input.getFloatOr("VehicleRoll", 0.0F),
                units
        );
    }
}
