package com.fest.visuals.inject.client;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {
    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;onlineMode()Z"))
    private boolean festvisuals$forceShowFaces(ClientPacketListener instance) {
        return true; // Always return true to force rendering player heads in the TAB list, even on cracked servers.
    }
}
