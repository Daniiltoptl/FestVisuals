package com.fest.visuals.client.features.modules.render;

import com.fest.visuals.api.event.EventListener;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render3DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.pipeline.FestLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.minecraft.client.player.AbstractClientPlayer;
import org.joml.Matrix4f;

import java.awt.Color;

@ModuleRegister(name = "ChinaHat", desc = "Р В РЎв„ўР В РЎвЂР РЋРІР‚С™Р В Р’В°Р В РІвЂћвЂ“Р РЋР С“Р В РЎвЂќР В Р’В°Р РЋР РЏ Р РЋРІвЂљВ¬Р В Р’В»Р РЋР РЏР В РЎвЂ”Р В Р’В°", category = Category.RENDER)
public class ChinaHatModule extends Module {
    @Getter private static final ChinaHatModule instance = new ChinaHatModule();

    private final BooleanSetting onSelf = new BooleanSetting("Р В РЎСљР В Р’В° Р РЋР С“Р В Р’ВµР В Р’В±Р В Р’Вµ").value(true);
    private final BooleanSetting onPlayers = new BooleanSetting("Р В РЎСљР В Р’В° Р В РЎвЂР В РЎвЂ“Р РЋР вЂљР В РЎвЂўР В РЎвЂќР В Р’В°Р РЋРІР‚В¦").value(false);
    private final SliderSetting height = new SliderSetting("Р В РІР‚в„ўР РЋРІР‚в„–Р РЋР С“Р В РЎвЂўР РЋРІР‚С™Р В Р’В°").value(0.3f).range(0.1f, 1.0f).step(0.05f);
    private final SliderSetting radius = new SliderSetting("Р В Р’В Р В Р’В°Р В РўвЂР В РЎвЂР РЋРЎвЂњР РЋР С“").value(0.8f).range(0.3f, 2.0f).step(0.05f);
    private final ColorSetting color = new ColorSetting("Р В Р’В¦Р В Р вЂ Р В Р’ВµР РЋРІР‚С™").value(new Color(255, 120, 0, 120));

    public ChinaHatModule() {
        addSettings(onSelf, onPlayers, height, radius, color);
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            PoseStack matrixStack = event.matrixStack();
            RenderUtil.WORLD.startRender(matrixStack);

            for (AbstractClientPlayer player : mc.level.players()) {
                if (player.getUUID().equals(mc.player.getUUID())) {
                    if (!onSelf.getValue() || mc.options.getCameraType().isFirstPerson()) continue;
                } else {
                    if (!onPlayers.getValue()) continue;
                }
                renderHat(event, matrixStack, player, event.partialTicks());
            }

            RenderUtil.WORLD.endRender(matrixStack);
        }));

        addEvents(renderEvent);
    }

    private void renderHat(Render3DEvent.Render3DEventData event, PoseStack matrixStack, AbstractClientPlayer player, float tickDelta) {
        double x = player.xOld + (player.getX() - player.xOld) * tickDelta - event.cameraState().pos.x;
        double y = player.yOld + (player.getY() - player.yOld) * tickDelta - event.cameraState().pos.y;
        double z = player.zOld + (player.getZ() - player.zOld) * tickDelta - event.cameraState().pos.z;

        matrixStack.pushPose();
        matrixStack.translate(x, y + player.getBbHeight() + (player.isCrouching() ? -0.2 : 0.0), z);

        Matrix4f matrix = matrixStack.last().pose();
        VertexConsumer buffer = RenderUtil.WORLD.buffer(FestLayers.QUADS);
        VertexConsumer lineBuffer = RenderUtil.WORLD.buffer(FestLayers.DEBUG_LINES);

        float r = radius.getValue();
        float h = height.getValue();
        int segments = 30;
        int clr = color.getValue().getRGB();
        int lineClr = ColorUtil.setAlpha(color.getValue(), 255).getRGB();

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (i * Math.PI * 2 / segments);
            float angle2 = (float) ((i + 1) * Math.PI * 2 / segments);

            float c1 = (float) Math.cos(angle1) * r;
            float s1 = (float) Math.sin(angle1) * r;
            float c2 = (float) Math.cos(angle2) * r;
            float s2 = (float) Math.sin(angle2) * r;

            buffer.addVertex(matrix, 0, h, 0).setColor(clr);
            buffer.addVertex(matrix, 0, h, 0).setColor(clr);
            buffer.addVertex(matrix, c1, 0, s1).setColor(clr);
            buffer.addVertex(matrix, c2, 0, s2).setColor(clr);

            lineBuffer.addVertex(matrix, c1, 0, s1).setColor(lineClr);
            lineBuffer.addVertex(matrix, c2, 0, s2).setColor(lineClr);
            lineBuffer.addVertex(matrix, 0, h, 0).setColor(lineClr);
            lineBuffer.addVertex(matrix, c1, 0, s1).setColor(lineClr);
        }

        matrixStack.popPose();
    }
}