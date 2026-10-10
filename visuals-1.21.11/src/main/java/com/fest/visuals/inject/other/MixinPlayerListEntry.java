package com.fest.visuals.inject.other;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.fest.visuals.client.features.commands.CommandSkin;

@Mixin(PlayerInfo.class)
public class MixinPlayerListEntry {
    @ModifyReturnValue(method = "getSkin", at = @At("RETURN"))
    private PlayerSkin skinTexturesHook(PlayerSkin original) {
        var customSkin = CommandSkin.getCustomSkinTextures();
        var player = Minecraft.getInstance().player;
        if (player != null) {
            if (customSkin != null) {
                var playerListEntry = player.getPlayerInfo();
                if (playerListEntry != null && playerListEntry.equals(this)) {
                    original =  customSkin.get();
                }
            }
        }

        return original;
    }

    @ModifyExpressionValue(method = "createSkinLookup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalPlayer(Ljava/util/UUID;)Z"))
    private static boolean texturesSupplierHook(boolean original) {
        return original || CommandSkin.getCustomSkinTextures() != null;
    }
}
