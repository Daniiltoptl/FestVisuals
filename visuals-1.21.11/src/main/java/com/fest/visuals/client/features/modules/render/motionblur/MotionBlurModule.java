package com.fest.visuals.client.features.modules.render.motionblur;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

@ModuleRegister(name = "Motion Blur", desc = "Смазывание кадров при движении", category = Category.RENDER)
public class MotionBlurModule extends Module {
    @Getter private static final MotionBlurModule instance = new MotionBlurModule();
    public final ShaderMotionBlur shader;

    @Getter public final SliderSetting strength = new SliderSetting("Strength").value(-0.8f)
            .range(-2f, 2f).step(0.1f)
            .onAction(() -> setMotionBlurStrength(getStrength().getValue()));
    public final BooleanSetting useRRC = new BooleanSetting("Use refresh rate scaling").value(true);


    public MotionBlurModule() {
        shader = new ShaderMotionBlur(this);
        shader.registerShaderCallbacks();
        addSettings(strength, useRRC);
    }

    @Override
    public void onEvent() {

    }

    private void setMotionBlurStrength(float strength) {
        shader.updateBlurStrength(strength);
    }

    public enum BlurAlgorithm {BACKWARDS, CENTERED}
    public static BlurAlgorithm blurAlgorithm = BlurAlgorithm.CENTERED;
}
