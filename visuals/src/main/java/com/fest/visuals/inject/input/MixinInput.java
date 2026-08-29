package com.fest.visuals.inject.input;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import com.fest.visuals.api.system.interfaces.IPlayerInput;

@Mixin(ClientInput.class)
public abstract class MixinInput implements IPlayerInput {
    @Unique
    protected Input untransformed = Input.EMPTY;

    @Override
    public Input evelina$getUntransformed() {
        return untransformed;
    }
}
