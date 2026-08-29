package com.fest.visuals.api.event.interfaces;

import com.fest.visuals.api.event.Listener;

public interface Cacheable<T> {
    Listener<T>[] getCache();
}