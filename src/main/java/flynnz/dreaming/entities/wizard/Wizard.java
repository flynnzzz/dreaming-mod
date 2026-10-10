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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Wizard extends PathfinderMob implements NeutralMob, RangedAttackMob {
    private final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private final int MAX_CHARGING_TIME = 20 * 3;
    private long persistentAngerEndTime = 0L;

    private static final EntityDataAccessor<Integer> DATA_ARM_POSE = SynchedEntityData.defineId(Wizard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGING_TIME = SynchedEntityData.defineId(Wizard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_CASTING_SPELL = SynchedEntityData.defineId(Wizard.class, EntityDataSerializers.BOOLEAN);
    private EntityReference<LivingEntity> persistentAngerTarget;
    private boolean shouldShootLargeFireball = false;


    public Wizard(Level level) {
        super(DreamingEntityTypes.WIZARD, level);
    }
    public Wizard(EntityType<? extends Wizard> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createWizardAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.TEMPT_RANGE, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NonNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ARM_POSE, WizardArmPose.CROSSED.ordinal());
        builder.define(CHARGING_TIME, MAX_CHARGING_TIME);
        builder.define(IS_CASTING_SPELL, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new RangedAttackGoal(this, 1.25, 20, 10.0F));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
        targetSelector.addGoal(
                1, new NearestAttackableTargetGoal<>(
                        this, Mob.class, 10, true, false,
                        (target, _) -> target instanceof Raider)
        );
        targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
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
    public boolean hurtServer(final @NonNull ServerLevel level, final @NonNull DamageSource source, final float damage) {
        boolean wasHurt = super.hurtServer(level, source, damage);
        if (wasHurt && source.getEntity() instanceof LivingEntity attacker) {
            this.setPersistentAngerTarget(EntityReference.of(attacker));
            this.startPersistentAngerTimer();
            this.playSound(SoundEvents.VILLAGER_HURT, 1.0F, 1.0F);
        }

        return wasHurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel serverLevel) {
            this.updateArmPose();
            this.updatePersistentAnger(serverLevel, true);
            if (this.isAngry()) {
                tickChargeTime();
            }
        }
    }

    @Override
    public void performRangedAttack(final @NonNull LivingEntity target, final float power) {
        Dreaming.LOGGER.debug("Item in main hand: {}", this.getItemBySlot(EquipmentSlot.MAINHAND));
        final double xd = target.getX() - this.getX();
        final double yd = target.getY() - this.getY();
        final double zd = target.getZ() - this.getZ();
        final var direction = new Vec3(xd, yd, zd);

        if (this.level() instanceof ServerLevel) {
            if (!this.shouldShootLargeFireball) {
                final var randomPick = this.getRandom().nextInt(6);
                this.shouldShootLargeFireball = randomPick > 3;
                final var fireball = new SmallFireball(level(), this, direction);
                fireball.addDeltaMovement(direction.scale(0.3f));
                this.shootFireBall(fireball, direction);
            }
            else {
                if (this.getChargingTime() == 0) {
                    this.setIsCastingSpell(false);
                    this.resetChargingTime();
                    this.shootFireBall(new LargeFireball(level(), this, direction, 1), direction);
                    this.shouldShootLargeFireball = false;
                }
                else {
                    this.setIsCastingSpell(true);
                }
            }
        }
    }

    private void shootFireBall(Fireball fireball, Vec3 position) {
        this.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        fireball.setPos(
                this.getX() + position.x * 0.5,
                this.getEyeY() + position.y * 0.5,
                this.getZ() + position.z * 0.5
        );
        level().addFreshEntity(fireball);
    }

    private void updateArmPose() {
        WizardArmPose desired;
        if (this.isAngry()) {
            desired = isCastingSpell() ? WizardArmPose.SPELLCASTING : WizardArmPose.NEUTRAL;
        } else {
            this.setIsCastingSpell(false);
            this.resetChargingTime();
            desired = WizardArmPose.CROSSED;
        }
        if (getArmPose() != desired) {
            setArmPose(desired);
        }
    }

    public void setIsCastingSpell(boolean isCastingSpell) {
        this.entityData.set(IS_CASTING_SPELL, isCastingSpell);
    }

    public boolean isCastingSpell() {
        return this.entityData.get(IS_CASTING_SPELL);
    }

    public void tickChargeTime() {
        final int oldChargingTime = this.getChargingTime();
        if (oldChargingTime != 0 && shouldShootLargeFireball) {
            this.entityData.set(CHARGING_TIME, oldChargingTime - 1);
        }
    }

    public void resetChargingTime() {
        this.entityData.set(CHARGING_TIME, MAX_CHARGING_TIME);
    }

    public int getChargingTime() {
        return this.entityData.get(CHARGING_TIME);
    }

    public void setArmPose(WizardArmPose pose) {
        this.entityData.set(DATA_ARM_POSE, pose.ordinal());
    }

    public WizardArmPose getArmPose() {
        return WizardArmPose.values()[entityData.get(DATA_ARM_POSE)];
    }

    @Override
    public boolean canAttack(final LivingEntity target) {
        return target.is(EntityTypes.CREEPER) ? false : super.canAttack(target);
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

    public enum WizardArmPose {
        CROSSED,
        SPELLCASTING,
        NEUTRAL;
    }
}
