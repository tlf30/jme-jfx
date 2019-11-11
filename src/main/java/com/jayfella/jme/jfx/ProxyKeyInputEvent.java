package com.jayfella.jme.jfx;

import com.jme3.input.event.KeyInputEvent;

/**
 * An extension of KeyInputEvent just to differentiate from regular input events.
 * This class stops the JavaFX implementation from consuming the event in certain circumstances.
 * For example if JME handled the "pressed" event, and JFX takes focus, JFX will pass the "released" event to JME when it occurs.
 */
public class ProxyKeyInputEvent extends KeyInputEvent {

    public ProxyKeyInputEvent(int keyCode, char keyChar, boolean pressed, boolean repeating) {
        super(keyCode, keyChar, pressed, repeating);
    }

}
