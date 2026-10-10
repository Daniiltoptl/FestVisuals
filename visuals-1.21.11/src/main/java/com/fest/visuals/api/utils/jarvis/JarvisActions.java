package com.fest.visuals.api.utils.jarvis;

import java.awt.Color;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleManager;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.Setting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.system.configs.ConfigManager;
import com.fest.visuals.client.ui.theme.ThemeEditor;

/**
 * What Jarvis may see and change on the client: modules with their descriptions and settings,
 * configs and themes.
 *
 * <p>The description travels with each module so the server can map "выключи цветное небо" to
 * Sky Color without the player knowing the module's name. Settings are listed by name and type
 * only; values are resolved here, against the live setting, so a sloppy value from the language
 * model is clamped or ignored rather than breaking anything.
 */
public final class JarvisActions {
    private JarvisActions() {
    }

    /** Everything the server needs to understand the player, sent with each utterance. */
    public static JsonObject context() {
        JsonObject context = new JsonObject();

        JsonArray modules = new JsonArray();
        for (Module module : ModuleManager.getInstance().getModules()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", module.getName());
            entry.addProperty("desc", module.getDescription());
            entry.addProperty("on", module.isEnabled());

            JsonArray settings = new JsonArray();
            for (Setting<?> setting : module.getSettings()) {
                String kind = kind(setting);
                if (kind == null) continue;
                JsonObject s = new JsonObject();
                s.addProperty("name", setting.getName());
                s.addProperty("type", kind);
                if (setting instanceof ModeSetting mode) {
                    JsonArray options = new JsonArray();
                    mode.getModes().forEach(options::add);
                    s.add("options", options);
                } else if (setting instanceof SliderSetting slider) {
                    s.addProperty("min", slider.getMin());
                    s.addProperty("max", slider.getMax());
                }
                settings.add(s);
            }
            entry.add("settings", settings);
            modules.add(entry);
        }
        context.add("modules", modules);

        JsonArray configs = new JsonArray();
        ConfigManager.getInstance().getConfigsNames().forEach(configs::add);
        context.add("configs", configs);

        JsonArray themes = new JsonArray();
        ThemeEditor.getInstance().themeNames().forEach(themes::add);
        context.add("themes", themes);
        context.addProperty("theme", ThemeEditor.getInstance().getCurrentTheme().getName());
        return context;
    }

    private static String kind(Setting<?> setting) {
        if (setting instanceof BooleanSetting) return "bool";
        if (setting instanceof SliderSetting) return "number";
        if (setting instanceof ModeSetting) return "mode";
        if (setting instanceof ColorSetting) return "color";
        return null;
    }

    /** Runs one action of the new kinds; returns a short report, or null when there was nothing to do. */
    public static String run(JsonObject action) {
        String type = action.has("type") ? action.get("type").getAsString() : "";
        return switch (type) {
            case "setting" -> setting(action);
            case "config" -> config(action);
            case "theme" -> theme(action);
            default -> null;
        };
    }

    private static Module findModule(String name) {
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.getName().equalsIgnoreCase(name.trim())) return module;
        }
        String wanted = normalise(name);
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (normalise(module.getName()).equals(wanted)) return module;
        }
        return null;
    }

    private static Setting<?> findSetting(Module module, String name) {
        String wanted = normalise(name);
        Setting<?> partial = null;
        for (Setting<?> setting : module.getSettings()) {
            String candidate = normalise(setting.getName());
            if (candidate.equals(wanted)) return setting;
            if (partial == null && (candidate.contains(wanted) || wanted.contains(candidate))) partial = setting;
        }
        return partial;
    }

    private static String normalise(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static String setting(JsonObject action) {
        if (!action.has("module") || !action.has("setting") || !action.has("value")) return null;

        Module module = findModule(action.get("module").getAsString());
        if (module == null) return "не нашёл модуль " + action.get("module").getAsString();
        Setting<?> setting = findSetting(module, action.get("setting").getAsString());
        if (setting == null) return "у " + module.getName() + " нет настройки " + action.get("setting").getAsString();

        JsonElement value = action.get("value");
        try {
            switch (setting) {
                case BooleanSetting bool -> bool.setValue(asBoolean(value));
                case SliderSetting slider -> {
                    float number = value.getAsFloat();
                    slider.setValue(Math.max(slider.getMin(), Math.min(slider.getMax(), number)));
                }
                case ModeSetting mode -> {
                    String wanted = value.getAsString();
                    String match = null;
                    for (String option : mode.getModes()) {
                        if (option.equalsIgnoreCase(wanted)) match = option;
                    }
                    if (match == null) {
                        for (String option : mode.getModes()) {
                            if (normalise(option).contains(normalise(wanted))) match = option;
                        }
                    }
                    if (match == null) return "у " + setting.getName() + " нет варианта " + wanted;
                    mode.setValue(match);
                }
                case ColorSetting color -> color.setValue(parseColor(value.getAsString(), color.getValue()));
                default -> {
                    return null;
                }
            }
        } catch (RuntimeException e) {
            return "не смог поставить " + setting.getName();
        }
        return module.getName() + ": " + setting.getName() + " → " + value.getAsString();
    }

    private static boolean asBoolean(JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean();
        String text = value.getAsString().toLowerCase(Locale.ROOT);
        return text.equals("true") || text.equals("вкл") || text.equals("да") || text.equals("on") || text.equals("1");
    }

    private static Color parseColor(String text, Color fallback) {
        String hex = text.trim().replace("#", "");
        if (hex.matches("[0-9a-fA-F]{6}")) {
            int rgb = Integer.parseInt(hex, 16);
            return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, fallback.getAlpha());
        }
        return fallback;
    }

    private static String config(JsonObject action) {
        if (!action.has("name")) return null;
        String name = action.get("name").getAsString().trim();
        String operation = action.has("action") ? action.get("action").getAsString() : "load";
        if (name.isEmpty() || !name.matches("[\\p{L}\\p{N}_\\- ]{1,32}")) return "плохое имя конфига";

        ConfigManager configs = ConfigManager.getInstance();
        if ("save".equals(operation)) {
            configs.save(name);
            return "сохранил конфиг " + name;
        }
        for (String existing : configs.getConfigsNames()) {
            if (existing.equalsIgnoreCase(name)) {
                configs.load(existing);
                return "загрузил конфиг " + existing;
            }
        }
        return "нет конфига " + name;
    }

    private static String theme(JsonObject action) {
        if (!action.has("name")) return null;
        String name = action.get("name").getAsString();
        return ThemeEditor.getInstance().selectTheme(name) ? "тема " + name : "нет темы " + name;
    }
}
