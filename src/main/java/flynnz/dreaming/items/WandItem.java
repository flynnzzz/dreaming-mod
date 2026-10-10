package flynnz.dreaming.items;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

public class WandItem extends Item {
    public static final int MAX_CHARGE_DURATION = 20;
    public static final int MAX_CLICK_DURATION = 5;
    public static final int MIN_POWER_LEVEL = 1;
    public static final int MAX_POWER_LEVEL = 3;

    public WandItem(final Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(final @NonNull ItemStack itemStack, final @NonNull LivingEntity user) {
        return 72000;
    }

    @Override
    public @NonNull ItemUseAnimation getUseAnimation(final @NonNull ItemStack itemStack) {
        return ItemUseAnimation.SPYGLASS;
    }

    public @NonNull InteractionResult use(final @NonNull Level level, final @NonNull Player player, final @NonNull InteractionHand hand) {
        if (!level.isClientSide()) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    public static float getPowerForTime(final int timeHeld) {
        float pow = timeHeld / (float) MAX_CHARGE_DURATION;
        pow *= MAX_POWER_LEVEL;
        if (pow < MIN_POWER_LEVEL)
            pow = MIN_POWER_LEVEL;
        if (pow > MAX_POWER_LEVEL)
            pow = MAX_POWER_LEVEL;
        return pow;
    }

    @Override
    public boolean releaseUsing(
            final @NonNull ItemStack itemStack, final @NonNull Level level, final @NonNull LivingEntity entity, final int remainingTime
    ) {
        if (entity instanceof Player player) {

            int timeHeld = this.getUseDuration(itemStack, entity) - remainingTime;
            float pow = getPowerForTime(timeHeld);

            if (level instanceof ServerLevel serverLevel) {

                var fireball = timeHeld > MAX_CLICK_DURATION ?
                        createLargeFireball(level, player, (int) pow) :
                        createSmallFireball(level, player);

                var spellChargeSound = timeHeld < MAX_CLICK_DURATION ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_ACTIVATE;
                level.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        spellChargeSound,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + pow * 0.5F
                );

                shootFireball(serverLevel, fireball, player);
                itemStack.hurtAndBreak(1, player, player.getUsedItemHand());
            }

            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + pow * 0.5F
            );
            player.awardStat(Stats.ITEM_USED.get(this));
            return true;
        } else {
            return false;
        }
    }

    private SmallFireball createSmallFireball(final Level level, final LivingEntity shooter) {
        var direction = shooter.getViewVector(1.0f);
        var fireball = new SmallFireball(level, shooter, direction);
        fireball.addDeltaMovement(direction);
        return fireball;
    }

    private LargeFireball createLargeFireball(final Level level, final LivingEntity shooter, final int explosionPower) {
        var direction = shooter.getViewVector(1.0f).scale(2 * explosionPower);
        return new LargeFireball(level, shooter, direction, explosionPower);
    }

    private void shootFireball(final Level level, final Fireball fireball, final LivingEntity shooter) {
        var look = shooter.getViewVector(1.0f);
        fireball.setPos(
                shooter.getX() + look.x * 0.5,
                shooter.getEyeY() + look.y * 0.5,
                shooter.getZ() + look.z * 0.5
        );
        level.addFreshEntity(fireball);
    }
}
