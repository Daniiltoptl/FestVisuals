package com.fest.visuals.inject.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.ui.menu.MainMenuTheme;

/**
 * Rebuilds the title screen to the client's design: a centre column of clock, wordmark and two
 * mode cards, a settings row beneath them, and a hold-to-exit chip in the corner.
 *
 * <p>The vanilla widgets are kept and simply moved and re-tagged, so their actions, focus and
 * narration keep working; only the painting is ours. Realms, the compact icon row, the copyright
 * link and the quit button are removed — quitting moved to the corner chip, which needs a
 * press-and-hold a vanilla button cannot express.
 *
 * <p>Extends {@link Screen} so the protected widget-management methods are reachable; the
 * constructor is never invoked, mixin classes are merged into the target.
 */
@Mixin(TitleScreen.class)
public abstract class MixinTitleScreenLinks extends Screen {
    private static final String SINGLEPLAYER_KEY = "menu.singleplayer";
    private static final String MULTIPLAYER_KEY = "menu.multiplayer";
    private static final String OPTIONS_KEY = "menu.options";
    private static final String REALMS_KEY = "menu.online";
    private static final String QUIT_KEY = "menu.quit";

    protected MixinTitleScreenLinks() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void festvisuals$rebuild(CallbackInfo ci) {
        MainMenuTheme.clearRoles();

        List<GuiEventListener> doomed = new ArrayList<>();
        AbstractWidget single = null;
        AbstractWidget multi = null;
        AbstractWidget options = null;

        for (GuiEventListener child : children()) {
            // The compact icon row and the "Mojang AB" link have no place in the new layout.
            if (child instanceof SpriteIconButton || child instanceof PlainTextButton) {
                doomed.add(child);
                continue;
            }
            if (!(child instanceof AbstractWidget widget)) continue;

            String key = festvisuals$key(widget);
            if (key == null) continue;

            switch (key) {
                case SINGLEPLAYER_KEY -> single = widget;
                case MULTIPLAYER_KEY -> multi = widget;
                case OPTIONS_KEY -> options = widget;
                case REALMS_KEY, QUIT_KEY -> doomed.add(child);
                default -> { }
            }
        }

        doomed.forEach(this::removeWidget);
        if (single == null || multi == null || options == null) return;

        Button packs = addRenderableWidget(Button.builder(Component.literal("Resource Packs"),
                        button -> festvisuals$openPacks())
                .bounds(0, 0, 10, 10).build());

        festvisuals$layout(single, multi, options, packs);

        MainMenuTheme.assign(single, MainMenuTheme.Role.CARD_SINGLE);
        MainMenuTheme.assign(multi, MainMenuTheme.Role.CARD_MULTI);
        MainMenuTheme.assign(options, MainMenuTheme.Role.PILL_SETTINGS);
        MainMenuTheme.assign(packs, MainMenuTheme.Role.PILL_PACKS);
    }

    /** Positions the four survivors on the design's grid. */
    private void festvisuals$layout(AbstractWidget single, AbstractWidget multi,
                                    AbstractWidget options, AbstractWidget packs) {
        int centreX = this.width / 2;

        int cardGap = Math.round(MainMenuTheme.cardGap());
        int cardsTop = Math.round(MainMenuTheme.cardsTop());

        // The stage is scaled off the window height, so a window narrower than 16:9 would push
        // the pair past the edges. Shrink both cards together to keep them on screen.
        float pairFull = MainMenuTheme.cardWidth() * 2f + cardGap;
        float squeeze = Math.min(1f, (this.width * 0.94f) / pairFull);

        int cardWidth = Math.round(MainMenuTheme.cardWidth() * squeeze);
        int cardHeight = Math.round(MainMenuTheme.cardHeight() * squeeze);

        int pairWidth = cardWidth * 2 + cardGap;
        int cardsLeft = centreX - pairWidth / 2;

        single.setRectangle(cardWidth, cardHeight, cardsLeft, cardsTop);
        multi.setRectangle(cardWidth, cardHeight, cardsLeft + cardWidth + cardGap, cardsTop);

        int pillHeight = Math.round(MainMenuTheme.pillHeight());
        int pillGap = Math.round(MainMenuTheme.pillGap());
        // Follows the cards' real height, which the squeeze above may have reduced.
        int pillsTop = cardsTop + cardHeight + Math.round(MainMenuTheme.d(26f));

        int settingsWidth = festvisuals$pillWidth(options.getMessage().getString());
        int packsWidth = festvisuals$pillWidth(packs.getMessage().getString());
        int rowLeft = centreX - (settingsWidth + pillGap + packsWidth) / 2;

        options.setRectangle(settingsWidth, pillHeight, rowLeft, pillsTop);
        packs.setRectangle(packsWidth, pillHeight, rowLeft + settingsWidth + pillGap, pillsTop);
    }

    /** Label width plus the icon and the design's 18px horizontal padding. */
    private int festvisuals$pillWidth(String label) {
        float fontSize = MainMenuTheme.d(13f);
        float width = Fonts.PS_BOLD.getWidth(label, fontSize);
        return Math.round(width + MainMenuTheme.d(14f + 8f + 18f * 2f));
    }

    private void festvisuals$openPacks() {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = this;

        // PackSelectionScreen#onClose only commits the pack list, it never navigates anywhere —
        // vanilla's callers handle that. Without this override "Done" leaves the player stuck on
        // the pack list.
        mc.gui.setScreen(new PackSelectionScreen(
                mc.getResourcePackRepository(),
                repository -> mc.reloadResourcePacks(),
                mc.getResourcePackDirectory(),
                Component.translatable("resourcePack.title")) {
            @Override
            public void onClose() {
                super.onClose();
                mc.gui.setScreen(parent);
            }
        });
    }

    /** Clock, wordmark and the corner exit chip; the widgets paint themselves. */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void festvisuals$stage(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MainMenuTheme.renderStage(RenderUtil.matrices(), this.width);
        MainMenuTheme.renderExit(RenderUtil.matrices(), mouseX, mouseY);
    }

    /**
     * Drops the "Minecraft 26.2 (modified)" stamp. It is drawn straight into the extractor
     * rather than being a widget, so the draw call itself is redirected into nothing — this is
     * the only text() call in the method, the logo and splash go through their own renderers.
     */
    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"
            )
    )
    private void festvisuals$hideVersion(GuiGraphicsExtractor extractor, Font font, String text, int x, int y, int color) {
    }

    /** Matched on the translation key so it holds in every language. */
    private static String festvisuals$key(AbstractWidget widget) {
        return widget.getMessage().getContents() instanceof TranslatableContents contents
                ? contents.getKey()
                : null;
    }
}
