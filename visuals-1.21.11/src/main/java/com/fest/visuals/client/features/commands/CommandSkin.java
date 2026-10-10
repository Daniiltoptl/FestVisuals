package com.fest.visuals.client.features.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.fest.visuals.api.auth.ProfileRepository;
import com.fest.visuals.api.auth.UUIDUtils;
import com.fest.visuals.api.command.Command;
import com.fest.visuals.api.command.CommandRegister;
import com.fest.visuals.api.system.configs.ConfigSkin;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.player.PlayerSkin;

@CommandRegister(name = "skin")
public class CommandSkin extends Command {
    public static Supplier<PlayerSkin> customSkinTextures = null;
    public static boolean skinEnabled = false;

    @Override
    public void execute(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("off").executes(context -> {
            if (!skinEnabled) {
                print("Скин уже сброшен!");
            } else {
                customSkinTextures = null;
                skinEnabled = false;
                ConfigSkin.getInstance().save(null);
                print("Скин успешно сброшен!");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("set").then(argument("name", StringArgumentType.string()).executes(context -> {
            String username = StringArgumentType.getString(context, "name");

            try {
                customSkinTextures = createTextureSupplier(username);
                skinEnabled = true;
                ConfigSkin.getInstance().save(username);
                print("Установлен скин: " + username);

            } catch (Exception e) {
                print("Не удалось установить скин :c");
            }

            return SINGLE_SUCCESS;
        })));
    }

    public static Supplier<PlayerSkin> createTextureSupplier(String username) {
        UUID uuid = new ProfileRepository().uuidByName(username);
        if (uuid == null) uuid = UUIDUtils.generateOfflinePlayerUuid(username);

        ProfileResult hui = mc.services().sessionService().fetchProfile(uuid, false);
        GameProfile profile = hui == null ? null : hui.profile();
        if (profile == null) profile = new GameProfile(uuid, username);

        return PlayerInfo.createSkinLookup(profile);
    }

    public static Supplier<PlayerSkin> getCustomSkinTextures() {
        return skinEnabled ? customSkinTextures : null;
    }
}
