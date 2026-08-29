package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.PacketEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.utils.other.ChatPacketUtil;

import java.util.regex.Pattern;

@ModuleRegister(name = "Auto Accept", desc = "Автоматически принимает телепортацию", category = Category.OTHER)
public class AutoAcceptModule extends Module {
    @Getter private static final AutoAcceptModule instance = new AutoAcceptModule();

    // Общие формулировки популярных серверных плагинов (EssentialsX и т.п.).
    // При необходимости отредактируйте под конкретный сервер.
    private final Pattern teleportRequestPattern =
            Pattern.compile("(?i)has requested to teleport|запрос на телепорт|хочет телепортироваться к вам");

    @Override
    public void onEvent() {
        addEvents(PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive() || mc.player == null) return;

            String text = ChatPacketUtil.extractText(event.packet());
            if (text.isEmpty()) return;

            if (teleportRequestPattern.matcher(text).find()) {
                mc.player.connection.sendCommand("tpaccept");
            }
        })));
    }
}
