package flynnz.dreaming.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class WandItem extends Item {
    public WandItem(Properties settings) {
        super(settings);
    }

    public @NonNull InteractionResult use(final @NonNull Level level, final @NonNull Player player, @NonNull InteractionHand hand) {
        if (!level.isClientSide()) {
            Vec3 direction = player.getViewVector(1.0f);

            var fireball = new SmallFireball(level, player, direction);
            Vec3 spawn = player.getEyePosition().add(direction.scale(1.5));

            fireball.setPos(spawn);
            fireball.setDeltaMovement(direction.scale(1.5));

            level.addFreshEntity(fireball);
        }

        return InteractionResult.SUCCESS;
    }
}
