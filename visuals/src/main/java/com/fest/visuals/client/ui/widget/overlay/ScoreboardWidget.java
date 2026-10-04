package com.fest.visuals.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Comparator;
import java.util.List;
import java.awt.Color;

import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.McText;
import com.fest.visuals.client.features.modules.hud.ScoreboardHudModule;
import com.fest.visuals.client.ui.widget.Widget;

import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class ScoreboardWidget extends Widget {
    private static final int MAX_ROWS = 15;

    public ScoreboardWidget() {
        super(300f, 60f);
    }

    @Override
    public float fontMul() {
        return ScoreboardHudModule.getInstance().fontScale.getValue();
    }

    @Override
    public String getName() { return "Scoreboard"; }

    public static List<PlayerScoreEntry> entries(Scoreboard scoreboard, Objective objective) {
        return scoreboard.listPlayerScores(objective).stream()
                .filter(entry -> !entry.isHidden())
                .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed())
                .limit(MAX_ROWS)
                .toList();
    }

    public static Objective sidebar() {
        if (mc.level == null) return null;
        return mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    @Override
    public void render(PoseStack matrixStack) {
        Objective objective = com.fest.visuals.client.features.modules.render.RemovalsModule.getInstance().isScoreboard() ? null : sidebar();
        if (objective == null || mc.level == null) {
            getDraggable().setWidth(0f);
            getDraggable().setHeight(0f);
            return;
        }

        Scoreboard scoreboard = mc.level.getScoreboard();
        List<PlayerScoreEntry> rows = entries(scoreboard, objective);

        ScoreboardHudModule cfg = ScoreboardHudModule.getInstance();

        float pad = getGap() * 2f;
        float fontSize = scaled(8f * fontMul());
        float rowGap = scaled(2f);
        float titleSize = fontSize;

        Component title = objective.getDisplayName();
        boolean showScores = cfg.numbers.getValue();

        float width = McText.getWidth(title, titleSize);
        for (PlayerScoreEntry entry : rows) {
            Component name = displayName(scoreboard, entry);
            float line = McText.getWidth(name, fontSize);
            if (showScores) {
                Component scoreComp = entry.display();
                if (scoreComp == null) scoreComp = Component.literal(Integer.toString(entry.value()));
                line += getGap() * 3f + McText.getWidth(scoreComp, fontSize);
            }
            width = Math.max(width, line);
        }
        width += pad * 2f;

        float headerHeight = titleSize + pad * 1.4f;
        float bodyHeight = rows.isEmpty() ? 0f : rows.size() * (fontSize + rowGap) - rowGap;
        float height = headerHeight + bodyHeight + pad;

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        getDraggable().setWidth(width);
        getDraggable().setHeight(height);

        if (cfg.background.getValue()) {
            RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, getGap() * 2f, UIColors.widgetBlur());
        }

        float titleX = x + width / 2f - McText.getWidth(title, titleSize) / 2f;
        McText.drawText(matrixStack, title, titleX, y + (headerHeight - titleSize) / 2f, titleSize);

        float rowY = y + headerHeight;
        for (PlayerScoreEntry entry : rows) {
            Component name = displayName(scoreboard, entry);
            McText.drawText(matrixStack, name, x + pad, rowY, fontSize);

            if (showScores) {
                Component scoreComp = entry.display();
                if (scoreComp == null) scoreComp = Component.literal(Integer.toString(entry.value()));
                float scoreWidth = McText.getWidth(scoreComp, fontSize);
                McText.drawText(matrixStack, scoreComp, x + width - pad - scoreWidth, rowY, fontSize);
            }

            rowY += fontSize + rowGap;
        }
    }

    private static Component displayName(Scoreboard scoreboard, PlayerScoreEntry entry) {
        PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
        return PlayerTeam.formatNameForTeam(team, entry.ownerName());
    }
}
