package com.fest.visuals.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Comparator;
import java.util.List;

import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.client.features.modules.hud.ScoreboardHudModule;

import java.awt.Color;
import com.fest.visuals.client.ui.widget.Widget;

import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Draggable replacement for the vanilla sidebar scoreboard.
 *
 * <p>The scoreboard is redrawn rather than translated: vanilla pins it to the right edge with its
 * own anchor maths, so moving it by an offset would leave the drag box and the visible panel out
 * of step. The vanilla draw is cancelled in {@code MixinInGameHud} while this widget is on.
 */
public class ScoreboardWidget extends Widget {
    /** Vanilla shows at most this many rows. */
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

    /** Sidebar entries, highest score first, hidden ones dropped. */
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

        String title = objective.getDisplayName().getString().replaceAll("(?i)\\u00A7.", "");
        boolean showScores = cfg.numbers.getValue();

        // Width follows the widest line, so long team names are not clipped.
        float width = getMediumFont().getWidth(title, titleSize);
        for (PlayerScoreEntry entry : rows) {
            String name = displayName(scoreboard, entry);
            float line = getMediumFont().getWidth(name, fontSize);
            if (showScores) {
                line += getGap() * 3f + getMediumFont().getWidth(String.valueOf(entry.value()), fontSize);
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

        getMediumFont().drawCenteredText(matrixStack, title, x + width / 2f,
                y + (headerHeight - titleSize) / 2f, titleSize, UIColors.textColor());

        Color scoreColor = cfg.vanillaNumbers.getValue()
                ? new Color(255, 85, 85)
                : ColorUtil.setAlpha(UIColors.primary(), 255);

        float rowY = y + headerHeight;
        for (PlayerScoreEntry entry : rows) {
            String name = displayName(scoreboard, entry);
            getMediumFont().drawText(matrixStack, name, x + pad, rowY, fontSize, UIColors.textColor());

            if (showScores) {
                String score = String.valueOf(entry.value());
                float scoreWidth = getMediumFont().getWidth(score, fontSize);
                getMediumFont().drawText(matrixStack, score, x + width - pad - scoreWidth, rowY, fontSize, scoreColor);
            }

            rowY += fontSize + rowGap;
        }
    }

    /** Applies the owner's team prefix, suffix and colour, the way vanilla does. */
    private static String displayName(Scoreboard scoreboard, PlayerScoreEntry entry) {
        PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
        return PlayerTeam.formatNameForTeam(team, entry.ownerName()).getString().replaceAll("(?i)\\u00A7.", "");
    }
}
