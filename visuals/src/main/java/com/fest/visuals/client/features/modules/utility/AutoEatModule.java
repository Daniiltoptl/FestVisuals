package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.SliderSetting;

@ModuleRegister(name = "Auto Eat", desc = "Автоматически ест еду при определённом значении голода", category = Category.OTHER)
public class AutoEatModule extends Module {
    @Getter private static final AutoEatModule instance = new AutoEatModule();

    public final SliderSetting hunger = new SliderSetting("Голод").value(14f).range(0f, 19f).step(1f);

    private int previousSlot = -1;
    private boolean switchedSlot = false;

    public AutoEatModule() {
        addSettings(hunger);
    }

    @Override
    public void onEvent() {
        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> tick())));
    }

    @Override
    public void onDisable() {
        restoreSlot();
    }

    private void tick() {
        if (mc.player == null || mc.gameMode == null) return;

        boolean shouldEat = mc.player.getFoodData().getFoodLevel() <= hunger.getValue().intValue();

        if (!shouldEat) {
            restoreSlot();
            return;
        }

        if (mc.player.isUsingItem()) return;

        int foodSlot = findFoodSlot();
        if (foodSlot == -1) return;

        if (!switchedSlot) {
            previousSlot = mc.player.getInventory().getSelectedSlot();
            switchedSlot = true;
        }

        mc.player.getInventory().setSelectedSlot(foodSlot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    }

    private void restoreSlot() {
        if (switchedSlot && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        switchedSlot = false;
    }

    private int findFoodSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getComponents().has(DataComponents.FOOD)) {
                return i;
            }
        }
        return -1;
    }
}
