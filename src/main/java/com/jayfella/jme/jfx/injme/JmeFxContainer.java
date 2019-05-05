package com.jayfella.jme.jfx.injme;

import com.jme3.app.Application;
import com.jayfella.jme.jfx.injme.cursor.CursorDisplayProvider;
import com.jme3.scene.Node;
import javafx.scene.Group;
import javafx.scene.Scene;

/**
 * The interface to work with container of javaFX UI.
 *
 * @author JavaSaBr
 */
public interface JmeFxContainer {

    /**
     * Build the JavaFX container for the application.
     *
     * @param application the application.
     * @param guiNode     the GUI node.
     * @return the javaFX container.
     */
    static JmeFxContainer install(final Application application, final Node guiNode) {
        return JmeFxContainerImpl.install(application, guiNode);
    }

    /**
     * Build the JavaFX container for the application.
     *
     * @param application    the application.
     * @param guiNode        the GUI node.
     * @param cursorProvider the cursor provider.
     * @return the javaFX container.
     */
    static JmeFxContainer install(final Application application, final Node guiNode,
                                  final CursorDisplayProvider cursorProvider) {
        return JmeFxContainerImpl.install(application, guiNode, cursorProvider);
    }

    /**
     * Checks of existing waiting frames.
     *
     * @return true if need to write javaFx frame.
     */
    boolean isNeedWriteToJme();

    /**
     * Write javaFX frame to jME texture.
     */
    Void writeToJme();

    /**
     * Set a new scene to this container.
     *
     * @param newScene the new scene or null.
     * @param rootNode the new root of the scene.
     */
    void setScene(Scene newScene, Group rootNode);

    /**
     * Gets the current cursor provider.
     *
     * @return the current cursor provider.
     */
    CursorDisplayProvider getCursorProvider();

    /**
     * Gets the current scene.
     *
     * @return the current scene.
     */
    Scene getScene();

    /**
     * Gets the root UI node.
     *
     * @return the root UI node.
     */
    Group getRootNode();
}
