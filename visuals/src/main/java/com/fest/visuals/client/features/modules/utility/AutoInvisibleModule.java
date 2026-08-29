package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;

@ModuleRegister(name = "Auto Invisible", desc = "Автоматически пьёт зелье невидимости", category = Category.UTILITY)
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
        // NOTE: точный API PotionContents/MobEffects может отличаться между
        // версиями маппингов — при сборке под конкретную версию Minecraft
        // сверьте этот метод с ./gradlew build (сборка недоступна в среде,
        // где готовился этот патч).
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
            if (potion == null) continue;

            boolean hasInvisibility = potion.getAllEffects().stream()
                    .anyMatch(effect -> effect.getEffect() == BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.INVISIBILITY.value()));

            if (hasInvisibility) return i;
        }
        return -1;
    }
}
