package com.jayfella.jme.jfx.impl;

/**
 * Gives a javafx control an update loop just like an AppState.
 */

@FunctionalInterface
public interface JmeUpdateLoop {
    void update(float tpf);
}
