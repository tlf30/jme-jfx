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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class JavaFxUI {

    private static final Logger log = LoggerFactory.getLogger(JavaFxUI.class);

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

    /**
     * Initializes the JavaFxUI class ready for use.
     * This initialization must be called first before this class is ready for use.
     * @param application the Jmonkey Application.
     * @param cssStyles   The global css stylesheets.
     */
    public static void initialize(Application application, String... cssStyles) {
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

        if (cssStyles != null) {
            scene.getStylesheets().addAll(cssStyles);
        }

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

    /**
     * Attach a javafx.scene.Node to the GUI scene.
     * @param node the node to attach to the scene.
     */
    public static void attachChild(javafx.scene.Node node) {
        JfxPlatform.runInFxThread(() ->{
            uiscene.getChildren().add(node);
            recursivelyNotifyChildrenAdded(node);
        });
    }

    /**
     * Detach a node from the GUI scene.
     * @param node the node to detach from the scene.
     */
    public static void detachChild(javafx.scene.Node node) {
        JfxPlatform.runInFxThread(() ->{
            uiscene.getChildren().remove(node);
            recursivelyNotifyChildrenRemoved(node);
        });
    }

    /**
     * Detach a node from the GUI scene.
     * @param fxId the fx:id of the node.
     */
    public static void detachChild(String fxId) {
        JfxPlatform.runInFxThread(() -> {

            javafx.scene.Node node = uiscene.lookup("#" + fxId);

            if (node != null) {
                uiscene.getChildren().remove(node);
                recursivelyNotifyChildrenRemoved(node);
            }
        });
    }

    /**
     * Get a control from the scene with the given fx:id
     * @param fxId the String fx:id if the node.
     * @return the node with the given name, or null if the node was not found.
     */
    public static javafx.scene.Node getChild(String fxId) {
        return uiscene.lookup("#" + fxId);
    }

    /**
     * Removes all children from the GUI scene.
     */
    public static void removeAllChildren() {
        JfxPlatform.runInFxThread(() -> {

            // remove the children before we notify them.

            List<javafx.scene.Node> children = new ArrayList<>(uiscene.getChildren());
            uiscene.getChildren().clear();

            children.forEach(JavaFxUI::recursivelyNotifyChildrenRemoved);
        });
    }

    private static void recursivelyNotifyChildrenRemoved(javafx.scene.Node node) {

        // we can do these things in a single execution, rather than individual calls.
        boolean sceneNotifier = node instanceof SceneNotifier;
        boolean jmeUpdateLoop = node instanceof JmeUpdateLoop;

        if (sceneNotifier || jmeUpdateLoop) {

            app.enqueue(() -> {
                if (sceneNotifier) {
                    ((SceneNotifier)node).onDetached();
                }

                if (jmeUpdateLoop) {
                    updatingItems.remove(node);
                }
            });
        }

        if (node instanceof Parent) {
            Parent parent = (Parent) node;
            parent.getChildrenUnmodifiable().forEach(JavaFxUI::recursivelyNotifyChildrenRemoved);
        }
    }

    private static void recursivelyNotifyChildrenAdded(javafx.scene.Node node) {

        // we can do these things in a single execution, rather than individual calls.
        boolean sceneNotifier = node instanceof SceneNotifier;
        boolean jmeUpdateLoop = node instanceof JmeUpdateLoop;

        if (sceneNotifier || jmeUpdateLoop) {

            app.enqueue(() -> {
                if (sceneNotifier) {
                    ((SceneNotifier)node).onAttached(app);
                }

                if (jmeUpdateLoop) {
                    updatingItems.add(((JmeUpdateLoop)node));
                }
            });
        }

        if (node instanceof Parent) {
            Parent parent = (Parent) node;
            parent.getChildrenUnmodifiable().forEach(JavaFxUI::recursivelyNotifyChildrenAdded);
        }
    }

    /**
     * Display a javafx.scene.Node as a centered dialog.
     * A dimmed background will be drawn behind the node and any click events will be ignored
     * on GUI items behind it.
     * @param node the node to display as a dialog.
     */
    public static void showDialog(javafx.scene.Node node) {
        showDialog(node, true);
    }

    /**
     * Display a javafx.scene.Node as a centered dialog.
     * A dimmed or transparent background will be drawn behind the node and any click events will be ignored
     * on GUI items behind it.
     * @param node   the node to display as a dialog.
     * @param dimmed whether or not to dim the scene behind the given node.
     */
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

    /**
     * Removes the shown dialog from the scene.
     */
    public static void removeDialog() {
        JfxPlatform.runInFxThread(() -> {
            dialogAnchorPanel.getChildren().clear();
            uiscene.getChildren().remove(dialogAnchorPanel);
        });

        updatingItems.remove(dialog);
        dialog = null;
    }

    /**
     * Execute a task on the JavaFX thread.
     * @param task the task to execute.
     */
    public static void runInJavaFxThread(Runnable task) {
        Platform.runLater(task);
    }

    /**
     * Execute a task on the Jmonkey GL thread.
     * @param task the task to execute.
     */
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

                if (log.isDebugEnabled()) {
                    log.debug("Bounds changing from [" + camWidth + "x" + camHeight + "] to [" + cam.getWidth() + "x" + cam.getHeight() + "]");
                }

                camWidth = cam.getWidth();
                camHeight = cam.getHeight();
                refreshSceneBounds();

                if (log.isDebugEnabled()) {
                    log.debug("Bounds refreshed.");
                }
            }

            if (container.isNeedWriteToJme()) {
                container.writeToJme();
            }

            updatingItems.forEach(item -> item.update(tpf));
        }
    }

}
