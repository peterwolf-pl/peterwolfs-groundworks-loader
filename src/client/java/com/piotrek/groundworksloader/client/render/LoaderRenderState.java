package com.piotrek.groundworksloader.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Client rendering state extracted from {@link com.piotrek.groundworksloader.entity.GroundworksLoaderEntity}.
 */
public class LoaderRenderState extends EntityRenderState {

    public float baseYaw;
    public float basePitch;
    public float baseRoll;

    public float boomAngle;
    public float bucketAngle;
    public float steerAngle;
    public float forwardSpeed;
    public float wheelRotation;

    public int carriedMaterialId;
    public int carriedUnits;
    public float fillRatio;

    public boolean isScooping;
    public boolean isDumping;
    public boolean isEngineRunning;

    public float beaconSpin;
    public boolean beaconFlash;
}
