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
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Wizard extends PathfinderMob implements NeutralMob, RangedAttackMob {
    private long persistentAngerEndTime = 20 * 30;
    private EntityReference<LivingEntity> persistentAngerTarget;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private static final EntityDataAccessor<Integer> DATA_ARM_POSE =
            SynchedEntityData.defineId(Wizard.class, EntityDataSerializers.INT);

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
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new RangedAttackGoal(this, 1.25, 20, 10.0F));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, (target, _) -> target instanceof AbstractIllager));
        targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    protected void populateDefaultEquipmentSlots(final @NonNull RandomSource random, final @NonNull DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DreamingItems.WAND_ITEM));
    }

    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
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
        if (!this.level().isClientSide()) {
            updateArmPose();
        }
    }

    private void updateArmPose() {
        WizardArmPose desired;
        if (this.isAngry()) {
            desired = WizardArmPose.NEUTRAL;
        } else {
            desired = WizardArmPose.CROSSED;
        }
        if (getArmPose() != desired) {
            setArmPose(desired);
        }
        Dreaming.LOGGER.debug("State changed to: {}", desired);
    }

    @Override
    public boolean canAttack(final LivingEntity target) {
        return target.is(EntityTypes.CREEPER) ? false : super.canAttack(target);
    }

    public void setArmPose(WizardArmPose pose) {
        this.entityData.set(DATA_ARM_POSE, pose.ordinal());
    }

    public WizardArmPose getArmPose() {
        return WizardArmPose.values()[entityData.get(DATA_ARM_POSE)];
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
    public void performRangedAttack(LivingEntity target, float power) {
        this.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public void die(final DamageSource source) {
        super.die(source);
    }

    public enum WizardArmPose {
        CROSSED,
        ATTACKING,
        SPELLCASTING,
        NEUTRAL;
    }
}
