package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.module.setting.StringSetting;

/**
 * Sends the auction resell command on a timer.
 *
 * <p>Driven by the clock rather than by a chat trigger: auction plugins announce an expired lot in
 * wildly different ways, while the interval is something the player can see and tune.
 */
@ModuleRegister(name = "Auto Resell", desc = "Перевыставляет лоты на аукционе", category = Category.OTHER)
public class AutoResellModule extends Module {
    @Getter private static final AutoResellModule instance = new AutoResellModule();

    public final SliderSetting interval = new SliderSetting("Интервал (сек)").value(40f).range(5f, 300f).step(5f);
    public final StringSetting command = new StringSetting("Команда")
            .value("ah resell").placeholder("ah resell").maxLength(64);

    private int ticks;

    public AutoResellModule() {
        addSettings(interval, command);
    }

    @Override
    public void onEvent() {
        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || command.isEmpty()) return;

            if (++ticks < interval.getValue() * 20f) return;
            ticks = 0;

            mc.player.connection.sendCommand(command.getValue());
        })));
    }

    @Override
    public void onEnable() {
        // Fire on the next tick rather than making the player wait out a full interval first.
        ticks = Integer.MAX_VALUE / 2;
    }

    @Override
    public void onDisable() {
        ticks = 0;
    }
}
