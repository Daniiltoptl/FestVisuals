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

@ModuleRegister(name = "Auto Invisible", desc = "Автоматически пьёт зелье невидимости", category = Category.OTHER)
public class AutoInvisibleModule extends Module {
    @Getter private static final AutoInvisibleModule instance = new AutoInvisibleModule();

    private boolean drinking;
    private int returnSlot = -1;
    private int cooldown;

    @Override
    public void onEvent() {
        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> tick())));
    }

    private void tick() {
        if (mc.player == null || mc.gameMode == null) return;

        // Drinking takes 32 ticks. The slot must stay selected for all of them, so the swap back
        // waits until the player has actually stopped using the item.
        if (drinking) {
            if (mc.player.isUsingItem()) return;

            if (returnSlot >= 0) {
                mc.player.getInventory().setSelectedSlot(returnSlot);
                returnSlot = -1;
            }
            drinking = false;
            cooldown = 10;
            return;
        }

        if (cooldown-- > 0) return;
        if (mc.player.hasEffect(MobEffects.INVISIBILITY)) return;
        if (mc.player.isUsingItem() || mc.gui.screen() != null) return;

        int slot = findInvisibilitySlot();
        if (slot == -1) return;

        returnSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        drinking = true;
    }

    @Override
    public void onDisable() {
        if (drinking && mc.player != null && returnSlot >= 0) {
            mc.player.getInventory().setSelectedSlot(returnSlot);
        }
        drinking = false;
        returnSlot = -1;
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
