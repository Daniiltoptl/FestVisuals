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

    @Override
    public void onEvent() {
        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> tick())));
    }

    private void tick() {
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.player.hasEffect(MobEffects.INVISIBILITY)) return;
        if (mc.player.isUsingItem()) return;

        int slot = findInvisibilitySlot();
        if (slot == -1) return;

        int previousSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(previousSlot);
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
