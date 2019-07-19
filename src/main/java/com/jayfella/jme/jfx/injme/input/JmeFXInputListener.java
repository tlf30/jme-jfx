/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jayfella.jme.jfx.injme.input;

import com.jme3.app.Application;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.event.*;
import com.jayfella.jme.jfx.injme.JmeFxContainerInternal;
import com.jayfella.jme.jfx.injme.JmeFxDnDHandler;
import com.jayfella.jme.jfx.util.JfxPlatform;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import javafx.scene.Scene;

import java.awt.event.KeyEvent;
import java.util.BitSet;

import static java.util.Objects.requireNonNull;

/**
 * Converts Jme Events to JavaFx Events
 *
 * @author Heist, JavaSaBr
 */
public class JmeFXInputListener implements RawInputListener {

    /**
     * The javaFX container.
     */
    private final JmeFxContainerInternal container;

    /**
     * The key state set.
     */
    private final BitSet keyStateSet;

    /**
     * The key char array.
     */
    private final char[][] keyCharArray;

    /**
     * The key char set.
     */
    private final char[] keyCharSet;

    /**
     * The mouse button states.
     */
    private final boolean[] mouseButtonState;

    /**
     * The raw input listener.
     */
    private volatile RawInputListener rawInputListener;

    /**
     * The D&D handler.
     */
    private volatile JmeFxDnDHandler dndHandler;

    public JmeFXInputListener(final JmeFxContainerInternal container) {
        this.container = container;
        this.keyStateSet = new BitSet(0xFF);
        this.keyCharSet = new char[Character.MAX_CODE_POINT];
        this.mouseButtonState = new boolean[3];
        this.keyCharArray = new char[Character.MAX_CODE_POINT][];

        for (int i = 0, length = keyCharArray.length; i < length; i++) {
            keyCharArray[i] = new char[]{(char) i};
        }
    }

    /**
     * Gets the D&D handler.
     *
     * @return the D&D handler.
     */
    private JmeFxDnDHandler getDNDHandler() {
        return dndHandler;
    }

    @Override
    public void beginInput() {
        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.beginInput();
    }

    @Override
    public void endInput() {
        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.endInput();
    }

    /**
     * Gets the raw input listener.
     *
     * @return the raw input listener.
     */
    private RawInputListener getRawInputListener() {
        return rawInputListener;
    }

    /**
     * Gets the javaFX container.
     *
     * @return the javaFX container.
     */
    private JmeFxContainerInternal getContainer() {
        return container;
    }

    /**
     * Gets the key char array.
     *
     * @return the key char array.
     */
    private char[][] getKeyCharArray() {
        return keyCharArray;
    }

    /**
     * Gets the key char set.
     *
     * @return the key char set.
     */
    private char[] getKeyCharSet() {
        return keyCharSet;
    }

    /**
     * Gets the key state set.
     *
     * @return the key state set.
     */
    private BitSet getKeyStateSet() {
        return keyStateSet;
    }

    /**
     * Gets the mouse button states.
     *
     * @return the mouse button states.
     */
    private boolean[] getMouseButtonState() {
        return mouseButtonState;
    }

    @Override
    public void onJoyAxisEvent(final JoyAxisEvent event) {
        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onJoyAxisEvent(event);
    }

    @Override
    public void onJoyButtonEvent(final JoyButtonEvent event) {
        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onJoyButtonEvent(event);
    }

    @Override
    public void onKeyEvent(final KeyInputEvent event) {

        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onKeyEvent(event);

        final JmeFxContainerInternal container = getContainer();
        final EmbeddedSceneInterface sceneInterface = container.getSceneInterface();
        if (sceneInterface == null) return;

        final BitSet keyStateSet = getKeyStateSet();

        final char[][] keyCharArray = getKeyCharArray();
        final char[] keyCharSet = getKeyCharSet();
        final char keyChar = event.getKeyChar();

        final int keyCode = event.getKeyCode();

        int fxKeyCode = keyCode == KeyInput.KEY_UNKNOWN ? KeyEvent.VK_UNDEFINED : AwtKeyInput.convertJmeCode(keyCode);

        final int keyState = retrieveKeyState();

        if (fxKeyCode > keyCharSet.length) {
            switch (keyChar) {
                case '\\': {
                    fxKeyCode = KeyEvent.VK_BACK_SLASH;
                    break;
                }
                default: {
                    return;
                }
            }
        }

        if (container.isFocused()) {
            event.setConsumed();
        }

        if (event.isRepeating()) {

            final char x = keyCharSet[fxKeyCode];

            if (container.isFocused()) {
                sceneInterface.keyEvent(AbstractEvents.KEYEVENT_TYPED, fxKeyCode, keyCharArray[x], keyState);
            }

        } else if (event.isPressed()) {

            keyCharSet[fxKeyCode] = keyChar;
            keyStateSet.set(fxKeyCode);

            if (container.isFocused()) {
                sceneInterface.keyEvent(AbstractEvents.KEYEVENT_PRESSED, fxKeyCode, keyCharArray[keyChar], keyState);
                sceneInterface.keyEvent(AbstractEvents.KEYEVENT_TYPED, fxKeyCode, keyCharArray[keyChar], keyState);
            }

        } else {

            final char x = keyCharSet[fxKeyCode];

            keyStateSet.clear(fxKeyCode);

            if (container.isFocused()) {
                sceneInterface.keyEvent(AbstractEvents.KEYEVENT_RELEASED, fxKeyCode, keyCharArray[x], keyState);
            }
        }
    }

