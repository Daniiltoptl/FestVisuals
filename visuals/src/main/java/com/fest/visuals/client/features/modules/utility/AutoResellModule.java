package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.PacketEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.system.configs.UtilityConfig;
import com.fest.visuals.api.utils.other.ChatPacketUtil;

import java.util.regex.Pattern;

/**
 * Триггер-фраза и команда перевыставления задаются в other/utility.json
 * (resellTrigger / resellCommand) — они зависят от конкретного плагина
 * аукциона на сервере, поэтому вынесены в конфиг, а не захардкожены.
 */
@ModuleRegister(name = "Auto Resell", desc = "Автоматически перевыставляет предметы на аукционе", category = Category.UTILITY)
public class AutoResellModule extends Module {
    @Getter private static final AutoResellModule instance = new AutoResellModule();

    @Override
    public void onEvent() {
        addEvents(PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive() || mc.player == null) return;

            UtilityConfig config = UtilityConfig.getInstance();
            if (config.getResellCommand().isEmpty()) return;

            String text = ChatPacketUtil.extractText(event.packet());
            if (text.isEmpty()) return;

            try {
                if (Pattern.compile(config.getResellTrigger()).matcher(text).find()) {
                    mc.player.connection.sendCommand(config.getResellCommand());
                }
            } catch (Exception ignored) {
            }
        })));
    }
}
