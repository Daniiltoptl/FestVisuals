package com.fest.visuals.api.event.interfaces;

import com.fest.visuals.api.event.EventListener;

public interface Subscribable<L, T> {
    EventListener subscribe(L listener);
    void unsubscribe(L listener);
}
