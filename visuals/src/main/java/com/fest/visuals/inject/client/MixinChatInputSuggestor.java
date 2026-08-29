package com.fest.visuals.inject.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.command.CommandManager;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;

@Mixin(CommandSuggestions.class)
public abstract class MixinChatInputSuggestor {
    @Final
    @Shadow
    EditBox input;
    @Shadow
    boolean keepSuggestions;
    @Shadow
    private ParseResults<SharedSuggestionProvider> currentParse;
    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow
    private CommandSuggestions.SuggestionsList suggestions;

    @Shadow
    protected abstract void updateUsageInfo(ParseResults<SharedSuggestionProvider> currentParse, Suggestions suggestions);

    @Inject(method = "updateCommandInfo", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z", remap = false), cancellable = true)
    public void onRefresh(CallbackInfo callbackInfo, @Local StringReader reader) {
        if (reader.canRead(CommandManager.getInstance().getPrefix().length()) && reader.getString().startsWith(CommandManager.getInstance().getPrefix(), reader.getCursor())) {
            reader.setCursor(reader.getCursor() + 1);

            if (currentParse == null) {
                currentParse = CommandManager.getInstance().getDispatcher().parse(reader, CommandManager.getInstance().getSource());
            }

            int cursor = input.getCursorPosition();

            if (cursor >= 1 && (suggestions == null || !keepSuggestions)) {
                pendingSuggestions = CommandManager.getInstance().getDispatcher().getCompletionSuggestions(currentParse, cursor);
                pendingSuggestions.thenAccept(suggestionResult -> {
                    if (pendingSuggestions.isDone()) {
                        updateUsageInfo(currentParse, suggestionResult);
                    }
                });
            }

            callbackInfo.cancel();
        }
    }
}
