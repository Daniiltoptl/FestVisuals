package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import net.minecraft.world.entity.HumanoidArm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.RunSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

/**
 * Moves, turns and resizes the held item.
 *
 * <p>The transform is applied by {@link SwingAnimationModule#handleRenderItem} for every item,
 * right after the pose is pushed. It used to sit in one branch of that method, which meant a bow,
 * a crossbow or anything being eaten ignored it entirely.
 */
@ModuleRegister(name = "View Model", desc = "Настройка модели рук и предметов", category = Category.RENDER)
public class ViewModelModule extends Module {
    @Getter private static final ViewModelModule instance = new ViewModelModule();

    public final SliderSetting rightX = new SliderSetting("Правая X").value(0f).range(-2f, 2f).step(0.05f);
    public final SliderSetting rightY = new SliderSetting("Правая Y").value(0f).range(-2f, 2f).step(0.05f);
    public final SliderSetting rightZ = new SliderSetting("Правая Z").value(0f).range(-2f, 2f).step(0.05f);
    public final SliderSetting leftX = new SliderSetting("Левая X").value(0f).range(-2f, 2f).step(0.05f);
    public final SliderSetting leftY = new SliderSetting("Левая Y").value(0f).range(-2f, 2f).step(0.05f);
    public final SliderSetting leftZ = new SliderSetting("Левая Z").value(0f).range(-2f, 2f).step(0.05f);

    public final SliderSetting scale = new SliderSetting("Масштаб").value(1f).range(0.2f, 2.5f).step(0.05f);
    public final SliderSetting rotateX = new SliderSetting("Наклон X").value(0f).range(-180f, 180f).step(1f);
    public final SliderSetting rotateY = new SliderSetting("Наклон Y").value(0f).range(-180f, 180f).step(1f);
    public final SliderSetting rotateZ = new SliderSetting("Наклон Z").value(0f).range(-180f, 180f).step(1f);

    private final RunSetting reset = new RunSetting("Сбросить").value(this::resetPos);

    public ViewModelModule() {
        addSettings(rightX, rightY, rightZ, leftX, leftY, leftZ, scale, rotateX, rotateY, rotateZ, reset);
    }

    @Override
    public void onEvent() {

    }

    /** Applied inside the item pose, so nothing leaks into the rest of the frame. */
    public void apply(PoseStack matrices, HumanoidArm arm) {
        if (!isEnabled()) return;

        if (arm == HumanoidArm.RIGHT) {
            matrices.translate(rightX.getValue().doubleValue(), rightY.getValue().doubleValue(), rightZ.getValue().doubleValue());
        } else {
            matrices.translate(-leftX.getValue().doubleValue(), leftY.getValue().doubleValue(), leftZ.getValue().doubleValue());
        }

        if (rotateX.getValue() != 0f) matrices.mulPose(Axis.XP.rotationDegrees(rotateX.getValue()));
        if (rotateY.getValue() != 0f) matrices.mulPose(Axis.YP.rotationDegrees(rotateY.getValue()));
        if (rotateZ.getValue() != 0f) matrices.mulPose(Axis.ZP.rotationDegrees(rotateZ.getValue()));

        float factor = scale.getValue();
        if (factor != 1f) matrices.scale(factor, factor, factor);
    }

    private void resetPos() {
        rightX.setValue(0f);
        rightY.setValue(0f);
        rightZ.setValue(0f);
        leftX.setValue(0f);
        leftY.setValue(0f);
        leftZ.setValue(0f);
        scale.setValue(1f);
        rotateX.setValue(0f);
        rotateY.setValue(0f);
        rotateZ.setValue(0f);
    }
}
