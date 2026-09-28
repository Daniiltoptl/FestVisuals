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
@ModuleRegister(name = "Auction Helper", desc = "Подсвечивает самые дешёвые лоты на аукционе FT", category = Category.OTHER)
public class AuctionHelperModule extends Module {
    @Getter private static final AuctionHelperModule instance = new AuctionHelperModule();

    public final BooleanSetting onlyAuction = new BooleanSetting("Только в аукционе").value(true);
    public final SliderSetting top = new SliderSetting("Сколько подсвечивать").value(3f).range(1f, 10f).step(1f);
    public final BooleanSetting perItem = new BooleanSetting("Лучшая цена за штуку").value(true);
    public final BooleanSetting pulse = new BooleanSetting("Пульсация").value(true);
    public final ColorSetting cheapColor = new ColorSetting("Цвет дешёвых").value(new Color(60, 255, 110, 190));
    public final ColorSetting perItemColor = new ColorSetting("Цвет за штуку").value(new Color(255, 210, 60, 190));

    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:[\\s\\u00A0,._'’]\\d{3})*(?:[.,]\\d+)?)\\s*([kкmмbб]{0,2})(?![\\p{L}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern GROUP = Pattern.compile("[\\s\u00A0,._'’](?=\\d{3}(?:\\D|$))");

    /** Rank of each slot this frame: 0 is the cheapest. Rebuilt every time the slots are drawn. */
    private final Map<Slot, Integer> ranks = new IdentityHashMap<>();
    private Slot bestPerItem;

    public AuctionHelperModule() {
        addSettings(onlyAuction, top, perItem, pulse, cheapColor, perItemColor);
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
        return text.contains("аукцион") || text.contains("поиск") || text.contains("auction") || text.contains("маркет");
    }

    /** Called once per frame before the slots are drawn. */
    public void prepare(Component title, List<Slot> slots) {
        ranks.clear();
        bestPerItem = null;
        if (!isEnabled() || (onlyAuction.getValue() && !looksLikeAuction(title))) return;

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

        if (slot == bestPerItem) return argb(perItemColor.getValue(), wave);

        Integer rank = ranks.get(slot);
        if (rank == null) return 0;

        int count = Math.max(1, Math.min(ranks.size(), top.getValue().intValue()));
        float strength = count == 1 ? 1f : 1f - rank / (float) count * 0.7f;
        return argb(cheapColor.getValue(), wave * strength);
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
        for (Component line : lore.lines()) {
            String text = line.getString().replaceAll("§[0-9a-fk-or]", "");
            String lower = text.toLowerCase(Locale.ROOT);
            boolean pricey = lower.contains("цена") || lower.contains("стоим") || lower.contains("price")
                    || lower.contains("$") || lower.contains("¤") || lower.contains("монет");
            if (!pricey || lower.contains("истек") || lower.contains("осталось")) continue;

            // "Цена за 1 шт" style lines are per-item; scale them back up to the lot.
            boolean perUnit = lower.contains("за 1") || lower.contains("за шт") || lower.contains("/шт");
            long value = parse(text);
            if (value <= 0) continue;
            if (perUnit) value *= Math.max(1, stack.getCount());
            best = Math.max(best, value);
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
