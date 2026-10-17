package com.piotrek.groundworksloader.entity;

import com.piotrek.groundworksloader.bucket.LoaderBucketController;

/**
 * Physical and bucket specifications for each deployable loader size.
 */
public enum LoaderVariant {
    STANDARD(
            LoaderBucketController.BUCKET_CAPACITY,
            LoaderBucketController.BUCKET_WIDTH_METERS,
            1.0F
    ),
    GIANT(
            15 * 512, // Matches the Groundworks dump truck's complete 15 m³ bed.
            4.6F,
            1.65F
    );

    private final int bucketCapacity;
    private final float bucketWidthMeters;
    private final float scale;

    LoaderVariant(int bucketCapacity, float bucketWidthMeters, float scale) {
        this.bucketCapacity = bucketCapacity;
        this.bucketWidthMeters = bucketWidthMeters;
        this.scale = scale;
    }

    public int bucketCapacity() {
        return bucketCapacity;
    }

    public float bucketWidthMeters() {
        return bucketWidthMeters;
    }

    public float scale() {
        return scale;
    }
}
