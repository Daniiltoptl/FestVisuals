package com.fest.visuals.api.utils.render.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

@UtilityClass
public class FestTextures {
    public GpuTextureView view(Identifier texture) {
        return Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView();
    }

    public GpuSampler sampler() {
        // FestVisuals's standalone atlases and render targets do not allocate mip
        // levels. Sampling them with mipmaps enabled reads undefined GPU memory
        // on 26.2 (garbled glyphs and tiled framebuffer fragments).
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }

    public GpuSampler nearest() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
    }
}
