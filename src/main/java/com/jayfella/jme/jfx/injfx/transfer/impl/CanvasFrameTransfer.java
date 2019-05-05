package com.jayfella.jme.jfx.injfx.transfer.impl;

import com.jayfella.jme.jfx.injfx.processor.FrameTransferSceneProcessor.TransferMode;
import com.jme3.texture.FrameBuffer;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelWriter;

/**
 * The class for transferring content from the jME to {@link Canvas}.
 *
 * @author JavaSaBr
 */
public class CanvasFrameTransfer extends AbstractFrameTransfer<Canvas> {

    public CanvasFrameTransfer(Canvas canvas, TransferMode transferMode, int width, int height) {
        this(canvas, transferMode, null, width, height);
    }

    public CanvasFrameTransfer(
            Canvas canvas,
            TransferMode transferMode,
            FrameBuffer frameBuffer,
            int width,
            int height
    ) {
        super(canvas, transferMode, frameBuffer, width, height);
    }

    @Override
    protected PixelWriter getPixelWriter(
            Canvas destination,
            FrameBuffer frameBuffer,
            int width,
            int height
    ) {
        return destination.getGraphicsContext2D().getPixelWriter();
    }
}
