package com.fest.visuals.inject.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface MixinLivingEntityRendererInvoker {
    @Invoker("addLayer")
    boolean festvisuals$addLayer(RenderLayer<?, ?> layer);
}
