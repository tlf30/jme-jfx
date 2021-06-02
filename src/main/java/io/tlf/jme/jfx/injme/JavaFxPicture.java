package io.tlf.jme.jfx.injme;

import com.jme3.system.JmeContext;
import com.jme3.ui.Picture;
import com.sun.javafx.embed.EmbeddedStageInterface;

import java.util.logging.Logger;

import static io.tlf.jme.jfx.injme.util.JmeWindowUtils.*;

/**
 * The implementation of the {@link Picture} to represent javaFX UI Scene.
 *
 * @author JavaSaBr
 */
public class JavaFxPicture extends Picture {

    private static final Logger LOGGER = Logger.getLogger(JavaFxPicture.class.getName());

    /**
     * The JavaFX container.
     */
    private final JmeFxContainerInternal container;

    public JavaFxPicture(final JmeFxContainerInternal container) {
        super("JavaFxContainer", true);
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
    public void updateLogicalState(final float tpf) {

        final JmeFxContainerInternal container = getContainer();
        final JmeContext jmeContext = container.getJmeContext();
        try {

            final EmbeddedStageInterface stageInterface = container.getStageInterface();
            if (stageInterface == null) {
                return;
            }

            final int windowWidth = getWidth(jmeContext);
            final int windowHeight = getHeight(jmeContext);

            if (windowWidth != container.getSceneWidth() || windowHeight != container.getSceneHeight()) {
                container.fitSceneToWindowSize();
            }

            final int currentX = getX(jmeContext);
            final int currentY = getY(jmeContext);

            if (container.getPositionX() != currentX || container.getPositionY() != currentY) {
                container.move(currentX, currentY);
            }

        } finally {
            super.updateLogicalState(tpf);
        }
    }
}
