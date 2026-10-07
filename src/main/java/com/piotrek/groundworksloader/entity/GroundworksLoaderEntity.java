package com.piotrek.groundworksloader.entity;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.bucket.LoaderBucketController;
import com.piotrek.groundworksloader.bucket.LoaderBucketController.BucketTickResult;
import com.piotrek.groundworksloader.integration.groundworks.GroundworksLoaderAdapter;
import com.piotrek.groundworksloader.vehicle.EngineSoundProfile;
import com.piotrek.groundworksloader.vehicle.ExhaustPuffs;
import com.piotrek.groundworksloader.vehicle.LoaderExhaustTransform;
import com.piotrek.groundworksloader.vehicle.LoaderMovementController;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Server-authoritative 4-wheel industrial wheel loader vehicle entity.
 */
public class GroundworksLoaderEntity extends Entity {

    // ── Synched Entity Data ───────────────────────────────────────────
    private static final EntityDataAccessor<Float> BOOM_ANGLE =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BUCKET_ANGLE =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STEER_ANGLE =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FORWARD_SPEED =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WHEEL_ROTATION =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CARRIED_MATERIAL_ID =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CARRIED_UNITS =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VEHICLE_PITCH =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VEHICLE_ROLL =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_SCOOPING =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_DUMPING =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENGINE_RUNNING =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.BOOLEAN);
    // Append new synced fields after the legacy loader schema to preserve accessor IDs.
    private static final EntityDataAccessor<Boolean> HORN_HELD =
            SynchedEntityData.defineId(GroundworksLoaderEntity.class, EntityDataSerializers.BOOLEAN);

    // ── Subsystems ───────────────────────────────────────────────────
    private final LoaderMovementController movementController = new LoaderMovementController();
    private final LoaderBucketController bucketController = new LoaderBucketController();

    // Operator input buffer
    private float inputThrottle;
    private float inputSteer;
    private float inputBoomLift;
    private float inputBucketTilt;
    private int inputFreshTicks;

    public GroundworksLoaderEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BOOM_ANGLE, 0.0F);
        builder.define(BUCKET_ANGLE, 0.0F);
        builder.define(STEER_ANGLE, 0.0F);
        builder.define(FORWARD_SPEED, 0.0F);
        builder.define(WHEEL_ROTATION, 0.0F);
        builder.define(CARRIED_MATERIAL_ID, 0);
        builder.define(CARRIED_UNITS, 0);
        builder.define(VEHICLE_PITCH, 0.0F);
        builder.define(VEHICLE_ROLL, 0.0F);
        builder.define(IS_SCOOPING, false);
        builder.define(IS_DUMPING, false);
        builder.define(ENGINE_RUNNING, false);
        builder.define(HORN_HELD, false);
    }

    public void setControlInputs(float throttle, float steer, float boomLift, float bucketTilt) {
        this.inputThrottle = Mth.clamp(throttle, -1.0F, 1.0F);
        this.inputSteer = Mth.clamp(steer, -1.0F, 1.0F);
        this.inputBoomLift = Mth.clamp(boomLift, -1.0F, 1.0F);
        this.inputBucketTilt = Mth.clamp(bucketTilt, -1.0F, 1.0F);
        this.inputFreshTicks = 5;
    }

    public void setHornInput(boolean hornActive) {
        this.entityData.set(HORN_HELD, hornActive && this.getControllingPassenger() != null);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            spawnExhaustParticles();
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();

        // 1. Driver status & input processing (custom packet + vanilla input fallback)
        Entity driver = this.getControllingPassenger();
        if (driver instanceof ServerPlayer player) {
            var vanillaInput = player.getLastClientInput();
            if (this.inputFreshTicks > 0) {
                this.inputFreshTicks--;
            } else {
                this.inputThrottle = vanillaInput.forward() ? 1.0F : vanillaInput.backward() ? -1.0F : 0.0F;
                this.inputSteer = vanillaInput.left() ? -1.0F : vanillaInput.right() ? 1.0F : 0.0F;
                this.inputBoomLift = 0.0F;
                this.inputBucketTilt = 0.0F;
            }

            // Allow vanilla forward/backward and steering override if packet values are neutral
            if (Math.abs(this.inputThrottle) < 0.01F) {
                if (vanillaInput.forward()) this.inputThrottle = 1.0F;
                else if (vanillaInput.backward()) this.inputThrottle = -1.0F;
            }
            if (Math.abs(this.inputSteer) < 0.01F) {
                if (vanillaInput.left()) this.inputSteer = -1.0F;
                else if (vanillaInput.right()) this.inputSteer = 1.0F;
            }
        } else {
            this.inputThrottle = 0.0F;
            this.inputSteer = 0.0F;
            this.inputBoomLift = 0.0F;
            this.inputBucketTilt = 0.0F;
            this.inputFreshTicks = 0;
        }

        boolean hasDriver = driver != null;
        this.entityData.set(ENGINE_RUNNING, hasDriver);
        if (!hasDriver) {
            this.entityData.set(HORN_HELD, false);
        }

        // 2. Physics & Motion simulation
        LoaderMovementController.StepResult moveRes =
                this.movementController.step(this.inputThrottle, this.inputSteer);

        this.bucketController.updateAngles(this.inputBoomLift, this.inputBucketTilt);

        // Yaw heading update
        if (Math.abs(moveRes.deltaYaw()) > 0.001F) {
            this.setYRot(Mth.wrapDegrees(this.getYRot() + moveRes.deltaYaw()));
            this.setYHeadRot(this.getYRot());
            this.setYBodyRot(this.getYRot());
        }

        // Update 2-axle terrain suspension orientation (pitch & roll)
        this.movementController.updateTerrainOrientation(
                serverLevel,
                this.position(),
                this.getYRot()
        );

        double targetGroundY = this.movementController.getAverageGroundY(
                serverLevel,
                this.position(),
                this.getYRot()
        );

        // Compute translation
        float currentSpeed = moveRes.forwardSpeed();
        float yawRad = (float) Math.toRadians(this.getYRot());
        double dx = -Math.sin(yawRad) * currentSpeed;
        double dz = Math.cos(yawRad) * currentSpeed;

        // Smooth vertical climbing with front axle / gravity
        double heightDiff = targetGroundY - this.getY();
        double dy;
        if (heightDiff > 0.03D) {
            dy = Math.min(heightDiff, Math.max(0.08D, Math.abs(currentSpeed) * 0.70D));
        } else if (heightDiff < -0.05D) {
            dy = Math.max(heightDiff, this.onGround() ? -0.25D : -0.08D);
        } else {
            dy = this.onGround() ? 0.0D : -0.08D;
        }

        this.move(MoverType.SELF, new Vec3(dx, dy, dz));

        // 3. Groundworks Excavation & Dumping simulation
        GroundworksLoaderAdapter adapter = GroundworksLoaderAdapter.of(serverLevel, this);
        BucketTickResult bucketRes = this.bucketController.tick(
                adapter,
                this.position(),
                this.getYRot(),
                this.movementController.vehiclePitch(),
                currentSpeed
        );

        // Particle and sound effects on server
        if (bucketRes.isScooping() && (this.tickCount % 2 == 0)) {
            spawnGranularParticles(serverLevel, bucketRes.lipWorldCenter(), bucketRes.carriedMaterialAfter());
            if (this.tickCount % 6 == 0) {
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F);
            }
        }

        if (bucketRes.isDumping() && (this.tickCount % 2 == 0)) {
            spawnGranularParticles(serverLevel, bucketRes.lipWorldCenter(), bucketRes.carriedMaterialAfter());
            if (this.tickCount % 5 == 0) {
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
        }

        // 4. Synchronize data to clients
        this.entityData.set(BOOM_ANGLE, this.bucketController.boomAngle());
        this.entityData.set(BUCKET_ANGLE, this.bucketController.bucketAngle());
        this.entityData.set(STEER_ANGLE, moveRes.steerAngle());
        this.entityData.set(FORWARD_SPEED, currentSpeed);
        this.entityData.set(WHEEL_ROTATION, moveRes.wheelRotation());
        this.entityData.set(CARRIED_UNITS, bucketRes.carriedUnitsAfter());
        this.entityData.set(CARRIED_MATERIAL_ID, bucketRes.carriedMaterialAfter().id());
        this.entityData.set(VEHICLE_PITCH, this.movementController.vehiclePitch());
        this.entityData.set(VEHICLE_ROLL, this.movementController.vehicleRoll());
        this.entityData.set(IS_SCOOPING, bucketRes.isScooping());
        this.entityData.set(IS_DUMPING, bucketRes.isDumping());
    }

    private void spawnExhaustParticles() {
        if (!this.isEngineRunning()) {
            return;
        }

        boolean hydraulicActive = this.isScooping() || this.isDumping();
        float load = Math.max(0.08F, EngineSoundProfile.machineLoad(this.getForwardSpeed(), hydraulicActive));
        Vec3 exhaustPos = LoaderExhaustTransform.getExhaustWorldPosition(
                this.position(),
                this.getYRot(),
                this.getVehiclePitch(),
                this.getVehicleRoll()
        );

        int whitePuffs = ExhaustPuffs.whitePuffCount(load, this.tickCount);
        if (whitePuffs > 0) {
            float blend = (load - ExhaustPuffs.MIN_LOAD) / (ExhaustPuffs.MAX_LOAD - ExhaustPuffs.MIN_LOAD);
            double spread = 0.004D + 0.010D * blend;
            double rise = 0.010D + 0.012D * blend;
            for (int i = 0; i < whitePuffs; i++) {
                this.level().addParticle(
                        ParticleTypes.WHITE_SMOKE,
                        exhaustPos.x,
                        exhaustPos.y + 0.08D,
                        exhaustPos.z,
                        (Math.random() - 0.5D) * spread,
                        rise + Math.random() * 0.008D,
                        (Math.random() - 0.5D) * spread
                );
            }
        }

        if (load > 0.30F) {
            if (load >= 0.95F) {
                this.level().addParticle(
                        ParticleTypes.LARGE_SMOKE,
                        exhaustPos.x, exhaustPos.y + 0.05D, exhaustPos.z,
                        (Math.random() - 0.5D) * 0.03D,
                        0.08D + Math.random() * 0.04D,
                        (Math.random() - 0.5D) * 0.03D
                );
                this.level().addParticle(
                        ParticleTypes.SMOKE,
                        exhaustPos.x, exhaustPos.y + 0.05D, exhaustPos.z,
                        (Math.random() - 0.5D) * 0.02D,
                        0.06D,
                        (Math.random() - 0.5D) * 0.02D
                );
            } else if (load > 0.60F) {
                if (this.tickCount % 2 == 0) {
                    this.level().addParticle(
                            ParticleTypes.SMOKE,
                            exhaustPos.x, exhaustPos.y + 0.05D, exhaustPos.z,
                            (Math.random() - 0.5D) * 0.02D,
                            0.05D + Math.random() * 0.02D,
                            (Math.random() - 0.5D) * 0.02D
                    );
                }
            } else if (this.tickCount % 4 == 0) {
                this.level().addParticle(
                        ParticleTypes.WHITE_SMOKE,
                        exhaustPos.x, exhaustPos.y + 0.05D, exhaustPos.z,
                        (Math.random() - 0.5D) * 0.01D,
                        0.04D,
                        (Math.random() - 0.5D) * 0.01D
                );
            }
        }
    }

    private void spawnGranularParticles(ServerLevel serverLevel, Vec3 pos, GranularMaterial material) {
        BlockParticleOption particle = new BlockParticleOption(
                ParticleTypes.BLOCK,
                material != null && material.id() != 0 ? Blocks.DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState()
        );
        serverLevel.sendParticles(
                particle,
                pos.x, pos.y, pos.z,
                4,
                0.35D, 0.15D, 0.35D,
                0.05D
        );
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        // Shift + Right-Click with empty hand: retrieve / dismantle loader into inventory
        if (player.isSecondaryUseActive() && player.getItemInHand(hand).isEmpty()) {
            if (!this.level().isClientSide() && this.getPassengers().isEmpty()) {
                if (!player.getAbilities().instabuild) {
                    player.getInventory().add(new ItemStack(GroundworksLoaderMod.LOADER_ITEM));
                }
                this.level().playSound(
                        null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F
                );
                this.discard();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }

        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide()) {
            if (this.getPassengers().isEmpty()) {
                player.startRiding(this);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return this.position().add(this.getPassengerAttachmentPoint(passenger, this.getDimensions(this.getPose()), 1.0F));
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        // Inside cabin: slightly behind center, 1.45m elevated
        return forward.scale(-0.10D).add(up.scale(1.45D));
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 left = new Vec3(-Math.cos(yawRad), 0.0D, -Math.sin(yawRad));
        // Safe exit next to side boarding ladder
        return this.position().add(left.scale(2.4D)).add(0.0D, 0.20D, 0.0D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isInvulnerableToBase(source)) {
            return false;
        }

        level.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ANVIL_HIT, SoundSource.PLAYERS, 0.8F, 1.1F
        );

        if (source.getEntity() instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                this.spawnAtLocation(level, GroundworksLoaderMod.LOADER_ITEM);
            }
            this.discard();
            return true;
        }

        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        LoaderState state = LoaderState.load(input);
        this.bucketController.setBoomAngle(state.boomAngle());
        this.bucketController.setBucketAngle(state.bucketAngle());
        this.bucketController.setCarriedUnits(state.carriedUnits());
        this.bucketController.setCarriedMaterial(GranularMaterialRegistry.byId(state.carriedMaterialId()));

        this.movementController.setForwardSpeed(state.forwardSpeed());
        this.movementController.setSteerAngle(state.steerAngle());
        this.movementController.setWheelRotation(state.wheelRotation());
        this.movementController.setOrientation(state.vehiclePitch(), state.vehicleRoll());

        this.entityData.set(BOOM_ANGLE, state.boomAngle());
        this.entityData.set(BUCKET_ANGLE, state.bucketAngle());
        this.entityData.set(STEER_ANGLE, state.steerAngle());
        this.entityData.set(FORWARD_SPEED, state.forwardSpeed());
        this.entityData.set(WHEEL_ROTATION, state.wheelRotation());
        this.entityData.set(CARRIED_UNITS, state.carriedUnits());
        this.entityData.set(CARRIED_MATERIAL_ID, state.carriedMaterialId());
        this.entityData.set(VEHICLE_PITCH, state.vehiclePitch());
        this.entityData.set(VEHICLE_ROLL, state.vehicleRoll());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        LoaderState state = new LoaderState(
                this.bucketController.boomAngle(),
                this.bucketController.bucketAngle(),
                this.movementController.steerAngle(),
                this.movementController.forwardSpeed(),
                this.movementController.wheelRotation(),
                this.bucketController.carriedUnits(),
                this.bucketController.carriedMaterial() != null ? this.bucketController.carriedMaterial().id() : 0,
                this.movementController.vehiclePitch(),
                this.movementController.vehicleRoll()
        );
        state.save(output);
    }

    // ── Getters for Render & Telemetry ───────────────────────────────
    public float getBoomAngle() {
        return this.entityData.get(BOOM_ANGLE);
    }

    public float getBucketAngle() {
        return this.entityData.get(BUCKET_ANGLE);
    }

    public float getSteerAngle() {
        return this.entityData.get(STEER_ANGLE);
    }

    public float getForwardSpeed() {
        return this.entityData.get(FORWARD_SPEED);
    }

    public float getWheelRotation() {
        return this.entityData.get(WHEEL_ROTATION);
    }

    public int getCarriedUnits() {
        return this.entityData.get(CARRIED_UNITS);
    }

    public int getCarriedMaterialId() {
        return this.entityData.get(CARRIED_MATERIAL_ID);
    }

    public GranularMaterial getCarriedMaterial() {
        return GranularMaterialRegistry.byId(getCarriedMaterialId());
    }

    public float getVehiclePitch() {
        return this.entityData.get(VEHICLE_PITCH);
    }

    public float getVehicleRoll() {
        return this.entityData.get(VEHICLE_ROLL);
    }

    public boolean isScooping() {
        return this.entityData.get(IS_SCOOPING);
    }

    public boolean isDumping() {
        return this.entityData.get(IS_DUMPING);
    }

    public boolean isEngineRunning() {
        return this.entityData.get(ENGINE_RUNNING);
    }

    public boolean isHornHeld() {
        return this.entityData.get(HORN_HELD);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return other != null && !this.hasPassenger(other);
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    protected boolean isLocalClientAuthoritative() {
        return false;
    }

    @Override
    public float maxUpStep() {
        return 0.35F;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof LivingEntity && this.getPassengers().isEmpty();
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof LivingEntity living ? living : null;
    }

    public boolean isDriver(Entity entity) {
        return entity != null && entity == this.getControllingPassenger();
    }
}
