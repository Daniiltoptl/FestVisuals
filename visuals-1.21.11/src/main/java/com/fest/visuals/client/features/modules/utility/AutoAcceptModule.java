package com.fest.visuals.client.features.modules.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.PacketEvent;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.module.setting.StringSetting;
import com.fest.visuals.api.system.configs.FriendManager;
import com.fest.visuals.api.utils.other.ChatPacketUtil;

/**
 * Accepts teleport and clan invitations from server chat.
 *
 * <p>Wording differs between servers, so the patterns cover the phrasings used by the popular
 * Russian anarchy servers (FunTime, HolyWorld, ReallyWorld) alongside the English EssentialsX
 * defaults, and the commands themselves are editable settings — a server that words it
 * differently only needs the command changed, not the module rewritten.
 *
 * <p>The reply is delayed by a few ticks: several servers drop a command that arrives in the same
 * tick as the message that asked for it.
 */
@ModuleRegister(name = "Auto Accept", desc = "Принимает телепорт и приглашения в клан", category = Category.OTHER)
public class AutoAcceptModule extends Module {
    @Getter private static final AutoAcceptModule instance = new AutoAcceptModule();

    public final ModeSetting from = new ModeSetting("От кого").value("От всех").values("От всех", "Только друзья");
    public final BooleanSetting teleports = new BooleanSetting("Телепорты").value(true);
    public final BooleanSetting clans = new BooleanSetting("Приглашения в клан").value(false);
    public final SliderSetting delay = new SliderSetting("Задержка (тики)").value(10f).range(0f, 60f).step(1f);

    public final StringSetting teleportCommand = new StringSetting("Команда телепорта")
            .value("tpaccept").placeholder("tpaccept").setVisible(teleports::getValue);
    public final StringSetting clanCommand = new StringSetting("Команда клана")
            .value("clan accept").placeholder("clan accept").setVisible(clans::getValue);

    /**
     * Teleport requests. Covers "X хочет телепортироваться к вам", "Игрок X отправил запрос на
     * телепортацию", "X просит(ся) телепортироваться" and the EssentialsX English wording.
     */
    private static final Pattern TELEPORT = Pattern.compile(
            "(?iu)(запрос(?:ил)?\\s+(?:на\\s+)?телепорт|хочет\\s+телепортироваться|телепортироваться\\s+к\\s+вам"
                    + "|просит\\s+телепорт|has\\s+requested\\s+to\\s+teleport|requests?\\s+to\\s+teleport)");

    /** Clan/guild invitations: "X пригласил вас в клан", "приглашение в клан", "invited you to". */
    private static final Pattern CLAN = Pattern.compile(
            "(?iu)(пригласил[аи]?\\s+вас\\s+в\\s+(?:клан|гильди|группу)|приглашени[ея]\\s+в\\s+(?:клан|гильди)"
                    + "|invited\\s+you\\s+to\\s+(?:the\\s+)?(?:clan|guild|party))");

    /** A Minecraft name, optionally carrying a server rank prefix such as "[VIP] Nick". */
    private static final Pattern NAME = Pattern.compile("(?<![\\w\\[])([A-Za-z0-9_]{3,16})(?![\\w\\]])");

    private String pending;
    private int countdown = -1;

    public AutoAcceptModule() {
        addSettings(from, teleports, clans, delay, teleportCommand, clanCommand);
    }

    @Override
    public void onEvent() {
        addEvents(PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive() || mc.player == null) return;

            String text = ChatPacketUtil.extractText(event.packet());
            if (text.isEmpty() || pending != null) return;

            boolean teleport = teleports.getValue() && TELEPORT.matcher(text).find();
            boolean clan = clans.getValue() && CLAN.matcher(text).find();
            if (!teleport && !clan) return;

            if (!allowed(text)) return;

            String command = teleport ? teleportCommand.getValue() : clanCommand.getValue();
            if (command.isEmpty()) return;

            pending = command;
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
        pending = null;
        countdown = -1;
    }

    /** In friends-only mode the message has to name somebody from the friend list. */
    private boolean allowed(String text) {
        if (from.is("От всех")) return true;

        Matcher matcher = NAME.matcher(text);
        while (matcher.find()) {
            if (FriendManager.getInstance().contains(matcher.group(1))) return true;
        }
        return false;
    }
}
