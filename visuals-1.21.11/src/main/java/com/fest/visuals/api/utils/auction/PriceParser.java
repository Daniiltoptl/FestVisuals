package com.fest.visuals.api.utils.auction;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.fest.visuals.api.system.interfaces.QuickImports;

public class PriceParser implements QuickImports {
    public ParseModeChoice currentMode = ParseModeChoice.FUN_TIME;

    public int getPrice(ItemStack stack) {
        for (Component text : stack.getTooltipLines(Item.TooltipContext.EMPTY, mc.player, TooltipFlag.NORMAL)) {
            String str = text.getString().replace("§r", "").replace("¤", "").trim();
            String textPrice = getStr();
            if (str.startsWith(textPrice)) return Integer.parseInt(str.replace(textPrice, "").replace(",", "").replace(" ", "").trim());
        }
        return -1;
    }

    private String getStr() {
        switch (currentMode) {
            case FUN_TIME -> { return "$ Ценa $"; }
            case SPOOKY_TIME -> { return "$ Цена: $"; }
            case HOLY_WORLD -> { return "▍ Цена за 1 ед.:"; }
            case REALLY_WORLD -> { return "Цена:"; }
        }
        return "";
    }
}
