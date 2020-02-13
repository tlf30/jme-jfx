package com.jayfella.jme.jfx;

import com.jayfella.jme.jfx.impl.JmeUpdateLoop;
import com.jayfella.jme.jfx.impl.SceneNotifier;
import com.jayfella.jme.jfx.injme.JmeFxContainer;
import com.jayfella.jme.jfx.injme.JmeFxContainerImpl;
import com.jayfella.jme.jfx.util.JfxPlatform;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class JavaFxUI {

    private static final Logger log = LoggerFactory.getLogger(JavaFxUI.class);

    private static JavaFxUI INSTANCE;

    private Application app;
    private JmeFxContainerImpl container;

    // the general overlay
    private Group group;
    private Scene scene;
    private AnchorPane uiscene;

    Runnable removeExistingPopup = () -> {};

    // dialog - overlays an anchorpane to stop clicking background items and allows "darkening" too.
    private AnchorPane dialogAnchorPanel;
    private javafx.scene.Node dialog;
    private ChangeListener<Bounds> dialogBoundsListener;

    private final List<JmeUpdateLoop> updatingItems = new ArrayList<>();

    private int camWidth, camHeight;

    private JavaFxUI(Application application, String... cssStyles) {

        app = application;

        Node guiNode = ((SimpleApplication)application).getGuiNode();
        container = (JmeFxContainerImpl) JmeFxContainer.install(application, guiNode);
        
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

        //Handling now cross input
        //Adding input handler
        JmeMemoryInputHandler memoryInputHandler = new JmeMemoryInputHandler();
        app.getInputManager().addRawInputListener(memoryInputHandler);
        //Set allowed to consume function
        JmeFxEventConsumeAllowedFunction allowedFunction = new JmeFxEventConsumeAllowedFunction(memoryInputHandler);
        container.getInputListener().setAllowedToConsumeInputEventFunction(allowedFunction);
    }

    /**
     * Initializes the JavaFxUI class ready for use.
     * This initialization must be called first before this class is ready for use.
     * @param application the Jmonkey Application.
     * @param cssStyles   The global css stylesheets.
     */
    public static void initialize(Application application, String... cssStyles) {
        INSTANCE = new JavaFxUI(application, cssStyles);
    }

    public static JavaFxUI getInstance() {
        return INSTANCE;
    }

    /**
     * Set the input focus to JavaFx.
     */
    public void grabFocus() {
        container.grabFocus();
    }

    /**
     * Set the input focus to JME.
     */
    public void loseFocus() {
        container.loseFocus();
    }

    private void refreshSceneBounds() {

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
    public void attachChild(javafx.scene.Node node) {
        JfxPlatform.runInFxThread(() ->{
            uiscene.getChildren().add(node);
            recursivelyNotifyChildrenAdded(node);
        });
    }

    public Runnable attachPopup(javafx.scene.Node node, double x, double y){
        removeExistingPopup.run();

        AnchorPane popupOverlay = new AnchorPane();
        popupOverlay.setMinWidth(app.getCamera().getWidth());
        popupOverlay.setMinHeight(app.getCamera().getHeight());
        popupOverlay.getChildren().add(node);

        node.setTranslateX(x);
        node.setTranslateY(y);

        group.getChildren().add(popupOverlay);

        removeExistingPopup = () -> {
            group.getChildren().remove(popupOverlay);
            removeExistingPopup = () -> {};
        };

        popupOverlay.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            removeExistingPopup.run();
        });

        return removeExistingPopup;
    }

    /**
     * Detach a node from the GUI scene.
     * @param node the node to detach from the scene.
     */
    public void detachChild(javafx.scene.Node node) {
        JfxPlatform.runInFxThread(() ->{
            uiscene.getChildren().remove(node);
            recursivelyNotifyChildrenRemoved(node);
        });
    }

    /**
     * Detach a node from the GUI scene.
     * @param fxId the fx:id of the node.
     */
    public void detachChild(String fxId) {
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
    public javafx.scene.Node getChild(String fxId) {
        return uiscene.lookup("#" + fxId);
    }

    /**
     * Removes all children from the GUI scene.
     */
    public void removeAllChildren() {
        JfxPlatform.runInFxThread(() -> {

            // remove the children before we notify them.

            List<javafx.scene.Node> children = new ArrayList<>(uiscene.getChildren());
            uiscene.getChildren().clear();

            children.forEach(this::recursivelyNotifyChildrenRemoved);
        });
    }

    private void recursivelyNotifyChildrenRemoved(javafx.scene.Node node) {

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
            parent.getChildrenUnmodifiable().forEach(this::recursivelyNotifyChildrenRemoved);
        }
    }

    private void recursivelyNotifyChildrenAdded(javafx.scene.Node node) {

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
            parent.getChildrenUnmodifiable().forEach(this::recursivelyNotifyChildrenAdded);
        }
    }

    /**
     * Display a javafx.scene.Node as a centered dialog.
     * A dimmed background will be drawn behind the node and any click events will be ignored
     * on GUI items behind it.
     * @param node the node to display as a dialog.
     */
    public void showDialog(javafx.scene.Node node) {
        showDialog(node, true);
    }

    /**
     * Display a javafx.scene.Node as a centered dialog.
     * A dimmed or transparent background will be drawn behind the node and any click events will be ignored
     * on GUI items behind it.
     * @param node   the node to display as a dialog.
     * @param dimmed whether or not to dim the scene behind the given node.
     */
    public void showDialog(javafx.scene.Node node, boolean dimmed) {

        // center the dialog
        int scrWidth = app.getCamera().getWidth();
        int scrHeight = app.getCamera().getHeight();

        dialog = node;
        dialogBoundsListener = (prop, oldBounds, newBounds) -> {
        	node.setLayoutX(scrWidth * 0.5 - newBounds.getWidth() * 0.5);
        	node.setLayoutY(scrHeight * 0.5 - newBounds.getHeight() * 0.5);
		};

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
            
            node.boundsInParentProperty().addListener(dialogBoundsListener);

        });
    }

    /**
     * Removes the shown dialog from the scene.
     */
    public void removeDialog() {
        JfxPlatform.runInFxThread(() -> {
            dialogAnchorPanel.getChildren().clear();
            uiscene.getChildren().remove(dialogAnchorPanel);
        });

        updatingItems.remove(dialog);
        
        dialog.boundsInParentProperty().removeListener(dialogBoundsListener);
        dialog = null;
        dialogBoundsListener = null;
    }

    /**
     * Execute a task on the JavaFX thread.
     * @param task the task to execute.
     */
    public void runInJavaFxThread(Runnable task) {
        Platform.runLater(task);
    }

    /**
     * Execute a task on the Jmonkey GL thread.
     * @param task the task to execute.
     */
    public void runInJmeThread(Runnable task) {
        app.enqueue(task);
    }

    private class JavaFxUpdater extends BaseAppState {

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
