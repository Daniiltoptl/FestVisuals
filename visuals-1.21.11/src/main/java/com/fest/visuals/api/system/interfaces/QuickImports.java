package com.fest.visuals.api.system.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.protocol.Packet;
import com.fest.visuals.api.utils.other.NetworkUtil;
import com.fest.visuals.api.utils.other.TextUtil;

public interface QuickImports {
    Minecraft mc = Minecraft.getInstance();

    default void print(String message) {
        TextUtil.sendMessage(message);
    }

    default void sendPacket(PredictiveAction packet) {
        NetworkUtil.sendPacket(packet);
    }
    default void sendSilentPacket(PredictiveAction packet) {
        NetworkUtil.sendSilentPacket(packet);
    }
    default void sendPacket(Packet<?> packet) {
        NetworkUtil.sendPacket(packet);
    }
    default void sendSilentPacket(Packet<?> packet) {
        NetworkUtil.sendSilentPacket(packet);
    }
}
