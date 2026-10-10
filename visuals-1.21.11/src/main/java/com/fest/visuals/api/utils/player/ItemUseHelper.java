package com.fest.visuals.api.utils.player;

import net.minecraft.world.InteractionHand;

import com.fest.visuals.api.system.interfaces.QuickImports;

/**
 * Holds a consumable through a full use and puts the previous hotbar slot back afterwards.
 *
 * <p>Two vanilla details make the naive "switch slot, call useItem" approach fail silently:
 *
 * <ul>
 *   <li>the selected slot reaches the server through {@code ensureHasSentCarriedItem}, which runs
 *       later in the same tick, so a use issued right after the switch is resolved against the
 *       item that was held before;</li>
 *   <li>{@code Minecraft.handleKeybinds} releases the item on every tick the use key is not held,
 *       which stops a 32-tick drink one tick after it starts.</li>
 * </ul>
 *
 * <p>So the switch waits a tick, and the use key is pinned down for as long as the item is being
 * consumed.
 */
public class ItemUseHelper implements QuickImports {
    /** Ticks between the slot change and the use, for the carried-item packet to go out. */
    private static final int SWITCH_DELAY = 2;

    /** The client does not report the use as started on the tick it is requested. */
    private static final int START_GRACE = 5;

    private enum Stage { IDLE, SWITCHING, USING }

    private Stage stage = Stage.IDLE;
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private int slot = -1;
    private int returnSlot = -1;
    private int timer;

    public boolean isBusy() {
        return stage != Stage.IDLE;
    }

    /** Begins the sequence. The caller keeps calling {@link #tick()} until it reports it is done. */
    public void start(int slot, InteractionHand hand) {
        if (mc.player == null || isBusy()) return;

        this.hand = hand;
        this.slot = slot;
        this.returnSlot = mc.player.getInventory().getSelectedSlot();
        this.stage = Stage.SWITCHING;
        this.timer = SWITCH_DELAY;

        mc.player.getInventory().setSelectedSlot(slot);
    }

    /**
     * Advances the sequence.
     *
     * @return true on the tick the sequence finished, so the caller can start a cooldown
     */
    public boolean tick() {
        if (mc.player == null || mc.gameMode == null) {
            reset();
            return true;
        }

        switch (stage) {
            case SWITCHING -> {
                if (timer-- > 0) return false;

                // A screen opening mid-sequence means the player took over; do not fight them.
                if (mc.screen != null) {
                    cancel();
                    return true;
                }

                mc.options.keyUse.setDown(true);
                mc.gameMode.useItem(mc.player, hand);

                stage = Stage.USING;
                timer = START_GRACE;
                return false;
            }
            case USING -> {
                // Re-asserted every tick: releasing the key is what cancels the animation.
                mc.options.keyUse.setDown(true);

                if (timer-- > 0) return false;
                if (mc.player.isUsingItem()) return false;

                cancel();
                return true;
            }
            default -> {
                return true;
            }
        }
    }

    /** Lets go of the key and restores the slot the player had selected. */
    public void cancel() {
        if (stage != Stage.IDLE) {
            mc.options.keyUse.setDown(false);

            if (mc.player != null && returnSlot >= 0) {
                mc.player.getInventory().setSelectedSlot(returnSlot);
            }
        }
        reset();
    }

    private void reset() {
        stage = Stage.IDLE;
        slot = -1;
        returnSlot = -1;
        timer = 0;
    }
}
