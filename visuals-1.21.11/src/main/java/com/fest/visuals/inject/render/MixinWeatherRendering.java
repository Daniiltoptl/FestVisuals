package com.fest.visuals.inject.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.fest.visuals.client.features.modules.render.AmbienceModule;

@Mixin(WeatherEffectRenderer.class)
public abstract class MixinWeatherRendering {

    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;getPrecipitationAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation modifyBiomePrecipitation(Biome.Precipitation original) {
        if (com.fest.visuals.client.features.modules.render.RemovalsModule.getInstance().isWeather()) {
            return Biome.Precipitation.NONE;
        }

        var moduleOverrideWeather = AmbienceModule.getInstance();
        if (moduleOverrideWeather.isEnabled() && moduleOverrideWeather.weather.is(AmbienceModule.Weather.SNOWY)) {
            return Biome.Precipitation.SNOW;
        }

        return original;
    }

}
