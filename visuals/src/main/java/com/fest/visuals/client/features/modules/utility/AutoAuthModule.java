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
 * Пароль и регулярки задаются в other/utility.json (см. UtilityConfig) —
 * в текущем ClickGUI нет текстового поля ввода. Работает только с
 * вашим собственным аккаунтом на сервере, куда вы сами вводите пароль.
 */
@ModuleRegister(name = "Auto Auth", desc = "Автоматически регистрирует и входит в аккаунт на сервере", category = Category.UTILITY)
public class AutoAuthModule extends Module {
    @Getter private static final AutoAuthModule instance = new AutoAuthModule();

    private boolean handledThisJoin = false;

    @Override
    public void onEvent() {
        addEvents(PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive() || mc.player == null) return;

            if (event.packet() instanceof net.minecraft.network.protocol.game.ClientboundLoginPacket) {
                handledThisJoin = false;
                return;
            }

            UtilityConfig config = UtilityConfig.getInstance();
            if (config.getAuthPassword().isEmpty()) return;

            String text = ChatPacketUtil.extractText(event.packet());
            if (text.isEmpty() || handledThisJoin) return;

            if (matches(text, config.getAuthRegisterPattern())) {
                mc.player.connection.sendCommand("register " + config.getAuthPassword() + " " + config.getAuthPassword());
                handledThisJoin = true;
            } else if (matches(text, config.getAuthLoginPattern())) {
                mc.player.connection.sendCommand("login " + config.getAuthPassword());
                handledThisJoin = true;
            }
        })));
    }

    @Override
    public void onDisable() {
        handledThisJoin = false;
    }

    private boolean matches(String text, String pattern) {
        try {
            return Pattern.compile(pattern).matcher(text).find();
        } catch (Exception e) {
            return false;
        }
    }
}
