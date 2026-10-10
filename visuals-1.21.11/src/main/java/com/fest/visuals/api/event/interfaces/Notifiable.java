package com.fest.visuals.api.event.interfaces;

public interface Notifiable<E> {
    void notify(E event);
}
