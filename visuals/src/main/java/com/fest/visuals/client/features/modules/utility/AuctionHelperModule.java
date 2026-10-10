package com.fest.visuals.client.features.modules.utility;

import java.awt.Color;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.module.setting.ModeSetting;

/**
 * Highlights the best deals on the FunTime auction.
 *
 * <p>The price is read from each lot's lore — the line that mentions "цена" or a currency sign —
 * with "k"/"кк"/"m" suffixes and any digit grouping understood. The cheapest lots pulse green,
 * fading from the best down to the third; the lowest price per item, which is often a different
 * lot when stacks differ in size, gets its own colour.
 *
 * <p>The drawing is done by the container screen mixin, behind each item, so nothing covers
 * the item itself.
 */
@ModuleRegister(name = "Auction Helper", desc = "\u041F\u043E\u0434\u0441\u0432\u0435\u0442\u043A\u0430 \u0434\u0435\u0448\u0435\u0432\u044B\u0445 \u043B\u043E\u0442\u043E\u0432 \u043D\u0430 \u0430\u0443\u043A\u0446\u0438\u043E\u043D\u0430\u0445", category = Category.OTHER)
public class AuctionHelperModule extends Module {
    @Getter private static final AuctionHelperModule instance = new AuctionHelperModule();

    public final ModeSetting server = new ModeSetting("\u0421\u0435\u0440\u0432\u0435\u0440").value("FunTime").values("FunTime", "HolyWorld", "ReallyWorld");

    public final BooleanSetting onlyAuction = new BooleanSetting("\u0422\u043E\u043B\u044C\u043A\u043E \u0410\u0425").value(true);
    public final SliderSetting top = new SliderSetting("\u0422\u043E\u043F \u043B\u043E\u0442\u043E\u0432").value(3f).range(1f, 10f).step(1f);
    public final BooleanSetting pulse = new BooleanSetting("\u041F\u0443\u043B\u044C\u0441\u0430\u0446\u0438\u044F").value(true);
    public final BooleanSetting perItem = new BooleanSetting("\u0417\u0430 \u0448\u0442\u0443\u043a\u0443").value(true);
    public final ColorSetting cheapestColor = new ColorSetting("\u0426\u0432\u0435\u0442 \u043B\u0443\u0447\u0448\u0435\u0433\u043E").value(new Color(60, 255, 110, 190));
    public final ColorSetting cheapestUnitColor = new ColorSetting("\u0426\u0432\u0435\u0442 \u043B\u0443\u0447\u0448\u0435\u0433\u043E \u0437\u0430 \u0448\u0442").value(new Color(255, 210, 60, 190));

    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:[\\s\\u00A0,._'’]\\d{3})*(?:[.,]\\d+)?)\\s*([kкmмbб]{0,2})(?![\\p{L}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern GROUP = Pattern.compile("[\\s\u00A0,._'’](?=\\d{3}(?:\\D|$))");

    /** Rank of each slot this frame: 0 is the cheapest. Rebuilt every time the slots are drawn. */
    private final Map<Slot, Integer> ranks = new IdentityHashMap<>();
    private Slot bestPerItem;

    public AuctionHelperModule() {
        addSettings(server, onlyAuction, top, pulse, perItem, cheapestColor, cheapestUnitColor);
    }

    @Override
    public void onEvent() {
    }

    @Override
    public void onDisable() {
        ranks.clear();
        bestPerItem = null;
    }

    private static boolean looksLikeAuction(Component title) {
        String text = title.getString().toLowerCase(Locale.ROOT);
        return text.contains("\u0430\u0443\u043a\u0446\u0438\u043e\u043d") ||
               text.contains("\u0440\u044b\u043d\u043e\u043a") ||
               text.contains("auction") ||
               text.contains("\u043f\u043e\u0438\u0441\u043a") ||
               text.contains("search");
    }

    /** Called once per frame before the slots are drawn. */
    public void prepare(Component title, List<Slot> slots) {
        ranks.clear();
        bestPerItem = null;
        if (!isEnabled()) return;
        boolean isAuction = !onlyAuction.getValue() || looksLikeAuction(title);

        record Lot(Slot slot, long price, double perUnit) {}
        List<Lot> lots = new ArrayList<>();

        for (Slot slot : slots) {
            if (slot.container instanceof Inventory) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            long price = price(stack);
            if (price <= 0) continue;
            lots.add(new Lot(slot, price, price / (double) Math.max(1, stack.getCount())));
        }
        
        if (onlyAuction.getValue() && !isAuction && lots.isEmpty()) return;
        if (lots.isEmpty()) return;

        lots.sort((a, b) -> Long.compare(a.price(), b.price()));
        int limit = Math.min(lots.size(), top.getValue().intValue());
        for (int i = 0; i < limit; i++) ranks.put(lots.get(i).slot(), i);

        if (perItem.getValue()) {
            Lot best = null;
            for (Lot lot : lots) {
                if (best == null || lot.perUnit() < best.perUnit()) best = lot;
            }
            // Only worth a separate mark when it is not already the cheapest lot overall.
            if (best != null && ranks.getOrDefault(best.slot(), -1) != 0) bestPerItem = best.slot();
        }
    }

