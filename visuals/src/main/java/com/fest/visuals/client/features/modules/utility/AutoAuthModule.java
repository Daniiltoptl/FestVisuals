package com.fest.visuals.client.features.modules.utility;

import java.util.regex.Pattern;

import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.PacketEvent;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.module.setting.StringSetting;
import com.fest.visuals.api.utils.other.ChatPacketUtil;

/**
 * Logs in or registers on servers that ask for it in chat.
 *
 * <p>Rather than matching the prose, which every server words differently, this looks for the
 * command the server is telling you to run: {@code /reg} or {@code /register} means the account
 * does not exist yet, {@code /login} or {@code /l} means it does. That is the one part of such a
 * message that is stable across servers.
 *
 * <p>The password is stored in the client config as plain text, like every other setting. It only
 * ever goes to the server you are already connected to.
 */
@ModuleRegister(name = "Auto Auth", desc = "Автоматически логинится и регистрируется", category = Category.OTHER)
public class AutoAuthModule extends Module {
    @Getter private static final AutoAuthModule instance = new AutoAuthModule();

    public final StringSetting password = new StringSetting("Пароль")
            .placeholder("введите пароль").maxLength(32).secret();
    public final SliderSetting delay = new SliderSetting("Задержка (тики)").value(20f).range(0f, 100f).step(5f);

    /** "/reg", "/register", "/r" — the account still has to be created. */
    private static final Pattern REGISTER = Pattern.compile("(?i)(^|[\\s\\p{Punct}])/(register|reg|r)(?![\\w])");

    /** "/login", "/l" — the account exists. */
    private static final Pattern LOGIN = Pattern.compile("(?i)(^|[\\s\\p{Punct}])/(login|log|l)(?![\\w])");

    private String pending;
    private int countdown = -1;
    private boolean handledThisJoin;

    public AutoAuthModule() {
        addSettings(password, delay);
    }

    @Override
    public void onEvent() {
        addEvents(PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive() || mc.player == null) return;

            if (event.packet() instanceof ClientboundLoginPacket) {
                reset();
                return;
            }

            if (handledThisJoin || pending != null || password.isEmpty()) return;

            String text = ChatPacketUtil.extractText(event.packet());
            if (text.isEmpty()) return;

            // Register wins when a server prints both, since an unregistered account cannot log in.
            if (REGISTER.matcher(text).find()) {
                pending = "register " + password.getValue() + " " + password.getValue();
            } else if (LOGIN.matcher(text).find()) {
                pending = "login " + password.getValue();
            } else {
                return;
            }

            handledThisJoin = true;
            countdown = (int) delay.getValue().floatValue();
        })));

        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (pending == null) return;
            if (countdown-- > 0) return;

            if (mc.player != null) mc.player.connection.sendCommand(pending);
            pending = null;
            countdown = -1;
        })));
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        handledThisJoin = false;
        pending = null;
        countdown = -1;
    }
}
