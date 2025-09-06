package com.pepper.faunify.items;

import com.pepper.faunify.entity.projectile.QuillEntity;
import com.pepper.faunify.registry.FaunifyItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

public class BlowpipeItem extends BowItem {

    public BlowpipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity shooter, int timeLeft) {
        if (!level.isClientSide() && shooter instanceof Player player) {
            ItemStack quillStack = findQuillInInventory(player);

            if (!quillStack.isEmpty()) {
                int charge = (int) ((this.getUseDuration(stack) - timeLeft) * 1.5F);
                float power = calculatePowerForTime(charge);

                if (power >= 0.1F) {
                    // Corrected the constructor call
                    QuillEntity quill = new QuillEntity(shooter, level);  // shooter first, then level
                    quill.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, power * 2.0F, 1.0F);
                    quill.setBaseDamage(1.0);

                    if (!player.isCreative()) {
                        stack.hurtAndBreak(1, shooter, (entity) -> entity.broadcastBreakEvent(shooter.getUsedItemHand()));
                    }

                    level.addFreshEntity(quill);
                    level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                            SoundEvents.ARROW_SHOOT, shooter.getSoundSource(), 1.0F,
                            1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F));

                    if (!player.isCreative()) {
                        quillStack.shrink(1);
                    }
                }
            }
        }
    }

    private ItemStack findQuillInInventory(Player player) {
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.is(FaunifyItems.QUILL.get()) && itemStack.getCount() > 0) {
                return itemStack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000 / 3;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return itemStack -> itemStack.is(FaunifyItems.QUILL.get());
    }

    private float calculatePowerForTime(int charge) {
        float f = (float) charge / 10F;
        f = (f * f + f * 2.0F) / 3.0F;
        return f > 1.0F ? 1.0F : f;
    }
}
