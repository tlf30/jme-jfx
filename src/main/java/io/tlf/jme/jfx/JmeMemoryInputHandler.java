package io.tlf.jme.jfx;

import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This {@link RawInputListener} will be executed after the
 * FX Input Listener. So pressed Keys and mouse buttons
 * can be considered as JME Pressed Input.
 *
 * @author Klaus Pichler
 */
public class JmeMemoryInputHandler implements RawInputListener {

    private static final Logger LOGGER = Logger.getLogger(JmeMemoryInputHandler.class.getName());

    private final Map<Integer, KeyInputEvent> jmeKeyInputEvents = new HashMap<>();
    private final Map<Integer, MouseButtonEvent> jmeMouseButtonEvents = new HashMap<>();

    @Override
    public void beginInput() {
        // Nothing to do
    }

    @Override
    public void endInput() {
        // Nothing to do
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
        // Nothing to do
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
        // Nothing to do
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        // Nothing to do
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {

        // Storing or removing now JME Mouse button events
        MouseButtonEvent existingEvent = jmeMouseButtonEvents.get(evt.getButtonIndex());

        if (evt.isPressed()) {
            if (existingEvent == null) {
                jmeMouseButtonEvents.put(evt.getButtonIndex(), evt);
            }
        } else if (evt.isReleased()) {
            if (existingEvent != null) {
                jmeMouseButtonEvents.remove(evt.getButtonIndex());
            }
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {

        // Storing or removing now JME key pressed events.
        if (evt.getKeyCode() == 0) {
            return;
        }

        KeyInputEvent existingEvent = jmeKeyInputEvents.get(evt.getKeyCode());

        if (evt.isPressed()) {
            if (existingEvent == null) {
                jmeKeyInputEvents.put(evt.getKeyCode(), evt);
            }
        } else if (evt.isReleased() && !evt.isRepeating()) {
            if (existingEvent != null) {
                jmeKeyInputEvents.remove(evt.getKeyCode());
            }
        }

    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
        // Nothing to do

    }

    public Map<Integer, KeyInputEvent> getJmeKeyInputEvents() {
        return jmeKeyInputEvents;
    }

    public Map<Integer, MouseButtonEvent> getJmeMouseButtonEvents() {
        return jmeMouseButtonEvents;
    }

}
