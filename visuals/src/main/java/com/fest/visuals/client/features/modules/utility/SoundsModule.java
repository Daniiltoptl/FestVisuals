package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.utils.other.SoundUtil;

@ModuleRegister(name = "Sounds", desc = "Добавляет клиенту звуки", category = Category.OTHER)
public class SoundsModule extends Module {
    @Getter private static final SoundsModule instance = new SoundsModule();

    public final ModeSetting theme = new ModeSetting("Тема").value("Smooth")
            .values("Smooth", "Celestial", "Nursultan", "Akrien", "Tech", "Blop");

    public SoundsModule() {
        addSettings(theme);
    }

    /** Вызывается из Module#setEnabled при переключении любого другого модуля. */
    public void playToggleSound(boolean enabling) {
        if (!isEnabled()) return;

        SoundUtil.playSound(switch (theme.getValue()) {
            case "Celestial" -> enabling ? SoundUtil.ENABLE_CEL_EVENT : SoundUtil.DISABLE_CEL_EVENT;
            case "Nursultan" -> enabling ? SoundUtil.ENABLE_NU_EVENT : SoundUtil.DISABLE_NU_EVENT;
            case "Akrien" -> enabling ? SoundUtil.ENABLE_AK_EVENT : SoundUtil.DISABLE_AK_EVENT;
            case "Tech" -> enabling ? SoundUtil.ENABLE_TECH_EVENT : SoundUtil.DISABLE_TECH_EVENT;
            case "Blop" -> enabling ? SoundUtil.ENABLE_BLOP_EVENT : SoundUtil.DISABLE_BLOP_EVENT;
            default -> enabling ? SoundUtil.ENABLE_SMOOTH_EVENT : SoundUtil.DISABLE_SMOOTH_EVENT;
        });
    }

    @Override
    public void onEvent() {
    }
}
