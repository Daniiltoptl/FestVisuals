package com.fest.visuals.api.event;

public record EventListener(Runnable action) {
    public void unsubscribe() {
        action.run();
    }
}
