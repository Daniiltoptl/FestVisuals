package com.fest.visuals.client.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.fest.visuals.api.command.Command;
import com.fest.visuals.api.command.CommandRegister;
import com.fest.visuals.api.system.backend.ClientInfo;
import com.fest.visuals.api.system.backend.SharedClass;
import com.fest.visuals.api.system.configs.ConfigManager;
import com.fest.visuals.client.features.commands.arguments.AnyConfigNameArgument;
import com.fest.visuals.client.features.commands.arguments.StrictlyConfigNameArgument;

import java.util.Collection;
import net.minecraft.commands.SharedSuggestionProvider;

@CommandRegister(name = "cfg")
public class CommandConfig extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("list").executes(context -> {
            Collection<String> configs = ConfigManager.getInstance().getConfigsNames();
            if (configs.isEmpty()) {
                print("Список конфигов пуст.");
            } else {
                print("Список конфигов: " + String.join(", ", configs));
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("remove")
                .then(argument("config", StrictlyConfigNameArgument.create())
                        .executes(context -> {
                            String config = StringArgumentType.getString(context, "config");
                            if (ConfigManager.getInstance().exists(config)) {
                                ConfigManager.getInstance().remove(config);
                                print("Конфиг " + config + " удален.");
                            } else {
                                print("Я не нашла такого конфига.");
                            }
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("load")
                .then(argument("config", StrictlyConfigNameArgument.create())
                        .executes(context -> {
                            String config = StringArgumentType.getString(context, "config");
                            ConfigManager.getInstance().load(config);
                            if (ConfigManager.getInstance().exists(config)) {
                                print("Загружен конфиг: " + config);
                            } else {
                                print("Я не нашла такого конфига T.T");
                            }
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("save")
                .then(argument("config", AnyConfigNameArgument.create())
                        .executes(context -> {
                            String config = StringArgumentType.getString(context, "config");
                            ConfigManager.getInstance().save(config);
                            print("Сохранен конфиг: " + config);
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("dir").executes(context -> {
            if (SharedClass.openFolder(ClientInfo.CONFIG_PATH_MAIN)) {
                print("Открываю папку с конфигами...");
            } else {
                print("Не удалось открыть папку конфигов.");
            }
            return SINGLE_SUCCESS;
        }));
    }
}