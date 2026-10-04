package com.piotrek.groundworksloader.entity;

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
        float vehicleRoll
) {

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
    }

    public static LoaderState load(ValueInput input) {
        return new LoaderState(
                input.getFloatOr("BoomAngle", 0.0F),
                input.getFloatOr("BucketAngle", 0.0F),
                input.getFloatOr("SteerAngle", 0.0F),
                input.getFloatOr("ForwardSpeed", 0.0F),
                input.getFloatOr("WheelRotation", 0.0F),
                input.getIntOr("CarriedUnits", 0),
                input.getIntOr("CarriedMaterialId", 0),
                input.getFloatOr("VehiclePitch", 0.0F),
                input.getFloatOr("VehicleRoll", 0.0F)
        );
    }
}