    @Override
    public void onMouseButtonEvent(final MouseButtonEvent event) {

        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onMouseButtonEvent(event);

        final JmeFxContainerInternal container = getContainer();
        final Application application = requireNonNull(container.getApplication());
        final InputManager inputManager = application.getInputManager();

        if (container.getSceneInterface() == null) {
            return;
        }

        // final Scene scene = notNull(container.getScene());
        final Scene scene = container.getScene();

        final int x = event.getX();
        final int y = (int) Math.round(scene.getHeight()) - event.getY();

        int button;

        switch (event.getButtonIndex()) {
            case 0: {
                button = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
                break;
            }
            case 1: {
                button = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
                break;
            }
            case 2: {
                button = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
                break;
            }
            default: {
                return;
            }
        }

        mouseButtonState[event.getButtonIndex()] = event.isPressed();

        final boolean covered = container.isCovered(x, y);

        if (!covered) {
            container.loseFocus();
        } else if (inputManager.isCursorVisible()) {
            event.setConsumed();
            container.grabFocus();
        }

        int type;

        if (event.isPressed()) {
            type = AbstractEvents.MOUSEEVENT_PRESSED;
        } else if (event.isReleased()) {
            type = AbstractEvents.MOUSEEVENT_RELEASED;
        } else {
            return;
        }

        if (inputManager.isCursorVisible() || event.isReleased()) {
            JfxPlatform.runInFxThread(() -> onMouseButtonEventImpl(x, y, button, type));
        }
    }

    private void onMouseButtonEventImpl(final int x, final int y, final int button, final int type) {

        final boolean[] mouseButtonState = getMouseButtonState();
        final JmeFxDnDHandler dndHandler = getDNDHandler();

        final boolean primaryBtnDown = mouseButtonState[0];
        final boolean middleBtnDown = mouseButtonState[1];
        final boolean secondaryBtnDown = mouseButtonState[2];

        if (dndHandler != null) {
            dndHandler.mouseUpdate(x, y, primaryBtnDown);
        }

        final JmeFxContainerInternal container = getContainer();
        final EmbeddedSceneInterface sceneInterface = requireNonNull(container.getSceneInterface());

        final int screenX = container.getPositionX() + x;
        final int screenY = container.getPositionY() + y;

        final BitSet keyStateSet = getKeyStateSet();

        final boolean shift = keyStateSet.get(KeyEvent.VK_SHIFT);
        final boolean ctrl = keyStateSet.get(KeyEvent.VK_CONTROL);
        final boolean alt = keyStateSet.get(KeyEvent.VK_ALT);
        final boolean meta = keyStateSet.get(KeyEvent.VK_META);
        final boolean popupTrigger = button == AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;

        /* Java 12 implementation
        sceneInterface.mouseEvent(type, button,
                primaryBtnDown, middleBtnDown, secondaryBtnDown,
                shift, ctrl,
                x, y, screenX, screenY,
                shift, ctrl, alt,
                meta, popupTrigger);
         */

        // java 11 implementation
        sceneInterface.mouseEvent(type, button, primaryBtnDown, middleBtnDown, secondaryBtnDown, x, y, screenX, screenY,
                shift, ctrl, alt, meta, popupTrigger);

    }

    @Override
    public void onMouseMotionEvent(final MouseMotionEvent event) {

        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onMouseMotionEvent(event);

        final JmeFxContainerInternal container = getContainer();

        final Application application = container.getApplication();
        final InputManager inputManager = application.getInputManager();

        if (container.getSceneInterface() == null) {
            return;
        }

        final Scene scene = container.getScene();

        final int x = event.getX();
        final int y = (int) Math.round(scene.getHeight()) - event.getY();

        final boolean covered = container.isCovered(x, y);

        if (covered) {
            event.setConsumed();
        }

        final boolean[] mouseButtonState = getMouseButtonState();
        // not sure if should be grabbing focused on mouse motion event
        // grabFocus();

        int type = AbstractEvents.MOUSEEVENT_MOVED;
        int button = AbstractEvents.MOUSEEVENT_NONE_BUTTON;

        final int wheelRotation = (int) Math.round(event.getDeltaWheel() / -120.0);
        final int wheelTotalRotation = event.getWheel();

        if (wheelRotation != 0) {
            type = AbstractEvents.MOUSEEVENT_VERTICAL_WHEEL;
            button = AbstractEvents.MOUSEEVENT_NONE_BUTTON;
        } else if (mouseButtonState[0]) {
            type = AbstractEvents.MOUSEEVENT_DRAGGED;
            button = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
        } else if (mouseButtonState[1]) {
            type = AbstractEvents.MOUSEEVENT_DRAGGED;
            button = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
        } else if (mouseButtonState[2]) {
            type = AbstractEvents.MOUSEEVENT_DRAGGED;
            button = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
        }

        final int finalType = type;
        final int finalButton = button;

        if (inputManager.isCursorVisible()) {
            JfxPlatform.runInFxThread(() -> onMouseMotionEventImpl(x, y, wheelRotation, wheelTotalRotation, finalType, finalButton));
        }
    }

