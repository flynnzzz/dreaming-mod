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
    public static final int NO_CHARGE_LIMIT = 5;

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
        return pow * 1.5f;
    }

    @Override
    public boolean releaseUsing(
            final @NonNull ItemStack itemStack, final @NonNull Level level, final @NonNull LivingEntity entity, final int remainingTime
    ) {
        if (entity instanceof Player player) {

            int timeHeld = this.getUseDuration(itemStack, entity) - remainingTime;
            float pow = getPowerForTime(timeHeld);

            if (level instanceof ServerLevel serverLevel) {

                var fireball = timeHeld > NO_CHARGE_LIMIT ?
                        createLargeFireball(level, player, (int) pow) :
                        createSmallFireball(level, player);

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

    private SmallFireball createSmallFireball(Level level, LivingEntity shooter) {
        var direction = shooter.getViewVector(1.0f).scale(2);
        return new SmallFireball(level, shooter, direction);
    }

    private LargeFireball createLargeFireball(Level level, LivingEntity shooter, int explosionPower) {
        explosionPower = explosionPower == 0 ? 1 : explosionPower;
        var direction = shooter.getViewVector(1.0f).scale(2 * explosionPower);
        return new LargeFireball(level, shooter, direction, explosionPower);
    }

    private void shootFireball(Level level, Fireball fireball, LivingEntity shooter) {
        var look = shooter.getViewVector(1.0f);
        fireball.setPos(
                shooter.getX() + look.x * 0.5,
                shooter.getEyeY() + look.y * 0.5,
                shooter.getZ() + look.z * 0.5
        );
        level.addFreshEntity(fireball);
    }
}
