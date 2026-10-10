package com.fest.visuals.inject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MixinMinecraftClientAccessor {
    @Mutable
    @Accessor("user")
    void festvisuals$setUser(User user);
}
