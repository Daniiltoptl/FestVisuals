package com.fest.visuals.inject.other;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.fest.visuals.api.event.events.other.ScreenEvent;
import com.fest.visuals.client.features.modules.utility.AuctionHelperModule;
import com.fest.visuals.client.features.modules.utility.FastScrollerModule;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
    protected MixinHandledScreen(Component title) {
        super(title);
    }

    @Shadow protected abstract boolean isHovering(Slot slotIn, double mouseX, double mouseY);
    @Shadow protected abstract void slotClicked(Slot slotIn, int slotId, int mouseButton, ClickType type);
    @Shadow protected Slot hoveredSlot;

    /** Slots already swept in the current drag, so one pass moves each stack exactly once. */
    @Unique private final Set<Integer> festvisuals$swept = new HashSet<>();
    @Unique private int festvisuals$button = -1;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ScreenEvent.ScreenEventData event = new ScreenEvent.ScreenEventData(this);
        ScreenEvent.getInstance().call(event);

        for (Button button : event.buttons()) {
            this.addRenderableWidget(button);
        }
    }

    @Inject(method = "renderSlots", at = @At("HEAD"))
    private void festvisuals$rankLots(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        AuctionHelperModule helper = AuctionHelperModule.getInstance();
        if (helper.isEnabled()) helper.prepare(this.title, this.getMenu().slots);
    }

    /** Drawn before the item, so the highlight sits behind it. */
    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void festvisuals$highlightLot(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        AuctionHelperModule helper = AuctionHelperModule.getInstance();
        if (!helper.isEnabled()) return;

        int color = helper.highlight(slot);
        if (color == 0) return;

        graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
        if (helper.isCheapest(slot)) {
            graphics.renderOutline(slot.x - 1, slot.y - 1, 18, 18, color | 0xFF000000);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void festvisuals$beginSweep(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        festvisuals$swept.clear();
        festvisuals$button = click.button();
        festvisuals$sweep();
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void festvisuals$continueSweep(MouseButtonEvent click, double deltaX, double deltaY,
                                           CallbackInfoReturnable<Boolean> cir) {
        festvisuals$button = click.button();
        festvisuals$sweep();
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void festvisuals$endSweep(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        festvisuals$swept.clear();
        festvisuals$button = -1;
    }

    /**
     * Moves whatever the cursor is currently over. The first slot of a drag is handled by the
     * vanilla click as well, so it is recorded here without being clicked twice.
     */
    @Unique
    private void festvisuals$sweep() {
        FastScrollerModule module = FastScrollerModule.getInstance();
        if (!module.isEnabled()) return;
        if (!module.acceptsButton(festvisuals$button) || !module.modifierHeld()) return;

        Slot slot = this.hoveredSlot;
        if (slot == null || !slot.hasItem()) return;
        if (!festvisuals$swept.add(slot.index)) return;

        // The slot under the initial press is left to vanilla; sweeping it too would send the
        // same move twice and desync the container.
        if (festvisuals$swept.size() == 1) return;

        ClickType type = module.dropping() ? ClickType.THROW : ClickType.QUICK_MOVE;
        this.slotClicked(slot, slot.index, module.dropping() ? 1 : 0, type);
    }
}
