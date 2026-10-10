package com.fest.visuals.api.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.client.features.commands.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandManager {
    @Getter private static final CommandManager instance = new CommandManager();

    private final CommandDispatcher<SharedSuggestionProvider> dispatcher;
    private final ClientSuggestionProvider source;

    public CommandManager() {
        this.dispatcher = new CommandDispatcher<>();
        this.source = new ClientSuggestionProvider(null, Minecraft.getInstance(), net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
    }

    private final List<Command> commands = new ArrayList<>();

    public void load() {
        register(
                new CommandConfig(), new CommandFriend(), new CommandGps()
        );
    }

    public void register(Command... commands) {
        for (Command command : commands) {
            command.register(dispatcher);
            this.commands.add(command);
        }
    }

    public String getPrefix() {
        return ".";
    }

    public void executeCommands(String message, CallbackInfo ci) {
        if (message.startsWith(getPrefix())) {
            try {
                getDispatcher().execute(message.substring(getPrefix().length()), getSource());
            } catch (CommandSyntaxException ignored) {

            }

            ci.cancel();
        }
    }
}
