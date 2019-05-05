package com.jayfella.jme.jfx.injfx.transfer.impl;

import com.jayfella.jme.jfx.injfx.processor.FrameTransferSceneProcessor.TransferMode;
import com.jayfella.jme.jfx.util.JfxPlatform;
import com.jme3.texture.FrameBuffer;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * The class for transferring a frame from jME to {@link ImageView}.
 *
 * @author JavaSaBr
 */
public class ImageFrameTransfer extends AbstractFrameTransfer<ImageView> {

    private WritableImage writableImage;

    public ImageFrameTransfer(ImageView imageView, TransferMode transferMode, int width, int height) {
        this(imageView, transferMode, null, width, height);
    }

    public ImageFrameTransfer(
            ImageView imageView,
            TransferMode transferMode,
            FrameBuffer frameBuffer,
            int width,
            int height
    ) {
        super(imageView, transferMode, frameBuffer, width, height);
        JfxPlatform.runInFxThread(() -> imageView.setImage(writableImage));
    }

    @Override
    protected PixelWriter getPixelWriter(
            ImageView destination,
            FrameBuffer frameBuffer,
            int width,
            int height
    ) {
        writableImage = new WritableImage(width, height);
        return writableImage.getPixelWriter();
    }
}
