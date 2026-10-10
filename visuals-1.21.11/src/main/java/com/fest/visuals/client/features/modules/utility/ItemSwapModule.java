package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import org.lwjgl.glfw.GLFW;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BindSetting;
import com.fest.visuals.api.module.setting.ModeSetting;

@ModuleRegister(name = "Item Swap", desc = "\u041c\u0435\u043d\u044f\u0435\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u0432\u043e \u0432\u0442\u043e\u0440\u043e\u0439 \u0440\u0443\u043a\u0435", category = Category.OTHER)
public class ItemSwapModule extends Module {
    @Getter private static final ItemSwapModule instance = new ItemSwapModule();

    public final BindSetting bind = new BindSetting("\u0411\u0438\u043d\u0434");
    public final ModeSetting swapFrom = new ModeSetting("\u0421 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430").values("\u0422\u043e\u0442\u0435\u043c", "\u0428\u0430\u0440", "\u0429\u0438\u0442", "\u0413\u0435\u043f\u043b").value("\u0422\u043e\u0442\u0435\u043c");
    public final ModeSetting swapTo = new ModeSetting("\u041d\u0430 \u043f\u0440\u0435\u0434\u043c\u0435\u0442").values("\u0422\u043e\u0442\u0435\u043c", "\u0428\u0430\u0440", "\u0429\u0438\u0442", "\u0413\u0435\u043f\u043b").value("\u0428\u0430\u0440");

    private boolean wasPressed = false;

    public ItemSwapModule() {
        addSettings(bind, swapFrom, swapTo);
    }

    @Override
    public void onEvent() {
        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> tick())));
    }

    private void tick() {
        if (mc.player == null) return;
        
        int key = bind.getValue();
        if (key == -999) {
            wasPressed = false;
            return;
        }

        boolean pressed = key < 0 
                ? GLFW.glfwGetMouseButton(mc.getWindow().handle(), key + 100) == 1
                : GLFW.glfwGetKey(mc.getWindow().handle(), key) == 1;

        if (pressed && !wasPressed) {
            doSwap();
        }
        wasPressed = pressed;
    }

    private void doSwap() {
        ItemStack offhand = mc.player.getOffhandItem();
        
        if (!isItem(offhand, swapFrom.getValue())) {
            if (isItem(offhand, swapTo.getValue())) {
                swapItems(swapTo.getValue(), swapFrom.getValue());
                return;
            }
            return;
        }

        swapItems(swapFrom.getValue(), swapTo.getValue());
    }

    private void swapItems(String fromType, String toType) {
        Inventory inv = mc.player.getInventory();
        
        int targetSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (isItem(stack, toType)) {
                targetSlot = i;
                break;
            }
        }

        if (targetSlot == -1) {
            return;
        }

        int syncId = mc.player.inventoryMenu.containerId;
        mc.gameMode.handleInventoryMouseClick(syncId, 45, targetSlot, ClickType.SWAP, mc.player);
    }

    private boolean isItem(ItemStack stack, String type) {
        if (stack == null || stack.isEmpty()) return false;
        
        if (type.equals("\u0422\u043e\u0442\u0435\u043c")) return stack.is(Items.TOTEM_OF_UNDYING);
        if (type.equals("\u0429\u0438\u0442")) return stack.is(Items.SHIELD);
        if (type.equals("\u0413\u0435\u043f\u043b")) return stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.GOLDEN_APPLE);
        if (type.equals("\u0428\u0430\u0440")) {
            String name = stack.getHoverName().getString().toLowerCase();
            return name.contains("\u0448\u0430\u0440") || name.contains("\u0441\u0444\u0435\u0440\u0430") || name.contains("sphere");
        }
        return false;
    }
}
