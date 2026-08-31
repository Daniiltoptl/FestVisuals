package com.fest.visuals.api.module.setting;

import lombok.Getter;

import java.util.function.Supplier;

/**
 * Free text a player types into the click GUI — a password, a nickname, a command.
 *
 * <p>{@link #secret} only changes how the value is drawn: the field shows dots instead of the
 * characters so a password is not readable on a stream. The value itself is stored and saved as
 * plain text, exactly like every other setting.
 */
@Getter
public class StringSetting extends Setting<String> {
    private String placeholder = "";
    private int maxLength = 48;
    private boolean secret;

    public StringSetting(String name) {
        super(name);
        this.value = "";
    }

    @Override
    public StringSetting value(String value) {
        setValue(value);
        return this;
    }

    @Override
    public void setValue(String value) {
        String clean = value == null ? "" : value;
        if (clean.equals(this.value)) return;
        super.setValue(clean);
        runAction();
    }

    public StringSetting placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public StringSetting maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    /** Draws the field masked. Does not encrypt anything. */
    public StringSetting secret() {
        this.secret = true;
        return this;
    }

    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    @Override
    public StringSetting setVisible(Supplier<Boolean> condition) {
        return (StringSetting) super.setVisible(condition);
    }

    @Override
    public StringSetting onAction(Runnable action) {
        return (StringSetting) super.onAction(action);
    }
}
