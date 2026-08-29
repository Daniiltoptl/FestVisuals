package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.PacketEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.system.configs.UtilityConfig;

/**
 * Команда задаётся в other/utility.json -> joinCommand (без ведущего '/').
 * Отправляется один раз, спустя небольшую задержку после входа на сервер.
 */
@ModuleRegister(name = "Auto Join", desc = "Автоматически заходит на режим", category = Category.UTILITY)
public class AutoJoinModule extends Module {
    @Getter private static final AutoJoinModule instance = new AutoJoinModule();

    private int delayTicks = -1;

    @Override
    public void onEvent() {
        addEvents(PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isReceive() && event.packet() instanceof ClientboundLoginPacket) {
                delayTicks = 40; // ~2 секунды на догрузку мира перед отправкой команды
            }
        })));

        addEvents(com.fest.visuals.api.event.events.client.TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (delayTicks < 0) return;

            delayTicks--;
            if (delayTicks == 0) {
                String command = UtilityConfig.getInstance().getJoinCommand();
                if (mc.player != null && !command.isEmpty()) {
                    mc.player.connection.sendCommand(command);
                }
            }
        })));
    }
}
