package com.jayfella.jme.jfx.impl;

import com.jme3.app.Application;

public interface SceneNotifier {

    void onAttached(Application app);
    void onDetached();
}
