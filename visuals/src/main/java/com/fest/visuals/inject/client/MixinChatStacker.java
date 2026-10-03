package com.fest.visuals.inject.client;

import com.fest.visuals.client.features.modules.utility.ChatHelperModule;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.List;

@Mixin(ChatComponent.class)
public class MixinChatStacker {
    @Shadow(aliases = {"allMessages"}) private List<?> allMessages;
    @Shadow(aliases = {"trimmedMessages"}) private List<?> trimmedMessages;

    private String lastMessage = "";
    private int messageCount = 1;

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component modifyMessage(Component message) {
        if (!ChatHelperModule.getInstance().isEnabled() || !ChatHelperModule.getInstance().stackMessages.getValue()) {
            lastMessage = message.getString();
            messageCount = 1;
            return message;
        }

        String current = message.getString();
        if (current.equals(lastMessage)) {
            messageCount++;
            
            if (allMessages != null && !allMessages.isEmpty()) {
                allMessages.remove(0);
            }
            if (trimmedMessages != null && !trimmedMessages.isEmpty()) {
                trimmedMessages.remove(0);
            }

            return message.copy().append(Component.literal(" \u00a7a|x" + messageCount + "|"));
        } else {
            lastMessage = current;
            messageCount = 1;
            return message;
        }
    }
}
