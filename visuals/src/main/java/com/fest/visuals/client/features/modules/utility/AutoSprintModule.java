package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.player.move.SprintEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;

@ModuleRegister(name = "Auto Sprint", desc = "Автоматически нажимает спринт", category = Category.UTILITY)
public class AutoSprintModule extends Module {
    @Getter private static final AutoSprintModule instance = new AutoSprintModule();

    @Override
    public void onEvent() {
        addEvents(SprintEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;
            if (mc.options.keySprint.isDown()) return;

            boolean moving = event.getDirectionalInput().isForwards();
            boolean canSprint = mc.player.getFoodData().getFoodLevel() > 6 && !mc.player.isUsingItem();

            if (moving && canSprint) {
                event.setSprint(true);
            }
        })));
    }
}
