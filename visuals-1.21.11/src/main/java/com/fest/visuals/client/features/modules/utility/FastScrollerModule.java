package com.fest.visuals.client.features.modules.utility;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ModeSetting;

/**
 * Drag the cursor across slots instead of clicking each one.
 *
 * <p>Hold the modifier and the left button, then sweep: every slot the cursor crosses is
 * shift-clicked once. The work happens in {@code MixinHandledScreen}, which is where the hovered
 * slot lives; this class only carries the settings and the on/off state.
 */
@ModuleRegister(name = "Fast Scroller", desc = "Перемещает предметы движением мыши", category = Category.OTHER)
public class FastScrollerModule extends Module {
    @Getter private static final FastScrollerModule instance = new FastScrollerModule();

    public final ModeSetting modifier = new ModeSetting("Модификатор")
            .value("Shift").values("Shift", "Ctrl", "Alt", "Без модификатора");
    public final ModeSetting action = new ModeSetting("Действие")
            .value("Переместить").values("Переместить", "Выбросить");
    public final BooleanSetting rightButton = new BooleanSetting("Правой кнопкой").value(true);

    public FastScrollerModule() {
        addSettings(modifier, action, rightButton);
    }

    /**
     * True when the configured modifier is held, or when none is required. Queried straight from
     * the window: Screen no longer exposes the hasShiftDown/hasControlDown helpers.
     */
    public boolean modifierHeld() {
        return switch (modifier.getValue()) {
            case "Ctrl" -> down(GLFW.GLFW_KEY_LEFT_CONTROL) || down(GLFW.GLFW_KEY_RIGHT_CONTROL);
            case "Alt" -> down(GLFW.GLFW_KEY_LEFT_ALT) || down(GLFW.GLFW_KEY_RIGHT_ALT);
            case "Без модификатора" -> true;
            default -> down(GLFW.GLFW_KEY_LEFT_SHIFT) || down(GLFW.GLFW_KEY_RIGHT_SHIFT);
        };
    }

    private boolean down(int key) {
        return InputConstants.isKeyDown(mc.getWindow(), key);
    }

    public boolean dropping() {
        return action.is("Выбросить");
    }

    public boolean acceptsButton(int button) {
        return button == 0 || (button == 1 && rightButton.getValue());
    }

    @Override
    public void onEvent() {
    }
}
