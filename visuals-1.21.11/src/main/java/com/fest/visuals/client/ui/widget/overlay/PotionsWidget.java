package com.fest.visuals.client.ui.widget.overlay;

import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.other.TextUtil;
import com.fest.visuals.client.features.modules.hud.PotionsHudModule;
import com.fest.visuals.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;

public class PotionsWidget extends ContainerWidget {
    public PotionsWidget() {
        super(3f, 120f);
    }

    @Override
    public String getName() {
        return "Potions";
    }

    private static final Identifier[] BAD_EFFECTS = {
            Identifier.fromNamespaceAndPath("minecraft", "wither"),
            Identifier.fromNamespaceAndPath("minecraft", "poison"),
            Identifier.fromNamespaceAndPath("minecraft", "slowness"),
            Identifier.fromNamespaceAndPath("minecraft", "weakness"),
            Identifier.fromNamespaceAndPath("minecraft", "mining_fatigue"),
            Identifier.fromNamespaceAndPath("minecraft", "nausea"),
            Identifier.fromNamespaceAndPath("minecraft", "blindness"),
            Identifier.fromNamespaceAndPath("minecraft", "hunger"),
            Identifier.fromNamespaceAndPath("minecraft", "levitation"),
            Identifier.fromNamespaceAndPath("minecraft", "unluck")
    };

    private static final Identifier[] COOL_EFFECTS = {
            Identifier.fromNamespaceAndPath("minecraft", "speed"),
            Identifier.fromNamespaceAndPath("minecraft", "strength"),
            Identifier.fromNamespaceAndPath("minecraft", "regeneration")
    };

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        Map<String, ContainerElement.ColoredString> map = new HashMap<>();
        PotionsHudModule cfg = PotionsHudModule.getInstance();
        boolean showAmp = cfg.showAmplifier.getValue();
        boolean showDur = cfg.showDuration.getValue();
        for (MobEffectInstance effect : mc.player.getActiveEffectsMap().values()) {
            Identifier id = effect.getEffect().unwrapKey().get().identifier();

            Color textColor = UIColors.textColor();
            Color effectColor = isBadEffect(id) ? ColorUtil.flashingColor(UIColors.negativeColor(), textColor) :
                                isCoolEffect(id) ? ColorUtil.flashingColor(UIColors.positiveColor(), textColor) :
                                UIColors.textColor();

            String level = showAmp && effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
            String name = Language.getInstance().getOrDefault(effect.getDescriptionId()) + level;
            String durationText = showDur ? TextUtil.getDurationText(effect.getDuration()) : "";
            map.put(name, new ContainerElement.ColoredString(durationText, effectColor));
        }
        return map;
    }

    @Override
    public float fontMul() { return PotionsHudModule.getInstance().fontScale.getValue(); }

    @Override
    protected boolean alwaysVisible() { return PotionsHudModule.getInstance().alwaysShow.getValue(); }

    private boolean isBadEffect(Identifier id) {
        for (Identifier badId : BAD_EFFECTS) {
            if (badId.equals(id)) return true;
        }
        return false;
    }

    private boolean isCoolEffect(Identifier id) {
        for (Identifier coolId : COOL_EFFECTS) {
            if (coolId.equals(id)) return true;
        }
        return false;
    }
}
