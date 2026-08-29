package com.fest.visuals.inject.input;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.fest.visuals.api.event.events.player.other.MovementInputEvent;
import com.fest.visuals.api.event.events.player.move.SprintEvent;
import com.fest.visuals.api.utils.player.DirectionalInput;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends MixinInput {
    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input onTick(Input original) {
        MovementInputEvent.MovementInputEventData movementInputEvent = new MovementInputEvent.MovementInputEventData(original, original.jump(), original.shift(), new DirectionalInput(original));
        MovementInputEvent.getInstance().call(movementInputEvent);

        DirectionalInput directionalInput = movementInputEvent.getDirectionalInput();

        SprintEvent.SprintEventData sprintEvent = new SprintEvent.SprintEventData(original.sprint(), directionalInput);
        SprintEvent.getInstance().call(sprintEvent);

        this.untransformed = new Input(
                directionalInput.isForwards(),
                directionalInput.isBackwards(),
                directionalInput.isLeft(),
                directionalInput.isRight(),
                original.jump(),
                original.shift(),
                sprintEvent.isSprint()
        );

        return new Input(
                directionalInput.isForwards(),
                directionalInput.isBackwards(),
                directionalInput.isLeft(),
                directionalInput.isRight(),
                movementInputEvent.isJump(),
                movementInputEvent.isSneak(),
                sprintEvent.isSprint()
        );
    }
}
