/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jayfella.jme.jfx.injme;

import com.jayfella.jme.jfx.util.JfxPlatform;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.HostInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of {@link HostInterface} to listen requests or notifications from embedded scene.
 *
 * @author JavaSaBr
 */
public class JmeFxHostInterface implements HostInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(JfxPlatform.class);

    /**
     * The JavaFX container.
     */
    private final JmeFxContainerInternal container;

    public JmeFxHostInterface(final JmeFxContainerInternal container) {
        this.container = container;
    }

    /**
     * Gets the JavaFX container.
     *
     * @return the JavaFX container.
     */
    private JmeFxContainerInternal getContainer() {
        return container;
    }

    @Override
    public boolean grabFocus() {
        LOGGER.debug("Called grabFocus()");
        return true;
    }

    @Override
    public void repaint() {
        LOGGER.debug("Called repaint()");
        getContainer().requestRedraw();
    }

    @Override
    public boolean requestFocus() {
        LOGGER.debug("Called requestFocus()");
        return getContainer().requestFocus();
    }

    @Override
    public void setCursor(final CursorFrame cursorFrame) {
        // LOGGER.debug(cursorFrame, frame -> "Called setCursor(" + frame + ")");
        getContainer().requestShowingCursor(cursorFrame);
    }

    @Override
    public void setEmbeddedScene(final EmbeddedSceneInterface sceneInterface) {

        if (sceneInterface == null) {
            LOGGER.debug("Called setEmbeddedScene(null)");
        } else {
            // LOGGER.debug(this, sceneInterface, scene -> "Called setEmbeddedScene(" + scene + ")");
        }

        final JmeFxContainerInternal container = getContainer();
        final EmbeddedSceneInterface currentSceneInterface = container.getSceneInterface();
        if (currentSceneInterface != null) {
            // FIXME release all things
        }

        container.setSceneInterface(sceneInterface);

        if (sceneInterface == null) {
            return;
        }

        var scaleFactor = container.getPixelScaleFactor();

        sceneInterface.setPixelScaleFactors(scaleFactor, scaleFactor);

        final int width = container.getSceneWidth();
        final int height = container.getSceneHeight();

        if (width > 0 && height > 0) {
            sceneInterface.setSize(width, height);
        }

        sceneInterface.setDragStartListener(new JmeFxDnDHandler(container));
    }

    @Override
    public void setEmbeddedStage(final EmbeddedStageInterface stageInterface) {

        if (stageInterface == null) {
            LOGGER.debug("Called setEmbeddedStage(null)");
        } else {
            //LOGGER.debug(this, stageInterface, stage -> "Called setEmbeddedStage(" + stage + ")");
        }

        final JmeFxContainerInternal container = getContainer();
        final EmbeddedStageInterface currentStageInterface = container.getStageInterface();
        if (currentStageInterface != null) {
            // FIXME release all things
        }

        container.setStageInterface(stageInterface);

        if (stageInterface == null) {
            return;
        }

        final int width = container.getSceneWidth();
        final int height = container.getSceneHeight();

        if (width > 0 && height > 0) {
            stageInterface.setSize(width, height);
        }

        stageInterface.setFocused(true, AbstractEvents.FOCUSEVENT_ACTIVATED);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        //LOGGER.debug(this, enabled, val -> "Called setEnabled(" + val + ")");
        getContainer().requestEnabled(enabled);
    }

    @Override
    public void setPreferredSize(final int width, final int height) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Called setPreferredSize(" + width + ", " + height + ")");
        }

        getContainer().requestPreferredSize(width, height);
    }

    @Override
    public boolean traverseFocusOut(final boolean forward) {
        //LOGGER.debug(this, forward, val -> "Called traverseFocusOut(" + val + ")");
        return true;
    }

    @Override
    public void ungrabFocus() {
        LOGGER.debug("Called ungrabFocus()");
    }
}
