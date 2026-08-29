package com.fest.visuals.client.features.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;

public class AnyNameArgument implements ArgumentType<String> {
    public static AnyNameArgument create() {
        return new AnyNameArgument();
    }

    @Override
    public String parse(StringReader reader) {
        try {
            return reader.readString();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Minecraft client = Minecraft.getInstance();
        List<String> playerNames = new ArrayList<>();
        if (client.getConnection() != null) {
            client.getConnection().getOnlinePlayers().forEach(entry -> playerNames.add(entry.getProfile().name()));
        }
        return SharedSuggestionProvider.suggest(playerNames, builder);
    }

    @Override
    public Collection<String> getExamples() {
        Minecraft client = Minecraft.getInstance();
        List<String> examples = new ArrayList<>();

        if (client.getConnection() != null) {
            client.getConnection().getOnlinePlayers().stream().limit(5).forEach(entry -> examples.add(entry.getProfile().name()));
        }

        if (examples.isEmpty()) {
            examples.addAll(List.of("Evelina", "Donya"));
        }

        return examples;
    }
}
