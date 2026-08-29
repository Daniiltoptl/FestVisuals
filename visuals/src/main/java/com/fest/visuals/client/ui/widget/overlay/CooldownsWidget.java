package com.fest.visuals.client.ui.widget.overlay;

import com.fest.visuals.api.utils.other.TextUtil;
import com.fest.visuals.client.features.modules.hud.CooldownsHudModule;
import com.fest.visuals.client.ui.widget.ContainerWidget;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;

public class CooldownsWidget extends ContainerWidget {
    @Override
    public String getName() {
        return "Cooldowns";
    }

    public CooldownsWidget() {
        super(120f, 100f);
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        Map<String, ContainerElement.ColoredString> cooldownData = new HashMap<>();
        if (mc.player == null) return cooldownData;

        ItemCooldowns manager = mc.player.getCooldowns();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            if (!manager.isOnCooldown(stack)) continue;

            int remaining = getRemainingCooldownTicks(stack, tickDelta);
            if (remaining > 0) {
                String name = stack.getHoverName().getString();
                String time = CooldownsHudModule.getInstance().showSeconds.getValue() ? TextUtil.getDurationText(remaining) : "";
                cooldownData.put(name, new ContainerElement.ColoredString(time));
            }
        }

        return cooldownData;
    }

    @Override
    public float fontMul() { return CooldownsHudModule.getInstance().fontScale.getValue(); }

    @Override
    protected boolean alwaysVisible() { return CooldownsHudModule.getInstance().alwaysShow.getValue(); }

    private int getRemainingCooldownTicks(ItemStack stack, float tickDelta) {
        ItemCooldowns manager = mc.player.getCooldowns();
        Identifier groupId = manager.getCooldownGroup(stack);
        ItemCooldowns.CooldownInstance entry = manager.cooldowns.get(groupId);

        if (entry != null) {
            return Math.max(0, entry.endTime() - (manager.tickCount + (int) tickDelta));
        }
        return 0;
    }
}
