package com.jayfella.jme.jfx.test;

import com.jayfella.jme.jfx.JavaFxUI;
import com.jme3.app.SimpleApplication;
import javafx.event.EventType;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;

/**
 * Tests horizontal and vertical scrolling using the mouse.
 */
public class TestScroll extends SimpleApplication {

    public static void main(String... args) {
        TestScroll main = new TestScroll();
        main.start();
    }

    @Override
    public void simpleInitApp() {



        JavaFxUI.initialize(this);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefHeight(600);
        scrollPane.setPrefWidth(400);

        Button button = new Button("My Button");
        button.setPrefSize(400, 1300);

        button.setOnScroll(System.err::println);
        // button.addEventHandler(EventType.ROOT, System.err::println);

        scrollPane.setContent(button);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        JavaFxUI.getInstance().attachChild(scrollPane);
    }

    @Override
    public void simpleUpdate(float tpf) {
        inputManager.setCursorVisible(true);
    }

}
