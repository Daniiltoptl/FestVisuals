package com.fest.visuals.inject.other;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.event.events.other.ScreenEvent;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
    protected MixinHandledScreen(Component title) {
        super(title);
    }

    @Shadow protected abstract boolean isHovering(Slot slotIn, double mouseX, double mouseY);
    @Shadow protected abstract void slotClicked(Slot slotIn, int slotId, int mouseButton, ContainerInput type);

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ScreenEvent.ScreenEventData event = new ScreenEvent.ScreenEventData(this);
        ScreenEvent.getInstance().call(event);

        for (Button button : event.buttons()) {
            this.addRenderableWidget(button);
        }
    }
}
