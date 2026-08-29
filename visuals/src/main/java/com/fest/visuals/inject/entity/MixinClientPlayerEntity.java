package com.fest.visuals.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.event.events.player.move.MotionEvent;
import com.fest.visuals.api.event.events.player.other.CloseScreenEvent;
import com.fest.visuals.api.event.events.player.other.UpdateEvent;
import com.fest.visuals.api.event.events.player.move.MoveEvent;
import com.fest.visuals.api.event.events.player.move.SprintEvent;
import com.fest.visuals.api.utils.player.DirectionalInput;

@Mixin(LocalPlayer.class)
public class MixinClientPlayerEntity extends AbstractClientPlayer {
    public MixinClientPlayerEntity(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    public ClientInput input;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHook(CallbackInfo ci) {
        UpdateEvent.getInstance().call();
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    public void moveHook(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        MoveEvent.MoveEventData event = new MoveEvent.MoveEventData(movement.x, movement.y, movement.z);
        if (MoveEvent.getInstance().call(event)) {
            super.move(movementType, new Vec3(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

    @Inject(method = "sendPosition", at = @At(value = "HEAD"), cancellable = true)
    private void preMotion(CallbackInfo ci) {
        MotionEvent.MotionEventData event = new MotionEvent.MotionEventData(getX(), getY(), getZ(), getViewYRot(1), getViewXRot(1), onGround());
        if (MotionEvent.getInstance().call(event)) ci.cancel();
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"))
    private boolean sprintEventTick(boolean original) {
        return sprintHook(original);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean sprintEventInput(boolean original) {
        return sprintHook(original);
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void noPushByBlocksHook(double x, double d, CallbackInfo ci) {
        if (false) {}
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z"), require = 0)
    private boolean tickMovementHook(boolean original) {
        if (false) return false;
        return original;
    }

    @Inject(method = "closeContainer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"), cancellable = true)
    private void onCloseHandledScreen(CallbackInfo ci) {
        if (CloseScreenEvent.getInstance().call()) ci.cancel();
    }

    @Unique
    private boolean sprintHook(boolean origin) {
        return SprintEvent.getInstance().call(new SprintEvent.SprintEventData(origin, new DirectionalInput(input)));
    }
}
