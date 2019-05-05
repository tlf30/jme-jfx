package com.jayfella.jme.jfx;

import com.jayfella.jme.jfx.impl.JmeUpdateLoop;
import com.jayfella.jme.jfx.impl.SceneNotifier;
import com.jayfella.jme.jfx.injme.JmeFxContainer;
import com.jayfella.jme.jfx.util.JfxPlatform;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class JavaFxUI {

    private static Application app;
    private static JmeFxContainer container;

    // the general overlay
    private static Group group;
    private static Scene scene;
    private static AnchorPane uiscene;

    // dialog - overlays an anchorpane to stop clicking background items and allows "darkening" too.
    private static AnchorPane dialogAnchorPanel;
    private static  javafx.scene.Node dialog;

    private static final List<JmeUpdateLoop> updatingItems = new ArrayList<>();

    private static int camWidth, camHeight;

    public static void initialize(Application application) {
        app = application;

        Node guiNode = ((SimpleApplication)application).getGuiNode();
        container = JmeFxContainer.install(application, guiNode);

        group = new Group();
        uiscene = new AnchorPane();
        uiscene.setMinWidth(app.getCamera().getWidth());
        uiscene.setMinHeight(app.getCamera().getHeight());
        group.getChildren().add(uiscene);

        scene = new Scene(group, app.getCamera().getWidth(), app.getCamera().getHeight());
        scene.setFill(Color.TRANSPARENT);

        container.setScene(scene, group);

        dialogAnchorPanel = new AnchorPane();
        dialogAnchorPanel.setMinWidth(app.getCamera().getWidth());
        dialogAnchorPanel.setMinHeight(app.getCamera().getHeight());

        // we get the screen bounds now - as soon as possible - because the bound check is done in an AppState.
        // By the time the AppState is initialized the screen size could have changed and our checks would fail.
        camWidth = application.getCamera().getWidth();
        camHeight = application.getCamera().getHeight();

        application.getStateManager().attach(new JavaFxUpdater());
    }

    private static void refreshSceneBounds() {

        JfxPlatform.runInFxThread(() -> {

            //uiscene.setMinWidth(app.getCamera().getWidth());
            //uiscene.setMinHeight(app.getCamera().getHeight());

            // group.getChildren().clear();

            // group = new Group();
            // group.getChildren().add(uiscene);

            // scene = new Scene(group, app.getCamera().getWidth(), app.getCamera().getHeight());
            // scene.setFill(Color.TRANSPARENT);

            // container.setScene(scene, group);
        });

    }

    public static void attachChild(javafx.scene.Node node) {
        JfxPlatform.runInFxThread(() ->uiscene.getChildren().add(node));

        if (node instanceof JmeUpdateLoop) {
            updatingItems.add((JmeUpdateLoop) node);
        }

        if (node instanceof SceneNotifier) {
            ((SceneNotifier)node).onAttached(app);
        }
    }

    public static void detachChild(javafx.scene.Node node) {
        JfxPlatform.runInFxThread(() ->uiscene.getChildren().remove(node));

        if (node instanceof JmeUpdateLoop) {
            updatingItems.remove(node);
        }

        if (node instanceof SceneNotifier) {
            ((SceneNotifier)node).onDetached();
        }
    }

    public static void detachChild(String fxId) {
        JfxPlatform.runInFxThread(() -> {
            javafx.scene.Node node = uiscene.lookup("#" + fxId);
            if (node != null) {
                uiscene.getChildren().remove(node);

                app.enqueue(() -> {
                    if (node instanceof JmeUpdateLoop) {
                        updatingItems.remove(node);
                    }

                    if (node instanceof SceneNotifier) {
                        ((SceneNotifier)node).onDetached();
                    }
                });
            }
        });
    }

    public static javafx.scene.Node getChild(String fxId) {
        return uiscene.lookup("#" + fxId);
    }

    /* removed for now,
    // I need to determine the best way to notify all children in the entire hierarchy that implement SceneNotifier.
    // I should probably just keep a reference to them.
    public static void removeAllChildren() {
        updatingItems.clear();

        JfxPlatform.runInFxThread(() -> {

            uiscene.getChildren().forEach(node -> {
                if (node instanceof SceneNotifier) {
                    ((SceneNotifier) node).onDetached();
                }
            });

            // uiscene.getChildren().clear();

        });

    }
    */

    public static void showDialog(javafx.scene.Node node) {
        showDialog(node, true);
    }

    public static void showDialog(javafx.scene.Node node, boolean dimmed) {

        // center the dialog
        int scrWidth = app.getCamera().getWidth();
        int scrHeight = app.getCamera().getHeight();



        dialog = node;

        if (dialog instanceof JmeUpdateLoop) {
            updatingItems.add((JmeUpdateLoop) dialog);
        }

        JfxPlatform.runInFxThread(() -> {
            dialogAnchorPanel.setStyle(null);

            dialogAnchorPanel.getChildren().add(node);

            if (dimmed) {
                dialogAnchorPanel.setStyle("-fx-background-color:#000000AA");
            }

            uiscene.getChildren().add(dialogAnchorPanel);

            float dialogWidth = (float) node.getBoundsInParent().getWidth();
            float dialogHeight = (float) node.getBoundsInParent().getHeight();

            node.setLayoutX((scrWidth * 0.5f) - (dialogWidth * 0.75f));
            node.setLayoutY((scrHeight * 0.5f) - (dialogHeight * 0.75f));
        });
    }

    public static void removeDialog() {
        JfxPlatform.runInFxThread(() -> {
            dialogAnchorPanel.getChildren().clear();
            uiscene.getChildren().remove(dialogAnchorPanel);
        });

        updatingItems.remove(dialog);
        dialog = null;
    }

    public static void runInJavaFxThread(Runnable task) {
        Platform.runLater(task);
    }

    public static void runInJmeThread(Runnable task) {
        app.enqueue(task);
    }

    public static class JavaFxUpdater extends BaseAppState {

        private Camera cam;


        @Override protected void initialize(Application app) {
            cam = app.getCamera();

        }

        @Override protected void cleanup(Application app) { }
        @Override protected void onEnable() { }
        @Override protected void onDisable() { }

        @Override
        public void update(float tpf) {

            if (camWidth != cam.getWidth() || camHeight != cam.getHeight()) {
                camWidth = cam.getWidth();
                camHeight = cam.getHeight();
                refreshSceneBounds();
                System.out.println("Bounds Changed.");
            }

            if (container.isNeedWriteToJme()) {
                container.writeToJme();
            }

            updatingItems.forEach(item -> item.update(tpf));
        }
    }

}
