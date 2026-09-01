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
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

/**
 * Eats when hunger drops to the configured level.
 *
 * <p>Eating takes about thirty ticks and only continues while the food stays selected, so the
 * module switches once, waits for the use to finish, and only then puts the previous slot back.
 * Re-issuing the use every tick — the previous behaviour — restarted the animation forever and
 * the player never actually ate.
 */
@ModuleRegister(name = "Auto Eat", desc = "Ест, когда голод падает ниже порога", category = Category.OTHER)
public class AutoEatModule extends Module {
    @Getter private static final AutoEatModule instance = new AutoEatModule();

    public final SliderSetting hunger = new SliderSetting("Порог голода").value(14f).range(1f, 19f).step(1f);
    public final BooleanSetting whileSaturated = new BooleanSetting("Есть при насыщении").value(false);

    /** Item use does not report as started until the tick after it is requested. */
    private static final int START_GRACE = 6;

    private boolean eating;
    private int returnSlot = -1;
    private int grace;

    public AutoEatModule() {
        addSettings(hunger, whileSaturated);
    }

    @Override
    public void onEvent() {
        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> tick())));
    }

    @Override
    public void onDisable() {
        stop();
    }

    private void tick() {
        if (mc.player == null || mc.gameMode == null) return;

        if (eating) {
            if (grace-- > 0) return;
            if (mc.player.isUsingItem()) return;

            // Full again, or the stack ran out — either way this pass is done.
            stop();
            return;
        }

        if (mc.gui.screen() != null || mc.player.isUsingItem()) return;
        if (mc.player.getFoodData().getFoodLevel() > hunger.getValue().intValue()) return;
        if (!whileSaturated.getValue() && mc.player.getFoodData().getSaturationLevel() > 0f
                && mc.player.getFoodData().getFoodLevel() >= 18) {
            return;
        }

        int slot = findFoodSlot();
        if (slot == -1) return;

        returnSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);

        eating = true;
        grace = START_GRACE;
    }

    private void stop() {
        if (mc.player != null && returnSlot >= 0) {
            mc.player.getInventory().setSelectedSlot(returnSlot);
        }
        eating = false;
        returnSlot = -1;
        grace = 0;
    }

    private int findFoodSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getComponents().has(DataComponents.FOOD)) return i;
        }
        return -1;
    }
}
