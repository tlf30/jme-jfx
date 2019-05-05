package com.jayfella.jme.jfx.util;

import javafx.application.Platform;

/**
 * The class with additional utility methods for JavaFX Platform.
 *
 * @author JavaSaBr
 */
public class JfxPlatform {

    /**
     * Execute the task in JavaFX thread.
     *
     * @param task the task.
     */
    public static void runInFxThread(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
}
