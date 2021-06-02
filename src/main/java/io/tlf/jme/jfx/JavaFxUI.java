package io.tlf.jme.jfx;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import io.tlf.jme.jfx.impl.JmeUpdateLoop;
import io.tlf.jme.jfx.impl.SceneNotifier;
import io.tlf.jme.jfx.injme.JmeFxContainer;
import io.tlf.jme.jfx.injme.JmeFxContainerImpl;
import io.tlf.jme.jfx.util.JfxPlatform;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class JavaFxUI {

    private static final Logger LOGGER = Logger.getLogger(JavaFxUI.class.getName());

    private static JavaFxUI INSTANCE;
    private static boolean installAdapter = true;

    private final Application app;
    private JmeFxContainerImpl container;

    // the general overlay
    private final Group group;
    private Scene scene = null;
    private final AnchorPane uiscene;

    // dialog - overlays an anchorpane to stop clicking background items and allows "darkening" too.
    private final AnchorPane dialogAnchorPanel;
    private javafx.scene.Node dialog;
    private ChangeListener<Bounds> dialogBoundsListener;

    private final List<JmeUpdateLoop> updatingItems = new ArrayList<>();

    private int camWidth, camHeight;

    private JavaFxUI(Application application, boolean installAdapter, String... cssStyles) {

        app = application;

        Node guiNode = ((SimpleApplication) application).getGuiNode();
        if (installAdapter) {
            container = (JmeFxContainerImpl) JmeFxContainer.install(application, guiNode);
        }

        group = new Group();
        uiscene = new AnchorPane();
        uiscene.setMinWidth(app.getCamera().getWidth());
        uiscene.setMinHeight(app.getCamera().getHeight());
        group.getChildren().add(uiscene);

        if (!installAdapter) {
            uiscene.setPickOnBounds(false);
            group.setPickOnBounds(false);
        }

        if (installAdapter) {
            scene = new Scene(group, app.getCamera().getWidth(), app.getCamera().getHeight());
            scene.setFill(Color.TRANSPARENT);

            if (cssStyles != null) {
                scene.getStylesheets().addAll(cssStyles);
            }

            container.setScene(scene, group);
        }

        dialogAnchorPanel = new AnchorPane();
        dialogAnchorPanel.setMinWidth(app.getCamera().getWidth());
        dialogAnchorPanel.setMinHeight(app.getCamera().getHeight());

        // we get the screen bounds now - as soon as possible - because the bound check is done in an AppState.
        // By the time the AppState is initialized the screen size could have changed and our checks would fail.
        camWidth = application.getCamera().getWidth();
        camHeight = application.getCamera().getHeight();

        if (installAdapter) {
            application.getStateManager().attach(new JavaFxUpdater());

            //Handling now cross input
            //Adding input handler
            JmeMemoryInputHandler memoryInputHandler = new JmeMemoryInputHandler();
            app.getInputManager().addRawInputListener(memoryInputHandler);
            //Set allowed to consume function
            io.tlf.jme.jfx.JmeFxEventConsumeAllowedFunction allowedFunction = new io.tlf.jme.jfx.JmeFxEventConsumeAllowedFunction(memoryInputHandler);
            container.getInputListener().setAllowedToConsumeInputEventFunction(allowedFunction);
        }
    }

    /**
     * Initializes the JavaFxUI class ready for use.
     * This initialization must be called first before this class is ready for use.
     *
     * @param application the Jmonkey Application.
     * @param cssStyles   The global css stylesheets.
     */
    public static void initialize(Application application, String... cssStyles) {
        INSTANCE = new JavaFxUI(application, installAdapter, cssStyles);
    }

    public static void setInstallAdapter(boolean installAdapter) {
        JavaFxUI.installAdapter = installAdapter;
    }

    public static JavaFxUI getInstance() {
        return INSTANCE;
    }

    public Group getGroup() {
        return group;
    }

    public Scene getScene() {
        return scene;
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

    /**
     * Attach a javafx.scene.Node to the GUI scene.
     *
     * @param node the node to attach to the scene.
     */
    public void attachChild(javafx.scene.Node node) {
        JfxPlatform.runInFxThread(() -> {
            uiscene.getChildren().add(node);
            recursivelyNotifyChildrenAdded(node);
        });
    }

    /**
     * Detach a node from the GUI scene.
     *
     * @param node the node to detach from the scene.
     */
    public void detachChild(javafx.scene.Node node) {
        JfxPlatform.runInFxThread(() -> {
            uiscene.getChildren().remove(node);
            recursivelyNotifyChildrenRemoved(node);
        });
    }

    /**
     * Detach a node from the GUI scene.
     *
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
     *
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
                    ((SceneNotifier) node).onDetached();
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
                    ((SceneNotifier) node).onAttached(app);
                }

                if (jmeUpdateLoop) {
                    updatingItems.add(((JmeUpdateLoop) node));
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
     *
     * @param node the node to display as a dialog.
     */
    public void showDialog(javafx.scene.Node node) {
        showDialog(node, true);
    }

    /**
     * Display a javafx.scene.Node as a centered dialog.
     * A dimmed or transparent background will be drawn behind the node and any click events will be ignored
     * on GUI items behind it.
     *
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

        if (dialog instanceof JmeUpdateLoop) {
            updatingItems.remove(dialog);
        }

        dialog.boundsInParentProperty().removeListener(dialogBoundsListener);
        dialog = null;
        dialogBoundsListener = null;
    }

    /**
     * Execute a task on the JavaFX thread.
     *
     * @param task the task to execute.
     */
    public void runInJavaFxThread(Runnable task) {
        Platform.runLater(task);
    }

    /**
     * Execute a task on the Jmonkey GL thread.
     *
     * @param task the task to execute.
     */
    public void runInJmeThread(Runnable task) {
        app.enqueue(task);
    }

    /**
     * Get the JmeFxContainer that is being used to manage Jfx with Jme.
     *
     * @return the current implementation of JmeFxContainer in use.
     */
    public JmeFxContainer getJmeFxContainer() {
        return container;
    }

    private class JavaFxUpdater extends BaseAppState {

        private Camera cam;


        @Override
        protected void initialize(Application app) {
            cam = app.getCamera();

        }

        @Override
        protected void cleanup(Application app) {
        }

        @Override
        protected void onEnable() {
        }

        @Override
        protected void onDisable() {
        }

        @Override
        public void update(float tpf) {

            if (camWidth != cam.getWidth() || camHeight != cam.getHeight()) {
                camWidth = cam.getWidth();
                camHeight = cam.getHeight();
            }

            if (container.isNeedWriteToJme()) {
                container.writeToJme();
            }

            updatingItems.forEach(item -> item.update(tpf));
        }
    }

}
