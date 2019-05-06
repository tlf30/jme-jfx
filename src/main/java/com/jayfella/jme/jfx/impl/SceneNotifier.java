package com.jayfella.jme.jfx.impl;

import com.jme3.app.Application;

/**
 * Allows a javafx node to be notified when they are attached and removed from the GUI.

 */
public interface SceneNotifier {

    /**
     * Called when a javafx.scene.Node is attached to a scene.
     * This method is always called on the jmonkeyengine GL thread.
     * @param app The jmonkeyengine application.
     */
    void onAttached(Application app);

    /**
     * Called when a javafx.scene.Node is detached from a scene.
     * This method is always called on the jmonkeyengine GL thread.
     */
    void onDetached();
}
