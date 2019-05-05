package com.jayfella.jme.jfx.injme.cursor;

import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.CursorType;

/**
 * The provider to implement the process of showing different types of cursors.
 *
 * @author JavaSaBr
 */
public interface CursorDisplayProvider {

    /**
     * This method will be called on initializing javaFX container to prepare an image for the cursor type.
     *
     * @param cursorType the cursor type.
     */
    void prepare(CursorType cursorType);

    /**
     * Shows an image for the cursor frame.
     *
     * @param cursorFrame the cursor frame.
     */
    void show(CursorFrame cursorFrame);
}
