package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.utils.player.ItemUseHelper;

/**
 * Switches to an invisibility potion, drinks it, and switches back.
 *
 * <p>Two things made the previous version look like it did nothing. The slot change is only sent
 * to the server on the next client tick, so a use issued in the same tick was applied to the old
 * item; and Minecraft releases the item every tick the use key is not held, which cancelled the
 * drink one tick after it started. Both are handled by {@link ItemUseHelper}.
 */
@ModuleRegister(name = "Auto Invisible", desc = "Автоматически пьёт зелье невидимости", category = Category.OTHER)
public class AutoInvisibleModule extends Module {
    @Getter private static final AutoInvisibleModule instance = new AutoInvisibleModule();

    private final ItemUseHelper use = new ItemUseHelper();
    private int cooldown;

    @Override
    public void onEvent() {
        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> tick())));
    }

    private void tick() {
        if (mc.player == null || mc.gameMode == null) return;

        if (use.isBusy()) {
            if (use.tick()) cooldown = 20;
            return;
        }

        if (cooldown-- > 0) return;
        if (mc.player.hasEffect(MobEffects.INVISIBILITY)) return;
        if (mc.player.isUsingItem() || mc.screen != null) return;

        int slot = findInvisibilitySlot();
        if (slot == -1) return;

        use.start(slot, InteractionHand.MAIN_HAND);
    }

    @Override
    public void onDisable() {
        use.cancel();
        cooldown = 0;
    }

    private int findInvisibilitySlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
            if (potion == null) continue;

            for (MobEffectInstance effect : potion.getAllEffects()) {
                if (effect.is(MobEffects.INVISIBILITY)) return i;
            }
        }
        return -1;
    }
}
