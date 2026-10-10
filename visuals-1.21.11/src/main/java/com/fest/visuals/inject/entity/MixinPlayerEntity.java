package com.fest.visuals.inject.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.fest.visuals.api.event.events.player.world.AttackEvent;
import com.fest.visuals.api.event.events.player.world.CritEvent;
import com.fest.visuals.api.event.events.player.move.TravelEvent;
import com.fest.visuals.api.system.backend.SharedClass;

@Mixin(Player.class)
public abstract class MixinPlayerEntity extends MixinLivingEntity {
    @Inject(method = "attack", at = @At("HEAD"))
    public void attackEventHook(Entity target, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        if ((Object) this == SharedClass.player()) {
            AttackEvent.getInstance().call(new AttackEvent.AttackEventData(target));
        }
    }

    /** The crit decision is private and made mid-attack; this reports it the moment it is made. */
    @Inject(method = "canCriticalAttack", at = @At("RETURN"))
    private void festvisuals$crit(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || SharedClass.player() == null) return;
        if ((Object) this != SharedClass.player()) return;

        CritEvent.getInstance().call(new CritEvent.CritEventData(target));
    }

    @Inject(method = "travel", at = @At("HEAD"))
    public void travelHook(Vec3 movementInput, CallbackInfo ci) {
        if ((Object) this != SharedClass.player()) {
            return;
        }

        TravelEvent.getInstance().call();
    }
}
