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
                
        Button alts = addRenderableWidget(Button.builder(Component.literal("Alts"),
                        button -> minecraft.gui.setScreen(new com.fest.visuals.client.ui.alt.AltManagerScreen(this)))
                .bounds(0, 0, 10, 10).build());

        festvisuals$layout(single, multi, options, packs, alts);

        MainMenuTheme.assign(single, MainMenuTheme.Role.CARD_SINGLE);
        MainMenuTheme.assign(multi, MainMenuTheme.Role.CARD_MULTI);
        MainMenuTheme.assign(options, MainMenuTheme.Role.PILL_SETTINGS);
        MainMenuTheme.assign(packs, MainMenuTheme.Role.PILL_PACKS);
        MainMenuTheme.assign(alts, MainMenuTheme.Role.PILL_PACKS);
    }

    private void festvisuals$layout(AbstractWidget single, AbstractWidget multi,
                                    AbstractWidget options, AbstractWidget packs, AbstractWidget alts) {
        int centreX = this.width / 2;

        int cardGap = Math.round(MainMenuTheme.cardGap());
        int cardsTop = Math.round(MainMenuTheme.cardsTop());

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
        int pillsTop = cardsTop + cardHeight + Math.round(MainMenuTheme.d(26f));

        int settingsWidth = festvisuals$pillWidth(MainMenuTheme.strip(options.getMessage().getString()));
        int packsWidth = festvisuals$pillWidth(MainMenuTheme.strip(packs.getMessage().getString()));
        int altsWidth = festvisuals$pillWidth(MainMenuTheme.strip(alts.getMessage().getString()));
        int rowLeft = centreX - (settingsWidth + pillGap + packsWidth + pillGap + altsWidth) / 2;

        options.setRectangle(settingsWidth, pillHeight, rowLeft, pillsTop);
        packs.setRectangle(packsWidth, pillHeight, rowLeft + settingsWidth + pillGap, pillsTop);
        alts.setRectangle(altsWidth, pillHeight, rowLeft + settingsWidth + pillGap + packsWidth + pillGap, pillsTop);
    }

    private int festvisuals$pillWidth(String label) {
        float fontSize = MainMenuTheme.d(13f);
        float width = Fonts.PS_BOLD.getWidth(label, fontSize);
        return Math.round(width + MainMenuTheme.d(14f + 8f + 18f * 2f));
    }

    private void festvisuals$openPacks() {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = this;

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

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void festvisuals$stage(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MainMenuTheme.renderStage(RenderUtil.matrices(), this.width);
        MainMenuTheme.renderExit(RenderUtil.matrices(), mouseX, mouseY);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"
            )
    )
    private void festvisuals$hideVersion(GuiGraphicsExtractor extractor, Font font, String text, int x, int y, int color) {
    }

    private static String festvisuals$key(AbstractWidget widget) {
        return widget.getMessage().getContents() instanceof TranslatableContents contents
                ? contents.getKey()
                : null;
    }
}
