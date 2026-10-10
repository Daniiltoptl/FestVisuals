package com.fest.visuals.api.utils.other;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import com.fest.visuals.api.system.backend.SharedClass;

@UtilityClass
@Accessors(fluent = true)
public class NetworkUtil {
    @Getter
    private boolean silentPacket = false;
    
    public void sendPacket(Packet<?> packet) {
        assert player() != null;

        player().connection.send(packet);
    }

    public void sendPacket(PredictiveAction packet) {
        assert player() != null;

        try (var ignored = Minecraft.getInstance().level.blockStatePredictionHandler.startPredicting()) {
            int sequence = Minecraft.getInstance().level.blockStatePredictionHandler.currentSequence();
            player().connection.send(packet.predict(sequence));
        }
    }

    public void sendSilentPacket(Packet<?> packet) {
        try {
            silentPacket = true;
            sendPacket(packet);
        } finally {
            silentPacket = false;
        }
    }

    public void sendSilentPacket(PredictiveAction packet) {
        try {
            silentPacket = true;
            sendPacket(packet);
        } finally {
            silentPacket = false;
        }
    }

    private LocalPlayer player() {
        return SharedClass.player();
    }
}