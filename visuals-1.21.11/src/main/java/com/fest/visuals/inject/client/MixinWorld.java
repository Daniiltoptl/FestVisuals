package com.fest.visuals.inject.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.fest.visuals.api.system.backend.Choice;
import com.fest.visuals.client.features.modules.render.AmbienceModule;

@Mixin(Level.class)
public class MixinWorld {
    @Inject(method = "getRainLevel", cancellable = true, at = @At("HEAD"))
    private void overrideWeather(float delta, CallbackInfoReturnable<Float> cir) {
        AmbienceModule module = AmbienceModule.getInstance();
        AmbienceModule.Weather weather = Choice.getChoiceByName(module.weather.getValue(), AmbienceModule.Weather.values());
        if (module.isEnabled()) {
            switch (weather) {
                case SUNNY -> cir.setReturnValue(0.0f);
                case RAINY, THUNDER -> cir.setReturnValue(1.0f);
                case SNOWY -> cir.setReturnValue(0.9f);
            }
        }
    }

    @Inject(method = "getThunderLevel", cancellable = true, at = @At("HEAD"))
    private void overrideThunder(float delta, CallbackInfoReturnable<Float> cir) {
        AmbienceModule module = AmbienceModule.getInstance();
        AmbienceModule.Weather weather = Choice.getChoiceByName(module.weather.getValue(), AmbienceModule.Weather.values());
        if (module.isEnabled()) {
            switch (weather) {
                case SUNNY, RAINY, SNOWY -> cir.setReturnValue(0.0f);
                case THUNDER -> cir.setReturnValue(1.0f);
            }
        }
    }

}