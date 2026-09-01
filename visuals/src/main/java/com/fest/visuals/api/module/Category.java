package com.fest.visuals.api.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    RENDER("Render"),
    HUD("HUD"),
    OTHER("Разное");

    private final String label;
}