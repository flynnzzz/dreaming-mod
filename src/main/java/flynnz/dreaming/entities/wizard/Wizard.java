package flynnz.dreaming.entities.wizard;

import flynnz.dreaming.Dreaming;
import flynnz.dreaming.entities.DreamingEntityTypes;
import flynnz.dreaming.items.DreamingItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;

public class Wizard extends AbstractVillager implements NeutralMob, RangedAttackMob {
    private final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    public static final int MAX_CHARGING_TIME = 20 * 2;
    public static final int MAX_DELAY_TIME = 20;
    public static final int MAX_CASTING_TIME = 20 * 2;
    public static final int MAX_SHOOTING_TIME = 20;
    private long persistentAngerEndTime = 0L;
    private boolean isCharged;
    private EntityReference<LivingEntity> persistentAngerTarget;

    private static final EntityDataAccessor<Integer> RENDER_STATE = SynchedEntityData.defineId(Wizard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(Wizard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_DELAY_TIME = SynchedEntityData.defineId(Wizard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGING_TIME = SynchedEntityData.defineId(Wizard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CASTING_TIME = SynchedEntityData.defineId(Wizard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHOOTING_TIME = SynchedEntityData.defineId(Wizard.class, EntityDataSerializers.INT);

    public Wizard(Level level) {
        super(DreamingEntityTypes.WIZARD, level);
    }
    public Wizard(EntityType<? extends Wizard> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createWizardAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 32)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NonNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RENDER_STATE, WizardArmPose.CROSSED.ordinal());
        builder.define(STATE, WizardState.IDLE.ordinal());
        builder.define(CHARGING_TIME, MAX_CHARGING_TIME);
        builder.define(CASTING_TIME, MAX_CASTING_TIME);
        builder.define(SHOOTING_TIME, MAX_SHOOTING_TIME);
        builder.define(ATTACK_DELAY_TIME, MAX_DELAY_TIME);
    }

    @Override
    protected void rewardTradeXp(@Nullable MerchantOffer offer) {

    }

    @Override
    protected void updateTrades(@Nullable ServerLevel level) {

    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new WizardAttackGoal<>(this, 1.25, 20, 40.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>( this, Mob.class, 10, true, false, (target, _) -> target instanceof Raider));
        targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    protected void populateDefaultEquipmentSlots(final @Nullable RandomSource random, final @Nullable DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DreamingItems.WAND_ITEM));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NonNull ServerLevelAccessor level, @NonNull DifficultyInstance difficulty, @NonNull EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        final var data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return data;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean hurtServer(final @NonNull ServerLevel level, final @NonNull DamageSource source, final float damage) {
        boolean wasHurt = super.hurtServer(level, source, damage);
        if (wasHurt) {
            this.playSound(SoundEvents.VILLAGER_HURT, 1.0F, 1.0F);

            if (source.getEntity() instanceof LivingEntity attacker) {
                this.setPersistentAngerTarget(EntityReference.of(attacker));
                this.startPersistentAngerTimer();
            }

            if (this.getHealth() < 10) {
                this.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 5 ));
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
                this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 10, 2));
                this.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 10, 3));
                this.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 10 ));
                setWizardState(WizardState.CASTING_BUFFS);
            }
            else if (this.getHealth() < 5) {
                this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 20 ));
                setWizardState(WizardState.CASTING_BUFFS);
            }
            else if (source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.IN_FIRE)){
                this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 10));
                setWizardState(WizardState.CASTING_BUFFS);
            }
        }

        return wasHurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        switch (getWizardState()) {
            case IDLE -> setWizardState(this.getTarget() != null ? WizardState.ANGRY : WizardState.IDLE);
            case ANGRY -> {
                if (this.getTarget() == null) {
                    setWizardState(WizardState.IDLE);
                    break;
                }
                tickDelayTime();
                if (getDelayTime() == 0) {
                    resetDelayTime();
                    var shouldCharge = this.getRandom().nextInt(6) > 3;
                    setWizardState(shouldCharge ? WizardState.CHARGING_MAGIC : WizardState.SHOOTING_MAGIC);
                }
            }
            case CHARGING_MAGIC -> {
                tickChargeTime();
                if (getChargingTime() == 0) {
                    this.setIsCharged(true);
                    resetChargingTime();
                    setWizardState(WizardState.SHOOTING_MAGIC);
                }
            }
            case CASTING_BUFFS -> {
                tickCastTime();
                if (getCastingTime() == 0) {
                    resetCastingTime();
                    setWizardState(WizardState.ANGRY);
                }
            }
            case SHOOTING_MAGIC -> {
                tickShootingTime();
                if (getShootingTime() == 0) {
                    if (this.getTarget() != null) {
                        performRangedAttack(getTarget(), 0);
                    }
                    resetShootingTIme();
                    setWizardState(WizardState.ANGRY);
                }
            }
            default -> {
                Dreaming.LOGGER.debug("Impossible state: {}", getWizardState());
            }
        }

        this.updatePersistentAnger(serverLevel, true);
        this.updateArmPose();
    }

    @Override
    public void performRangedAttack(final @NonNull LivingEntity target, final float power) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (Objects.requireNonNull(getWizardState()) != WizardState.SHOOTING_MAGIC) {
            return;
        }
        if (getShootingTime() != 0) {
            return;
        }

        // TODO: improve aim
        double xd = target.getX() - this.getX();
        double yd = target.getY() - this.getY();
        double zd = target.getZ() - this.getZ();
        var direction = new Vec3(xd, yd, zd);

        Fireball fireball;
        if (!this.isCharged()) {
            fireball = new SmallFireball(serverLevel, this, direction);
            fireball.addDeltaMovement(direction.scale(0.3f));
        } else {
            fireball = new LargeFireball(serverLevel, this, direction, 1);
            this.setIsCharged(false);
        }
        shootFireBall(fireball, direction);
    }

    private void shootFireBall(final Fireball fireball, final Vec3 position) {
        this.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        fireball.setPos(
                this.getX() + position.x * 0.5,
                this.getEyeY() + position.y * 0.5,
                this.getZ() + position.z * 0.5
        );
        level().addFreshEntity(fireball);
    }

    private void updateArmPose() {
        this.setArmPose(getWizardState().pose());
    }

    private void setWizardState(WizardState state) {
        this.entityData.set(STATE, state.ordinal());
    }

    private WizardState getWizardState() {
        return WizardState.values()[this.entityData.get(STATE)];
    }

    public void tickDelayTime() {
        final int oldDelayTime = this.getDelayTime();
        if (oldDelayTime != 0) {
            this.entityData.set(ATTACK_DELAY_TIME, oldDelayTime - 1);
        }
    }

    public void resetDelayTime() {
        this.entityData.set(ATTACK_DELAY_TIME, MAX_DELAY_TIME);
    }

    public int getDelayTime() {
        return this.entityData.get(ATTACK_DELAY_TIME);
    }

    public void tickCastTime() {
        final int oldCastingTime = this.getCastingTime();
        if (oldCastingTime != 0) {
            this.entityData.set(CASTING_TIME, oldCastingTime - 1);
        }
    }

    public void resetCastingTime() {
        this.entityData.set(CASTING_TIME, MAX_CASTING_TIME);
    }

    public int getCastingTime() {
        return this.entityData.get(CASTING_TIME);
    }

    public void tickChargeTime() {
        final int oldChargingTime = this.getChargingTime();
        if (oldChargingTime != 0) {
            this.entityData.set(CHARGING_TIME, oldChargingTime - 1);
        }
    }

    public void resetChargingTime() {
        this.entityData.set(CHARGING_TIME, MAX_CHARGING_TIME);
    }

    public int getChargingTime() {
        return this.entityData.get(CHARGING_TIME);
    }

    public void tickShootingTime() {
        final int oldShootingTime = this.getShootingTime();
        if (oldShootingTime != 0) {
            this.entityData.set(SHOOTING_TIME, oldShootingTime - 1);
        }
    }

    public void resetShootingTIme() {
        this.entityData.set(SHOOTING_TIME, MAX_SHOOTING_TIME);
    }

    public int getShootingTime() {
        return this.entityData.get(SHOOTING_TIME);
    }

    public void setArmPose(WizardArmPose pose) {
        this.entityData.set(RENDER_STATE, pose.ordinal());
    }

    public WizardArmPose getArmPose() {
        return WizardArmPose.values()[entityData.get(RENDER_STATE)];
    }

    public boolean isCharged() {
        return this.isCharged;
    }

    public void setIsCharged(boolean isCharged) {
        this.isCharged = isCharged;
    }

    @Override
    public long getPersistentAngerEndTime() {
        return persistentAngerEndTime;
    }

    @Override
    public void setPersistentAngerEndTime(long endTime) {
        this.persistentAngerEndTime = endTime;
    }

    @Override
    public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
        return persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> persistentAngerTarget) {
        this.persistentAngerTarget = persistentAngerTarget;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(random));
    }

    @Override
    protected SoundEvent getHurtSound(final @NonNull DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public boolean canAttack(final LivingEntity target) {
        return target.is(EntityTypes.CREEPER) ? false : super.canAttack(target);
    }

    private enum WizardState {
        IDLE(WizardArmPose.CROSSED),
        ANGRY(WizardArmPose.NEUTRAL),
        CASTING_BUFFS(WizardArmPose.SPELLCASTING),
        CHARGING_MAGIC(WizardArmPose.SPELLCASTING),
        SHOOTING_MAGIC(WizardArmPose.SHOOTING);

        private final WizardArmPose pose;
        WizardState(WizardArmPose pose) {
           this.pose = pose;
        }

        public WizardArmPose pose() {
            return this.pose;
        }
    }

    public enum WizardArmPose {
        CROSSED,
        NEUTRAL,
        SPELLCASTING,
        SHOOTING
    }

    private static class WizardAttackGoal<T extends PathfinderMob & RangedAttackMob> extends Goal {
        private final T mob;
        private final double speedModifier;
        private final int attackIntervalMin;
        private final float attackRadiusSqr;
        private int attackTime = -1;
        private int seeTime;
        private boolean strafingClockwise;
        private boolean strafingBackwards;
        private int strafingTime = -1;

        public WizardAttackGoal(final T mob, final double speedModifier, final int attackIntervalMin, final float attackRadius) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.attackIntervalMin = attackIntervalMin;
            this.attackRadiusSqr = attackRadius * attackRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mob.getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            super.start();
        }

        @Override
        public void stop() {
            super.stop();
            this.seeTime = 0;
            this.attackTime = -1;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target != null) {
                double targetDistSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
                boolean hasLineOfSight = this.mob.getSensing().hasLineOfSight(target);
                boolean hadLineOfSight = this.seeTime > 0;
                if (hasLineOfSight != hadLineOfSight) {
                    this.seeTime = 0;
                }

                if (hasLineOfSight) {
                    this.seeTime++;
                } else {
                    this.seeTime--;
                }

                final double attackDistance = 64.0;
                if (targetDistSqr < attackDistance && this.seeTime >= 20) {
                    // TODO: improve
                    double dx = this.mob.getX() - target.getX();
                    double dz = this.mob.getZ() - target.getZ();
                    double length = Math.sqrt(dx * dx + dz * dz);

                    if (length > 0) {
                        double retreatDistance = 6.0;
                        double retreatX = this.mob.getX() + dx / length * retreatDistance;
                        double retreatZ = this.mob.getZ() + dz / length * retreatDistance;
                        this.mob.getNavigation().moveTo(retreatX, this.mob.getY(), retreatZ, this.speedModifier);
                        this.mob.lookAt(target, 30.0f, 30.0f);
                    }
                    this.strafingTime = -1;
                }
                else {
                    this.mob.getNavigation().stop();
                    this.strafingTime++;
                }

                if (this.strafingTime >= 20) {
                    if (this.mob.getRandom().nextFloat() < 0.3) {
                        this.strafingClockwise = !this.strafingClockwise;
                    }

                    if (this.mob.getRandom().nextFloat() < 0.3) {
                        this.strafingBackwards = !this.strafingBackwards;
                    }

                    this.strafingTime = 0;
                }

                if (this.strafingTime > -1) {
                    if (targetDistSqr < this.attackRadiusSqr * 0.25F) {
                        this.strafingBackwards = true;
                    }

                    this.mob.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                    if (this.mob.getControlledVehicle() instanceof Mob vehicle) {
                        vehicle.lookAt(target, 30.0F, 30.0F);
                    }

                    this.mob.lookAt(target, 30.0F, 30.0F);
                } else {
                    this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }

                if (this.mob.isUsingItem()) {
                    if (!hasLineOfSight && this.seeTime < -60) {
                        this.mob.stopUsingItem();
                    } else if (hasLineOfSight) {
                        int pullTime = this.mob.getTicksUsingItem();
                        if (pullTime >= 20) {
                            this.mob.stopUsingItem();
                            this.mob.performRangedAttack(target, BowItem.getPowerForTime(pullTime));
                            this.attackTime = this.attackIntervalMin;
                        }
                    }
                } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
                    this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, DreamingItems.WAND_ITEM));
                }
            }
        }
    }
}
