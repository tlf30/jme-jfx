package com.jayfella.jme.jfx;

import com.jayfella.jme.jfx.injfx.input.JfxKeyInput;
import com.jme3.app.Application;
import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static com.jme3.input.KeyInput.*;

/**
 * An input handler to manage cross-input between JME and JavaFX.
 * If a key was "pressed" in JME, and the focus was changed to JFX, JFX should pass the "released" event to JME.
 */
public class CrossInputHandler {

    private static final Logger log = LoggerFactory.getLogger(CrossInputHandler.class);

    private final Application application;
    private final InputManager inputManager;
    private final AnchorPane uiscene;

    private final RawInputListener jmeRawInputListener;
    private final EventHandler<KeyEvent> jfxReleasedHandler;

    // JME will be reading, adding and removing.
    // JavaFx will be reading and removing.
    private final ConcurrentHashMap<Integer, KeyInputEvent> jmeKeyInputEvents = new ConcurrentHashMap<>();

    public CrossInputHandler(Application application, AnchorPane uiscene) {

        this.application = application;
        this.inputManager = application.getInputManager();
        this.uiscene = uiscene;

        this.jmeRawInputListener = new JmeCrossInputHandler();
        this.jfxReleasedHandler = new JfxKeyReleasedInputHandler();
    }

    private int convertKeyCode(KeyCode keyCode) {
        var code = JfxKeyInput.KEY_CODE_TO_JME.get(keyCode);
        return code == null ? KEY_UNKNOWN : code;
    }

    public void bind() {
        uiscene.setOnKeyReleased(jfxReleasedHandler);
        inputManager.addRawInputListener(jmeRawInputListener);
    }

    public void unbind() {
        uiscene.removeEventHandler(KeyEvent.KEY_RELEASED, jfxReleasedHandler);
        inputManager.removeRawInputListener(jmeRawInputListener);
    }

    private class JfxKeyReleasedInputHandler implements EventHandler<KeyEvent> {

        @Override
        public void handle(KeyEvent event) {

            // JavaFX has detected a key release.

            int jfxKeycode = event.getCode().getCode();
            int jmeKeyCode = convertKeyCode(event.getCode());

            if (jfxKeycode == 0) {
                return;
            }

            log.debug("JFX - KeyReleased: " + event.toString());
            log.debug("JFX - KeyCode: " + jfxKeycode + " - JME KeyCode: " + jmeKeyCode);

            KeyInputEvent keyInputEvent = jmeKeyInputEvents.get(jmeKeyCode);

            // if a keyInput exists it means the key was pressed while JME was focused. The key has been released
            // while JavaFX has the focus, so we need to tell JME we have released the key.

            if (keyInputEvent != null) {

                // since we are passing the release event to JME, we remove the pressed event from our list.
                jmeKeyInputEvents.remove(jmeKeyCode);
                log.debug("Passing KeyReleased to JME: " + jmeKeyCode + " - Size Left: " + jmeKeyInputEvents.size());

                KeyInputEvent releaseEvent = new ProxyKeyInputEvent(
                        keyInputEvent.getKeyCode(),
                        keyInputEvent.getKeyChar(),
                        false,
                        false
                );

                // we're on the JavaFX thread in this handler.
                application.enqueue(() -> queueInputEvent(releaseEvent));
            }
        }
    }

    private void queueInputEvent(KeyInputEvent event) {

        try {

            // we tell the inputManager to allow us to inject this event so it gets passed to all registered listeners.
            Field eventsPermittedField = inputManager.getClass().getDeclaredField("eventsPermitted");
            eventsPermittedField.setAccessible(true);
            eventsPermittedField.set(inputManager, true);
            inputManager.onKeyEvent(event);
            eventsPermittedField.set(inputManager, false);

        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    private class JmeCrossInputHandler implements RawInputListener {

        @Override public void beginInput() { }
        @Override public void endInput() { }
        @Override public void onJoyAxisEvent(JoyAxisEvent evt) { }
        @Override public void onJoyButtonEvent(JoyButtonEvent evt) { }
        @Override public void onMouseMotionEvent(MouseMotionEvent evt) { }
        @Override public void onMouseButtonEvent(MouseButtonEvent evt) { }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {

            if (evt.getKeyCode() == 0) {
                return;
            }

            KeyInputEvent existingEvent = jmeKeyInputEvents.get(evt.getKeyCode());

            if (evt.isPressed()) {
                if (existingEvent == null) {
                    jmeKeyInputEvents.put(evt.getKeyCode(), evt);
                    log.debug("JME - KeyEvent ADDED: " + evt.toString() + " - SIZE: " + jmeKeyInputEvents.size());
                }
            }

            else if (evt.isReleased() && !evt.isRepeating()) {
                if (existingEvent != null) {
                    if (jmeKeyInputEvents.remove(evt.getKeyCode()) != null) {
                        log.debug("JME - KeyEvent REMOVED: " + evt.toString() + " - SIZE: " + jmeKeyInputEvents.size());
                    }

                }

            }

        }

        @Override public void onTouchEvent(TouchEvent evt) { }

    }

}