    private void onMouseMotionEventImpl(int x, int y, int wheelRotation, int wheelTotalRotation, int type, int button) {

        final JmeFxContainerInternal container = getContainer();
        // final Application application = notNull(container.getApplication());
        final Application application = container.getApplication();
        final InputManager inputManager = application.getInputManager();

        if (!inputManager.isCursorVisible()) {
            return;
        }

        final JmeFxDnDHandler dndHandler = getDNDHandler();
        final boolean[] mouseButtonState = getMouseButtonState();

        final boolean primaryBtnDown = mouseButtonState[0];
        final boolean middleBtnDown = mouseButtonState[1];
        final boolean secondaryBtnDown = mouseButtonState[2];

        if (dndHandler != null) {
            dndHandler.mouseUpdate(x, y, primaryBtnDown);
        }

        final EmbeddedSceneInterface sceneInterface = container.getSceneInterface();

        final int screenX = container.getPositionX() + x;
        final int screenY = container.getPositionY() + y;

        final BitSet keyStateSet = getKeyStateSet();

        final boolean shift = keyStateSet.get(KeyEvent.VK_SHIFT);
        final boolean ctrl = keyStateSet.get(KeyEvent.VK_CONTROL);
        final boolean alt = keyStateSet.get(KeyEvent.VK_ALT);
        final boolean meta = keyStateSet.get(KeyEvent.VK_META);

        /* Java 12 implementation
        sceneInterface.mouseEvent(type, button,
                primaryBtnDown, middleBtnDown, secondaryBtnDown,
                shift, ctrl,
                x, y, screenX, screenY,
                shift, ctrl, alt,
                meta, false);
         */

        // Java 11 implementation
        sceneInterface.mouseEvent(type, button, primaryBtnDown, middleBtnDown, secondaryBtnDown, x, y, screenX, screenY,
                shift, ctrl, alt, meta, false);



        /*
        public void scrollEvent(int type, double scrollX, double scrollY,
                            double totalScrollX, double totalScrollY,
                            double xMultiplier, double yMultiplier,
                            double x, double y, double screenX, double screenY,
                            boolean shift, boolean ctrl,
                            boolean alt, boolean meta, boolean inertia);
         */


        // horizontal scroll
        // NOT IMPLEMENTED
        double scrollX = 0;
        double totalScrollX = 1;
        double xMultiplier = 50;

        // vertical scroll
        double scrollY = -wheelRotation;
        double totalScrollY = wheelTotalRotation;
        double yMultiplier = 60;

        boolean inertia = false;

        sceneInterface.scrollEvent(type, scrollX, scrollY,
                totalScrollX, totalScrollY,
                xMultiplier, yMultiplier,
                x, y, screenX, screenY,
                shift, ctrl, alt, meta, inertia);
    }

    @Override
    public void onTouchEvent(final TouchEvent event) {
        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onTouchEvent(event);
    }

    private int retrieveKeyState() {

        int embedModifiers = 0;

        final BitSet keyStateSet = getKeyStateSet();

        if (keyStateSet.get(KeyEvent.VK_SHIFT)) {
            embedModifiers |= AbstractEvents.MODIFIER_SHIFT;
        }

        if (keyStateSet.get(KeyEvent.VK_CONTROL)) {
            embedModifiers |= AbstractEvents.MODIFIER_CONTROL;
        }

        if (keyStateSet.get(KeyEvent.VK_ALT)) {
            embedModifiers |= AbstractEvents.MODIFIER_ALT;
        }

        if (keyStateSet.get(KeyEvent.VK_META)) {
            embedModifiers |= AbstractEvents.MODIFIER_META;
        }

        return embedModifiers;
    }

    public void setEverListeningRawInputListener(final RawInputListener rawInputListenerAdapter) {
        this.rawInputListener = rawInputListenerAdapter;
    }

    /**
     * set on drag start /nulled on end<br> necessary so that the drag events can be generated
     * appropriately
     * @param dndHandler The drag and drop handler.
     */
    public void setMouseDNDListener(final JmeFxDnDHandler dndHandler) {
        assert this.dndHandler == null || dndHandler == null : "duplicate dnd handler register? ";
        this.dndHandler = dndHandler;
    }
}
