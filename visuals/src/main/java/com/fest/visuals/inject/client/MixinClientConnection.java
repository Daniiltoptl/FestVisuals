package com.fest.visuals.inject.client;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.event.events.client.PacketEvent;
import com.fest.visuals.api.utils.other.NetworkUtil;

@Mixin(Connection.class)
public class MixinClientConnection {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void sendPackets(Packet<?> packet, @Nullable ChannelFutureListener callbacks, boolean flush, CallbackInfo callbackInfo) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null ||
                NetworkUtil.silentPacket()) return;

        if (PacketEvent.getInstance().call(new PacketEvent.PacketEventData(packet, PacketEvent.PacketEventData.PacketType.SEND))) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
    private static void receivePackets(Packet<?> packet, PacketListener listener, CallbackInfo callbackInfo) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return;

        if (packet instanceof ClientboundBundlePacket bundlePacket) {
            for (Packet<?> innerPacket : bundlePacket.subPackets()) {
                if (handleSinglePacket(innerPacket)) {
                    callbackInfo.cancel();
                    return;
                }
            }
        } else {
            if (handleSinglePacket(packet)) {
                callbackInfo.cancel();
            }
        }
    }

    @Unique
    private static boolean handleSinglePacket(Packet<?> packet) {
        return PacketEvent.getInstance().call(new PacketEvent.PacketEventData(packet, PacketEvent.PacketEventData.PacketType.RECEIVE));
    }
}
