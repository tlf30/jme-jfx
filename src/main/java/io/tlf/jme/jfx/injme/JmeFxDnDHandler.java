package io.tlf.jme.jfx.injme;

import com.sun.javafx.embed.EmbeddedSceneDSInterface;
import com.sun.javafx.embed.EmbeddedSceneDTInterface;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.HostDragStartListener;
import io.tlf.jme.jfx.injme.input.JmeFXInputListener;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.TransferMode;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javafx.scene.image.PixelFormat.getByteBgraInstance;

/**
 * A very hacky implementation of a DND system, similar to SwingDND but for jme context. <br> Allows
 * for inner application drag and drop support. <br> Cross GuiManager support is untested.
 *
 * @author empire, JavaSaBr
 */
public class JmeFxDnDHandler implements HostDragStartListener {

    private static final Logger LOGGER = Logger.getLogger(JmeFxDnDHandler.class.getName());

    /**
     * The JavaFX container.
     */
    private JmeFxContainerInternal container;

    /**
     * The drop target.
     */
    private EmbeddedSceneDTInterface dropTarget;

    /**
     * The drag source.
     */
    private EmbeddedSceneDSInterface dragSource;

    /**
     * The transfer mode.
     */
    private TransferMode transferMode;

    /**
     * The drag image.
     */
    private ImageView dragImage;

    public JmeFxDnDHandler(final JmeFxContainerInternal container) {
        this.container = container;
    }

    /**
     * this is kinda ridiculous, but well at least it seems to work
     */
    private void createDragImageProxy(final Object image, final Object offset) {
        if (!(image instanceof ByteBuffer)) {
            return;
        }

        try {

            final ByteBuffer imageData = (ByteBuffer) image;
            imageData.position(0);

            final int width = imageData.getInt();
            final int height = imageData.getInt();

            final byte[] pixels = new byte[imageData.remaining()];
            imageData.get(pixels);

            final WritableImage resultImage = new WritableImage(width, height);
            final PixelWriter writer = resultImage.getPixelWriter();
            writer.setPixels(0, 0, width, height, getByteBgraInstance(), pixels, 0, width * 4);

            dragImage = new ImageView(resultImage);
            dragImage.setStyle("dragimage:true;");
            dragImage.setMouseTransparent(true);
            dragImage.setVisible(true);

            if (offset instanceof ByteBuffer) {

                final ByteBuffer offsetBuffer = (ByteBuffer) offset;
                offsetBuffer.position(0);

                // LOGGER.debug(offsetBuffer, buff -> "Img offset " + buff.getInt() + ", " + buff.getInt());
            }

        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        }
    }

    @Override
    public void dragStarted(final EmbeddedSceneDSInterface dragSource, final TransferMode transferMode) {

        final JmeFxContainerInternal container = getContainer();
        final Group rootNode = container.getRootNode();
        final JmeFXInputListener inputListener = container.getInputListener();
        final EmbeddedSceneInterface sceneInterface = container.getSceneInterface();

        if (rootNode == null) {
            LOGGER.warning("The root node is null.");
            return;
        } else if (inputListener == null) {
            LOGGER.warning("The input listener is null.");
            return;
        } else if (sceneInterface == null) {
            LOGGER.warning("The scene interface is null.");
            return;
        }

        if (dragImage != null) {
            rootNode.getChildren().remove(dragImage);
            dragImage = null;
        }

        try {

            final Object dragImage = dragSource.getData("application/x-java-drag-image");
            final Object offset = dragSource.getData("application/x-java-drag-image-offset");

            if (dragImage != null) {
                createDragImageProxy(dragImage, offset);
            }

            inputListener.setMouseDNDListener(this);

            assert transferMode == TransferMode.COPY : "Only Copy is supported currently";

            //LOGGER.debug(dragSource, transferMode, (source, mode) -> "Drag started of " + source + " in mode " + mode);

            final Clipboard clipboard = Clipboard.getSystemClipboard();

            //LOGGER.debug(this, clipboard, clip -> "Clipboard : " + clip);

            this.dragSource = dragSource;
            this.dropTarget = sceneInterface.createDropTarget();
            // pseudo enter, we only support inner events, so it stays always entered
            this.dropTarget.handleDragEnter(0, 0, 0, 0, TransferMode.COPY, dragSource);

        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        }
    }

    /**
     * Gets the drag image.
     *
     * @return the drag image.
     */
    private ImageView getDragImage() {
        return dragImage;
    }

    /**
     * Gets the JavaFX container.
     *
     * @return the JavaFX container.
     */
    private JmeFxContainerInternal getContainer() {
        return container;
    }

    /**
     * Gets the drag source.
     *
     * @return the drag source.
     */
    private EmbeddedSceneDSInterface getDragSource() {
        return dragSource;
    }

    /**
     * Gets the drop target.
     *
     * @return the drop target.
     */
    private EmbeddedSceneDTInterface getDropTarget() {
        return dropTarget;
    }

    public void mouseUpdate(final int x, final int y, final boolean mousePressed) {

        final EmbeddedSceneDSInterface dragSource = getDragSource();
        final EmbeddedSceneDTInterface dropTarget = getDropTarget();

        if (dragSource == null) {
            LOGGER.warning("The drag source is null.");
            return;
        } else if (dropTarget == null) {
            LOGGER.warning("The drop target is null.");
            return;
        }

        final JmeFxContainerInternal container = getContainer();
        final Group rootNode = container.getRootNode();
        final JmeFXInputListener inputListener = container.getInputListener();
        final EmbeddedSceneInterface sceneInterface = container.getSceneInterface();

        if (rootNode == null) {
            LOGGER.warning("The root node is null.");
            return;
        } else if (inputListener == null) {
            LOGGER.warning("The input listener is null.");
            return;
        } else if (sceneInterface == null) {
            LOGGER.warning("The scene interface is null.");
            return;
        }

        final ObservableList<Node> children = rootNode.getChildren();
        final ImageView dragImage = getDragImage();
        try {

            if (mousePressed) {

                if (dragImage != null) {
                    dragImage.relocate(x, y);

                    // only add once it has a valid position
                    if (!children.contains(dragImage)) {
                        children.add(dragImage);
                    }
                }

                transferMode = dropTarget.handleDragOver(x, y, x, y, TransferMode.COPY);

            } else {

                if (dragImage != null) {
                    dragImage.setVisible(false);
                }

                if (transferMode != null) {
                    // causes exceptions when done without a target
                    transferMode = dropTarget.handleDragOver(x, y, x, y, TransferMode.COPY);
                    final TransferMode acceptedMode = dropTarget.handleDragDrop(x, y, x, y, TransferMode.COPY);
                    // Necessary to reset final the internal states, and allow final another drag drop
                    dragSource.dragDropEnd(acceptedMode);
                } else {
                    // seems to be necessary if no dragdrop attempt is being made
                    dropTarget.handleDragLeave();
                    dragSource.dragDropEnd(null);
                }

                inputListener.setMouseDNDListener(null);

                this.dragSource = null;
                this.dropTarget = null;
            }

        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        }
    }
}
