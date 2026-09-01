package com.fest.visuals.api.module.setting;

import lombok.Getter;

import java.util.function.Supplier;

/**
 * A small pixel grid the player paints in the click GUI.
 *
 * <p>Stored as one string of '0' and '1', one character per cell, row by row. That keeps it a
 * plain {@code Setting<String>} — the config already knows how to save those, and a grid that
 * changed size would still load instead of throwing.
 */
@Getter
public class CanvasSetting extends Setting<String> {
    /** The grid the crosshair is painted on. */
    public static final int SIZE = 16;

    public CanvasSetting(String name) {
        super(name);
        this.value = defaultCross();
    }

    @Override
    public CanvasSetting value(String value) {
        setValue(value);
        return this;
    }

    @Override
    public void setValue(String value) {
        String clean = normalise(value);
        if (clean.equals(this.value)) return;
        super.setValue(clean);
        runAction();
    }

    /** Pads or trims whatever arrives so a stale config can never break the grid. */
    private static String normalise(String value) {
        StringBuilder out = new StringBuilder(SIZE * SIZE);
        for (int i = 0; i < SIZE * SIZE; i++) {
            out.append(value != null && i < value.length() && value.charAt(i) == '1' ? '1' : '0');
        }
        return out.toString();
    }

    public boolean get(int column, int row) {
        if (column < 0 || row < 0 || column >= SIZE || row >= SIZE) return false;
        return value.charAt(row * SIZE + column) == '1';
    }

    public void set(int column, int row, boolean on) {
        if (column < 0 || row < 0 || column >= SIZE || row >= SIZE) return;

        int index = row * SIZE + column;
        if ((value.charAt(index) == '1') == on) return;

        StringBuilder out = new StringBuilder(value);
        out.setCharAt(index, on ? '1' : '0');
        super.setValue(out.toString());
        runAction();
    }

    public void clear() {
        setValue("");
    }

    public void reset() {
        setValue(defaultCross());
    }

    /** The shape the module starts with: a gapped cross around the centre. */
    private static String defaultCross() {
        StringBuilder out = new StringBuilder(SIZE * SIZE);
        int centre = SIZE / 2;

        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                int dx = Math.abs(column - centre);
                int dy = Math.abs(row - centre);
                boolean arm = (dx == 0 && dy >= 2 && dy <= 5) || (dy == 0 && dx >= 2 && dx <= 5);
                out.append(arm ? '1' : '0');
            }
        }
        return out.toString();
    }

    @Override
    public CanvasSetting setVisible(Supplier<Boolean> condition) {
        return (CanvasSetting) super.setVisible(condition);
    }
}
