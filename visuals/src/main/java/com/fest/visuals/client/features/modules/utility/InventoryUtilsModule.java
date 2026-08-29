package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;

/**
 * Пополняет опустевшие слоты хотбара (еда, инструменты) предметами того
 * же типа из основного инвентаря — тот же механизм, что и обычная
 * "цифра на клавиатуре, наведя на слот" (ContainerInput.SWAP), просто
 * автоматически. Не даёт никакого игрового преимущества — это ровно то
 * же самое действие, которое игрок мог бы сделать вручную.
 */
@ModuleRegister(name = "Inventory Utils", desc = "Помощник по инвентарю", category = Category.UTILITY)
public class InventoryUtilsModule extends Module {
    @Getter private static final InventoryUtilsModule instance = new InventoryUtilsModule();

    private int cooldown = 0;

    @Override
    public void onEvent() {
        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> tick())));
    }

    private void tick() {
        if (mc.player == null || mc.gameMode == null || mc.gui.screen() != null) return;
        if (cooldown-- > 0) return;

        for (int hotbarIndex = 0; hotbarIndex < 9; hotbarIndex++) {
            ItemStack hotbarStack = mc.player.getInventory().getItem(hotbarIndex);
            if (!hotbarStack.isEmpty()) continue;

            int mainSlotId = findRefillSlot(hotbarIndex);
            if (mainSlotId == -1) continue;

            mc.gameMode.handleContainerInput(
                    mc.player.inventoryMenu.containerId,
                    mainSlotId,
                    hotbarIndex,
                    ContainerInput.SWAP,
                    mc.player
            );

            cooldown = 4;
            return;
        }
    }

    /**
     * Простая эвристика: заполняет пустой хотбар-слот первым найденным в
     * основном инвентаре инструментом (кирка/топор/лопата/меч) или едой.
     * Не различает "тот самый" предмет, что был раньше — просто не даёт
     * слоту пустовать. При необходимости уточните логику под свои нужды.
     */
    private int findRefillSlot(int emptyHotbarIndex) {
        for (Slot slot : mc.player.inventoryMenu.slots) {
            if (slot.container != mc.player.getInventory()) continue;
            if (slot.getContainerSlot() < 9 || slot.getContainerSlot() >= 36) continue; // только основной инвентарь

            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            if (isFood(stack) || isTool(stack)) {
                return slot.index;
            }
        }
        return -1;
    }

    private boolean isFood(ItemStack stack) {
        return stack.getComponents().has(net.minecraft.core.component.DataComponents.FOOD);
    }

    private boolean isTool(ItemStack stack) {
        return stack.getItem().components().has(net.minecraft.core.component.DataComponents.TOOL);
    }
}
