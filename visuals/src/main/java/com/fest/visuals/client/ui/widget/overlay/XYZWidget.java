package com.fest.visuals.client.ui.widget.overlay;

import com.fest.visuals.api.utils.render.fonts.Icons;
import com.fest.visuals.client.features.modules.hud.StatsHudModule;
import com.fest.visuals.client.ui.widget.InformationWidget;

public class XYZWidget extends InformationWidget {
    @Override
    public String getName() {
        return "XYZ";
    }

    public XYZWidget() {
        super(30f, 120f);
    }

    @Override
    public String getValue() {
        String x = String.format("%.1f", mc.player.getX());
        String y = String.format("%.1f", mc.player.getY());
        String z = String.format("%.1f", mc.player.getZ());
        return x + ", " + y + ", " + z;
    }

    @Override
    public Icons getIcon() {
        return null;
    }

    @Override
    public float fontMul() { return StatsHudModule.getInstance().fontScale.getValue(); }
}
