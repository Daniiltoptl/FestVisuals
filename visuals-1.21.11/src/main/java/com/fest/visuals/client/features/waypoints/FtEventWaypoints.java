package com.fest.visuals.client.features.waypoints;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.other.TextUtil;

/**
 * Turns FunTime event announcements in chat into waypoints.
 *
 * <p>The server announces a spawning event with its name and coordinates, sometimes with a
 * countdown ("через: 120 сек"). A line only counts when it names a known event <em>and</em>
 * carries coordinates, so ordinary chat with numbers in it never creates a mark. The mark is
 * temporary: it lives for the countdown plus a margin, or a fixed time otherwise, and a repeat
 * announcement for the same event nearby refreshes it instead of adding a duplicate.
 */
public final class FtEventWaypoints implements QuickImports {
    /** How long a mark stays when the announcement gives no countdown. */
    private static final long DEFAULT_LIFETIME_MS = 15 * 60_000L;
    /** Kept around after the countdown ends, for the looting part of the event. */
    private static final long AFTER_START_MS = 5 * 60_000L;

    private record Event(String keyword, String name, Color color) {}

    /** Most specific first: "алтарь" alone would otherwise swallow "мистический алтарь". */
    private static final List<Event> EVENTS = List.of(
            new Event("мистический алтарь", "Мистический алтарь", new Color(180, 90, 255)),
            new Event("алтарь нежити", "Алтарь нежити", new Color(120, 200, 120)),
            new Event("мистический сундук", "Мистический сундук", new Color(200, 110, 255)),
            new Event("сундук смерти", "Сундук смерти", new Color(220, 60, 60)),
            new Event("маяк убийца", "Маяк убийца", new Color(255, 70, 70)),
            new Event("загадочный маяк", "Загадочный маяк", new Color(90, 200, 255)),
            new Event("вулкан", "Вулкан", new Color(255, 120, 30)),
            new Event("метеорит", "Метеоритный дождь", new Color(255, 170, 60)),
            new Event("аирдроп", "Аирдроп", new Color(80, 220, 255)),
            new Event("аир дроп", "Аирдроп", new Color(80, 220, 255)),
            new Event("airdrop", "Аирдроп", new Color(80, 220, 255)),
            new Event("смертельный вагон", "Смертельный вагон", new Color(200, 60, 90)),
            new Event("адская резня", "Адская резня", new Color(230, 40, 40)),
            new Event("тайник удачи", "Тайник удачи", new Color(255, 215, 60)),
            new Event("гейзер", "Гейзер", new Color(110, 190, 255)),
            new Event("алтарь", "Алтарь", new Color(170, 120, 255))
    );

    private static final Pattern LABELLED = Pattern.compile(
            "x\\s*[:=]?\\s*(-?\\d{1,7}).{0,8}?y\\s*[:=]?\\s*(-?\\d{1,4}).{0,8}?z\\s*[:=]?\\s*(-?\\d{1,7})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRIPLE = Pattern.compile("(-?\\d{1,7})[\\s,;/]+(-?\\d{1,4})[\\s,;/]+(-?\\d{1,7})");
    private static final Pattern PAIR = Pattern.compile("(-?\\d{1,7})[\\s,;/]+(-?\\d{1,7})");
    private static final Pattern COUNTDOWN = Pattern.compile("(\\d{1,5})\\s*(сек|мин)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private FtEventWaypoints() {
    }

    /** Called for every chat line from the server. */
    public static void onChat(String raw) {
        if (mc.player == null || mc.level == null || raw == null || raw.isEmpty()) return;

        String text = raw.replaceAll("§[0-9a-fk-or]", "");
        String lower = text.toLowerCase(Locale.ROOT);

        Event event = null;
        for (Event candidate : EVENTS) {
            if (lower.contains(candidate.keyword())) {
                event = candidate;
                break;
            }
        }
        if (event == null) return;

        double[] at = coordinates(lower);
        if (at == null) return;

        long now = System.currentTimeMillis();
        long lifetime = DEFAULT_LIFETIME_MS;
        Matcher countdown = COUNTDOWN.matcher(lower.substring(Math.min(lower.length(), lower.indexOf(event.keyword()))));
        if (countdown.find()) {
            long amount = Long.parseLong(countdown.group(1));
            boolean minutes = countdown.group(2).startsWith("м");
            lifetime = amount * (minutes ? 60_000L : 1000L) + AFTER_START_MS;
        }

        String dimension = mc.level.dimension().toString();
        WaypointManager manager = WaypointManager.getInstance();

        for (Waypoint existing : manager.getWaypoints()) {
            if (existing.getExpiresAt() == 0 || !existing.getName().equals(event.name())) continue;
            double dx = existing.getX() - at[0], dz = existing.getZ() - at[2];
            if (dx * dx + dz * dz < 32 * 32) {
                existing.setX(at[0]);
                existing.setY(at[1]);
                existing.setZ(at[2]);
                existing.setExpiresAt(now + lifetime);
                manager.save();
                return;
            }
        }

        Waypoint waypoint = new Waypoint(event.name(), at[0], at[1], at[2], dimension, event.color(), "COORDS");
        waypoint.setExpiresAt(now + lifetime);
        manager.addWaypoint(waypoint);

        TextUtil.sendMessage("Метка ивента: " + event.name() + " (" + (int) at[0] + ", " + (int) at[1] + ", " + (int) at[2] + ")");
    }

    /** x, y, z from the line; y falls back to sea level when only x and z are given. */
    private static double[] coordinates(String lower) {
        Matcher labelled = LABELLED.matcher(lower);
        if (labelled.find()) {
            return new double[]{parse(labelled.group(1)), parse(labelled.group(2)), parse(labelled.group(3))};
        }

        // Times like "120 сек" are not coordinates; blank them out before looking for number groups.
        String cleaned = COUNTDOWN.matcher(lower).replaceAll(" ");

        Matcher triple = TRIPLE.matcher(cleaned);
        if (triple.find()) {
            return new double[]{parse(triple.group(1)), parse(triple.group(2)), parse(triple.group(3))};
        }

        if (lower.contains("координат") || lower.contains("коорд")) {
            Matcher pair = PAIR.matcher(cleaned);
            if (pair.find()) return new double[]{parse(pair.group(1)), 70, parse(pair.group(2))};
        }
        return null;
    }

    private static double parse(String number) {
        return Double.parseDouble(number) + 0.5;
    }
}