    /** ARGB fill for the slot, or zero for none. */
    public int highlight(Slot slot) {
        if (!isEnabled()) return 0;

        float wave = pulse.getValue() ? (float) (0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 180.0)) : 1f;

        if (slot == bestPerItem) return argb(cheapestUnitColor.getValue(), wave);

        Integer rank = ranks.get(slot);
        if (rank == null) return 0;

        int count = Math.max(1, Math.min(ranks.size(), top.getValue().intValue()));
        float strength = count == 1 ? 1f : 1f - rank / (float) count * 0.7f;
        return argb(cheapestColor.getValue(), wave * strength);
    }

    public boolean isCheapest(Slot slot) {
        Integer rank = ranks.get(slot);
        return rank != null && rank == 0;
    }

    private static int argb(Color color, float scale) {
        int alpha = Math.max(0, Math.min(255, Math.round(color.getAlpha() * scale)));
        return (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    /** The lot price from lore, or -1 when there is none. */
    static long price(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return -1;

        long best = -1;
        String srv = instance.server.getValue();
        for (Component line : lore.lines()) {
            String text = line.getString().replaceAll("(?i)\\\\u00A7[0-9a-fk-or]", "");
            String lower = text.toLowerCase(Locale.ROOT);
            
            if ("FunTime".equals(srv)) {
                boolean pricey = lower.contains("\u0446\u0435\u043D") || lower.contains("\u0441\u0442\u043E\u0438\u043C\u043E\u0441\u0442") || lower.contains("price")
                        || lower.contains("$") || lower.contains("\u0444\u0442") || lower.contains("\u0442\u043E\u043A\u0435\u043D");
                if (!pricey || lower.contains("\u0448\u0442") || lower.contains("\u043F\u0440\u0435\u0434\u043C\u0435\u0442")) continue;
                boolean perUnit = lower.contains("\u0437\u0430 1") || lower.contains("\u0437\u0430 \u0448") || lower.contains("/\u0448");
                long value = parse(text);
                if (value <= 0) continue;
                if (perUnit) value *= Math.max(1, stack.getCount());
                best = Math.max(best, value);
            } else if ("HolyWorld".equals(srv)) {
                boolean pricey = lower.contains("\u0446\u0435\u043D") || lower.contains("$") || lower.contains("\u0441\u0442\u043E\u0438\u043C\u043E\u0441\u0442");
                if (!pricey) continue;
                boolean perUnit = lower.contains("\u0437\u0430 1") || lower.contains("\u0437\u0430 \u0448") || lower.contains("\u0448\u0442");
                long value = parse(text);
                if (value <= 0) continue;
                if (perUnit) value *= Math.max(1, stack.getCount());
                best = Math.max(best, value);
            } else if ("ReallyWorld".equals(srv)) {
                boolean pricey = lower.contains("\u0446\u0435\u043D") || lower.contains("$") || lower.contains("\u0441\u0442\u043E\u0438\u043C\u043E\u0441\u0442") || lower.contains("\u043C\u043E\u043D\u0435\u0442");
                if (!pricey) continue;
                boolean perUnit = lower.contains("\u0437\u0430 1") || lower.contains("\u0437\u0430 \u0448");
                long value = parse(text);
                if (value <= 0) continue;
                if (perUnit) value *= Math.max(1, stack.getCount());
                best = Math.max(best, value);
            } else {
                long value = parse(text);
                if (value > 0) {
                    best = Math.max(best, value);
                }
            }
        }
        return best;
    }

    private static long parse(String text) {
        Matcher matcher = NUMBER.matcher(text);
        long best = -1;
        while (matcher.find()) {
            String digits = matcher.group(1);
            String suffix = matcher.group(2).toLowerCase(Locale.ROOT);

            // Grouping separators are dropped; a trailing ".5" style fraction only matters with a suffix.
            String integer = GROUP.matcher(digits).replaceAll("");
            double value;
            try {
                value = Double.parseDouble(integer.replace(',', '.').replaceAll("[\\s\\u00A0_'’]", ""));
            } catch (NumberFormatException e) {
                continue;
            }

            long multiplier = switch (suffix) {
                case "k", "к" -> 1_000L;
                case "kk", "кк", "m", "м" -> 1_000_000L;
                case "b", "б", "kkk", "ккк" -> 1_000_000_000L;
                default -> 1L;
            };
            long result = Math.round(value * multiplier);
            best = Math.max(best, result);
        }
        return best;
    }
}
