package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.PacketEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.system.configs.UtilityConfig;
import com.fest.visuals.api.utils.other.TextUtil;

/**
 * По умолчанию координаты выводятся только вам в клиентский чат
 * (не отправляются на сервер). Включите deathCordsPublic в
 * other/utility.json, если хотите реально отправлять их в чат сервера.
 */
@ModuleRegister(name = "Death Cords", desc = "Отправляет координаты смерти в чат", category = Category.OTHER)
public class DeathCordsModule extends Module {
    @Getter private static final DeathCordsModule instance = new DeathCordsModule();

    @Override
    public void onEvent() {
        addEvents(PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive() || mc.player == null) return;
            if (!(event.packet() instanceof ClientboundPlayerCombatKillPacket)) return;

            int x = (int) mc.player.getX();
            int y = (int) mc.player.getY();
            int z = (int) mc.player.getZ();
            String dimension = mc.player.level().dimension().identifier().getPath();

            String message = String.format("Смерть на %d, %d, %d (%s)", x, y, z, dimension);

            if (UtilityConfig.getInstance().isDeathCordsPublic()) {
                mc.player.connection.sendChat(message);
            } else {
                TextUtil.sendMessage(message);
            }
        })));
    }
}
