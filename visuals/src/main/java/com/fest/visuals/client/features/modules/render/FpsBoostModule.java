package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import com.fest.visuals.api.utils.render.AdaptiveBudget;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

/**
 * Skips rendering what is far away and cheap to lose: chests, signs and banners past a distance,
 * falling sand and TNT, dropped items, and the extra copies vanilla draws for an item stack.
 *
 * <p>On by default — this is where the FPS goes on busy servers — but every cut is visible, so
 * each one has its own switch and distance.
 */
@ModuleRegister(name = "FPS Boost", desc = "Не рисует далёкие сундуки, таблички, лут и падающие блоки", category = Category.RENDER)
public class FpsBoostModule extends Module {
    @Getter private static final FpsBoostModule instance = new FpsBoostModule();

    public final BooleanSetting blockEntities = new BooleanSetting("Сундуки, таблички, баннеры").value(true);
    public final SliderSetting blockEntityDistance = new SliderSetting("Дистанция блоков").value(48f).range(16f, 128f).step(4f)
            .setVisible(blockEntities::getValue);
    public final BooleanSetting fallingBlocks = new BooleanSetting("Падающие блоки и TNT").value(true);
    public final BooleanSetting items = new BooleanSetting("Лут на земле").value(true);
    public final SliderSetting itemDistance = new SliderSetting("Дистанция лута").value(32f).range(8f, 96f).step(4f)
            .setVisible(items::getValue);
    public final BooleanSetting singleItemModel = new BooleanSetting("Одна модель на стак").value(true);

    public FpsBoostModule() {
        addSettings(blockEntities, blockEntityDistance, fallingBlocks, items, itemDistance, singleItemModel);
        setEnabled(true, true);
    }

    @Override
    public void onEvent() {
    }

    public double blockEntityCutoffSqr() {
        return isEnabled() && blockEntities.getValue() ? square(blockEntityDistance.getValue() * AdaptiveBudget.getInstance().factor()) : Double.MAX_VALUE;
    }

    public double fallingBlockCutoffSqr() {
        return isEnabled() && fallingBlocks.getValue() ? square(48.0 * AdaptiveBudget.getInstance().factor()) : Double.MAX_VALUE;
    }

    public double itemCutoffSqr() {
        return isEnabled() && items.getValue() ? square(itemDistance.getValue() * AdaptiveBudget.getInstance().factor()) : Double.MAX_VALUE;
    }

    public boolean singleItemModel() {
        return isEnabled() && singleItemModel.getValue();
    }

    private static double square(double value) {
        return value * value;
    }
}
