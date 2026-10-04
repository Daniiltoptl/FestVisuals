package com.fest.visuals.api.utils.render;

import java.awt.Color;
import java.time.Duration;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import com.fest.visuals.api.utils.render.fonts.Fonts;
import org.joml.Matrix3x2fStack;

/**
 * Draws with Minecraft's own font while keeping the call shape of the client's MSDF fonts, so a
 * widget can switch between the two by swapping the class it calls.
 *
 * <p>Vanilla text goes through the GUI pipeline rather than the client renderer, which means it
 * lands on top of anything the client draws in the normal queue. A widget mixing the two has to
 * put its own shapes on the backdrop queue, otherwise the panel covers its own labels.
 *
 * <p>The {@code PoseStack} arguments are ignored — vanilla text is positioned through the GUI
 * matrix stack instead. They stay in the signatures so call sites read the same either way.
 */
@UtilityClass
public class McText {
    /** Vanilla glyphs are 8px tall including the descender; sizes elsewhere are cap heights. */
    private final float BASE_HEIGHT = 8f;

    /** The extractor for the frame being built, set by the widget manager before it draws. */
    @Setter private GuiGraphicsExtractor context;

    
    public float getWidth(Component text, float size) {
        if (text == null) return 0f;
        if (context == null) return Fonts.PS_MEDIUM.getWidth(text, size);
        return Minecraft.getInstance().font.width(text) * (size / BASE_HEIGHT);
    }

    public float getWidth(String text, float size) {
        if (text == null || text.isEmpty()) return 0f;
        if (context == null) return Fonts.PS_MEDIUM.getWidth(text, size);

        return Minecraft.getInstance().font.width(text) * (size / BASE_HEIGHT);
    }

    public float getHeight(float size) {
        return size;
    }

    
    public void drawText(PoseStack matrixStack, Component text, float x, float y, float size) {
        if (text == null) return;
        if (context == null) {
            Fonts.PS_MEDIUM.drawText(matrixStack, text, x, y, size, 0f);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        float scale = size / BASE_HEIGHT;

        Matrix3x2fStack pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        // By default, draw the component without a forced color override, preserving its internal colors
        context.text(mc.font, text, 0, 0, 0xFFFFFFFF);
        pose.popMatrix();
    }

    public void drawText(PoseStack matrixStack, String text, float x, float y, float size, Color color) {
        if (text == null || text.isEmpty()) return;

        // Without an extractor there is no GUI pass to draw into. Fall back to the client font
        // rather than dropping the text, which is how a missing extractor used to look.
        if (context == null) {
            Fonts.PS_MEDIUM.drawText(matrixStack, text, x, y, size, color);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        float scale = size / BASE_HEIGHT;

        Matrix3x2fStack pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        context.text(mc.font, text, 0, 0, color.getRGB());
        pose.popMatrix();
    }

    public void drawCenteredText(PoseStack matrixStack, String text, float x, float y, float size, Color color) {
        drawText(matrixStack, text, x - getWidth(text, size) / 2f, y, size, color);
    }

    /**
     * Marquee: text wider than the box slides back and forth so the whole title can be read.
     * Short text is drawn plainly, without the pause the animation would otherwise introduce.
     */
    public void drawWrap(PoseStack matrixStack, String text, float x, float y, float width, float size,
                         Color color, float offset, Duration cycle, Duration pause) {
        if (context == null || text == null || text.isEmpty()) return;

        float textWidth = getWidth(text, size);
        if (textWidth <= width) {
            drawText(matrixStack, text, x, y, size, color);
            return;
        }

        long period = Math.max(1L, cycle.toMillis() + pause.toMillis() * 2L);
        float phase = (System.currentTimeMillis() % period) / (float) period;
        // Ease into a full there-and-back sweep, holding still at both ends.
        float travel = textWidth - width;
        float shift = (float) ((1.0 - Math.cos(phase * Math.PI * 2.0)) / 2.0) * travel;

        Matrix3x2fStack pose = context.pose();
        pose.pushMatrix();
        // Clip so the overflowing half never spills past the box it belongs to.
        context.enableScissor(Math.round(x), Math.round(y - size), Math.round(x + width), Math.round(y + size * 2f));
        drawText(matrixStack, text, x - shift, y, size, color);
        context.disableScissor();
        pose.popMatrix();
    }
}
