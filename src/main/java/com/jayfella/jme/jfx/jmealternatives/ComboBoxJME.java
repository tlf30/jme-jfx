package com.jayfella.jme.jfx.jmealternatives;

import com.jayfella.jme.jfx.JavaFxUI;
import javafx.geometry.Bounds;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;

/**
 * This is a JME alternative for the ComboBox.
 *
 * It behaves broadly the same way as the core ComboBox, but is not feature complete.
 *
 * In particular:
 * It doesn't fully support keyboard selection (a mouse click is needed to close it)
 * It doesn't support sizing based on number of entries, only by pixels
 * It can go "offscreen" when it tries to open near the bottom of the screen
 *
 * @param <T>
 */
public class ComboBoxJME<T> extends ComboBox<T> {

    Runnable removeListPopup;

    int maxHeight = 200;

    @Override
    public void show() {
        Bounds boundsInScene = localToScene(getBoundsInLocal());

        ListView<T> items = new ListView<>();
        items.setItems(this.getItems());
        items.setMinWidth(boundsInScene.getWidth());
        items.setMaxHeight(maxHeight);

        removeListPopup = JavaFxUI.getInstance().attachPopup(items, boundsInScene.getMinX(), boundsInScene.getMaxY());

        items.setOnMousePressed(event -> {
            getSelectionModel().select(items.getSelectionModel().getSelectedItem());
            removeListPopup.run();
        });
    }

    @Override
    public void hide() {
        //do nothing, we're handling our own open/close (although keyboard based selection might need this?)
    }

    /**
     * Sets the height of the combobox when it opens (in pixels)
     * @param height
     */
    public void setListHeight(int height){
        this.maxHeight = height;
    }

}
