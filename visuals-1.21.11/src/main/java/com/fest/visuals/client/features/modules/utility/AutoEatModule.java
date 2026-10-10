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
import com.fest.visuals.api.utils.player.ItemUseHelper;

/**
 * Eats once hunger drops to the configured level, then puts the previous slot back.
 *
 * <p>Same machinery as Auto Invisible: the slot change needs a tick to reach the server and the
 * use key has to stay held for the whole animation, otherwise Minecraft cancels it immediately.
 */
@ModuleRegister(name = "Auto Eat", desc = "Ест, когда голод падает ниже порога", category = Category.OTHER)
public class AutoEatModule extends Module {
    @Getter private static final AutoEatModule instance = new AutoEatModule();

    public final SliderSetting hunger = new SliderSetting("Порог голода").value(16f).range(1f, 19f).step(1f);

    private final ItemUseHelper use = new ItemUseHelper();
    private int cooldown;

    public AutoEatModule() {
        addSettings(hunger);
    }

    @Override
    public void onEvent() {
        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> tick())));
    }

    @Override
    public void onDisable() {
        use.cancel();
        cooldown = 0;
    }

    private void tick() {
        if (mc.player == null || mc.gameMode == null) return;

        if (use.isBusy()) {
            if (use.tick()) cooldown = 10;
            return;
        }

        if (cooldown-- > 0) return;
        if (mc.screen != null || mc.player.isUsingItem()) return;
        if (mc.player.getFoodData().getFoodLevel() > hunger.getValue().intValue()) return;

        int slot = findFoodSlot();
        if (slot == -1) return;

        use.start(slot, InteractionHand.MAIN_HAND);
    }

    private int findFoodSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getComponents().has(DataComponents.FOOD)) return i;
        }
        return -1;
    }
}
