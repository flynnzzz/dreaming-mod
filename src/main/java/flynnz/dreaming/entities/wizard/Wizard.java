package flynnz.dreaming.entities.wizard;

import flynnz.dreaming.entities.DreamingEntityTypes;
import flynnz.dreaming.items.DreamingItems;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

public class Wizard extends PathfinderMob {
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
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new TemptGoal(this, 1, Ingredient.of(Items.EMERALD), false));
        goalSelector.addGoal(1, new RandomStrollGoal(this, 1));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 4));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void populateDefaultEquipmentSlots(final @NonNull RandomSource random, final @NonNull DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DreamingItems.WAND_ITEM));
    }
}
