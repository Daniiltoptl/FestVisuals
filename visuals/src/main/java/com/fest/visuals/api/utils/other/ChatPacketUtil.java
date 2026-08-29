package com.fest.visuals.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

/**
 * Достаёт читаемый текст из системных чат-пакетов (сообщения сервера,
 * а не сообщения от других игроков), чтобы Utility-модули могли искать
 * в них ключевые фразы (запросы на телепорт, логин/регистрацию и т.д.).
 *
 * NOTE: имя класса пакета (`ClientboundSystemChatPacket`) соответствует
 * стандартному Mojmap для современных версий Minecraft. Если сборка под
 * вашу версию использует другое имя/структуру — поправьте здесь.
 */
@UtilityClass
public class ChatPacketUtil {
    public String extractText(Packet<?> packet) {
        if (packet instanceof ClientboundSystemChatPacket systemChat) {
            Component content = systemChat.content();
            return content == null ? "" : content.getString();
        }
        return "";
    }
}
