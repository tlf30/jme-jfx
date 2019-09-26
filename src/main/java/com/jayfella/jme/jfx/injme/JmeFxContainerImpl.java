package com.jayfella.jme.jfx.injme;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jayfella.jme.jfx.injme.cursor.CursorDisplayProvider;
import com.jayfella.jme.jfx.injme.cursor.proton.ProtonCursorProvider;
import com.jayfella.jme.jfx.injme.input.JmeFXInputListener;
import com.jayfella.jme.jfx.injme.util.JmeWindowUtils;
import com.jayfella.jme.jfx.lock.AsyncReadSyncWriteLock;
import com.jayfella.jme.jfx.lock.FinalAtomicReadWriteLock;
import com.jayfella.jme.jfx.util.JfxPlatform;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.system.JmeContext;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import com.sun.glass.ui.Pixels;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.stage.EmbeddedWindow;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * The container which interacts with jME and includes javaFX scene.
 *
 * @author abies / Artur Biesiadowski / JavaSaBr
 */
@SuppressWarnings("WeakerAccess")
public class JmeFxContainerImpl implements JmeFxContainer, JmeFxContainerInternal {

    private static final Logger LOGGER = LoggerFactory.getLogger(JfxPlatform.class);

    private static final int MIN_RESIZE_INTERVAL = 300;

    /**
     * Build the JavaFX container for the application.
     *
     * @param application the application.
     * @param guiNode     the GUI node.
     * @return the javaFX container.
     */
    public static JmeFxContainerImpl install(final Application application, final Node guiNode) {
        return install(application, guiNode, new ProtonCursorProvider(application, application.getAssetManager(),
                application.getInputManager()));
    }

    /**
     * Build the JavaFX container for the application.
     *
     * @param application    the application.
     * @param guiNode        the GUI node.
     * @param cursorProvider the cursor provider.
     * @return the javaFX container.
     */
    public static JmeFxContainerImpl install(final Application application, final Node guiNode,
                                                      final CursorDisplayProvider cursorProvider) {

        final JmeFxContainerImpl container = new JmeFxContainerImpl(application.getAssetManager(), application, cursorProvider);
        guiNode.attachChild(container.getJmeNode());

        final InputManager inputManager = application.getInputManager();
        inputManager.addRawInputListener(container.getInputListener());

        return container;
    }

    // TODO benchmark
    private static Void reorder_ARGB82ABGR8(final ByteBuffer data) {

        final int limit = data.limit() - 3;

        byte v;

        for (int i = 0; i < limit; i += 4) {
            v = data.get(i + 1);
            data.put(i + 1, data.get(i + 3));
            data.put(i + 3, v);
        }

        return null;
    }

    // TODO benchmark
    private static Void reorder_BGRA82ABGR8(final ByteBuffer data) {

        final int limit = data.limit() - 3;

        byte v0, v1, v2, v3;

        for (int i = 0; i < limit; i += 4) {
            v0 = data.get(i);
            v1 = data.get(i + 1);
            v2 = data.get(i + 2);
            v3 = data.get(i + 3);
            data.put(i, v3);
            data.put(i + 1, v0);
            data.put(i + 2, v1);
            data.put(i + 3, v2);
        }

        return null;
    }

    /**
     * The state to attach/detach javaFX UI.
     */
    private final AppState fxAppState = new AbstractAppState() {

        @Override
        public void cleanup() {
            Platform.exit();
            super.cleanup();
        }
    };

    protected volatile CompletableFuture<Format> nativeFormat = new CompletableFuture<>();

    /**
     * The count of frames which need to write to JME.
     */
    private final AtomicInteger waitCount;

    /**
     * The lock to control transfer frames from javaFX to JME.
     */
    private final AsyncReadSyncWriteLock imageLock;

    /**
     * The image node to present javaFX scene.
     */
    private final Picture picture;

    /**
     * The texture to present javaFX scene.
     */
    private final Texture2D texture;

    /**
     * The jMe context.
     */
    private final JmeContext jmeContext;

    /**
     * The jME application.
     */
    protected final Application application;

    /**
     * The current cursor provider.
     */
    protected final CursorDisplayProvider cursorProvider;

    /**
     * The host interface.
     */
    protected final HostInterface hostInterface;

    /**
     * The user input listener.
     */
    protected volatile JmeFXInputListener inputListener;

    /**
     * The current embedded stage interface.
     */
    protected volatile EmbeddedStageInterface stageInterface;

    /**
     * The current embedded scene interface.
     */
    protected volatile EmbeddedSceneInterface sceneInterface;

    /**
     * The embedded window.
     */
    protected volatile EmbeddedWindow embeddedWindow;

    /**
     * The current scene.
     */
    protected volatile Scene scene;

    /**
     * The root UI node.
     */
    protected volatile Group rootNode;

    /**
     * The image of jME presentation of javaFX frame.
     */
    protected volatile Image jmeImage;

    /**
     * The data of javaFX frame on the jME side.
     */
    protected volatile ByteBuffer jmeData;

    /**
     * The data buffer of javaFX frame on javaFX side.
     */
    protected volatile ByteBuffer fxData;

    /**
     * The temp data to transfer frames between javaFX and jME.
     */
    protected volatile ByteBuffer tempData;

    /**
     * The int presentation of the {@link #tempData}.
     */
    protected volatile IntBuffer tempIntData;

    /**
     * The function to reorder pixels.
     */
    protected volatile Function<ByteBuffer, Void> reorderData;

    /**
     * The time of last resized window.
     */
    protected volatile long lastResized;

    /**
     * The width of javaFX scene.
     */
    protected volatile int sceneWidth;

    /**
     * The height of javaFX scene.
     */
    protected volatile int sceneHeight;

    /**
     * The X position of this container.
     */
    protected volatile int positionX;

    /**
     * The Y position of this container.
     */
    protected volatile int positionY;

    /**
     * The flag of having focused.
     */
    protected volatile boolean focused;

    /**
     * The flag of supporting full screen.
     */
    protected volatile boolean fullScreenSupport;

    /**
     * The flag of visibility cursor.
     */
    protected volatile boolean visibleCursor;

    /**
     * The flag of enabling javaFX.
     */
    protected volatile boolean enabled;

    protected JmeFxContainerImpl(final AssetManager assetManager, final Application application,
                                 final CursorDisplayProvider cursorProvider) {
        this.initFx();
        this.positionY = -1;
        this.positionX = -1;
        this.jmeContext = application.getContext();
        this.waitCount = new AtomicInteger();
        this.imageLock = new FinalAtomicReadWriteLock(); //LockFactory.newAtomicARSWLock();
        this.cursorProvider = cursorProvider;
        this.application = application;
        this.visibleCursor = true;
        this.inputListener = new JmeFXInputListener(this);

        final AppStateManager stateManager = application.getStateManager();
        stateManager.attach(fxAppState);

        this.hostInterface = new JmeFxHostInterface(this);
        this.picture = new JavaFxPicture(this);
        this.picture.move(0, 0, -1);
        this.picture.setPosition(0, 0);
        this.texture = new Texture2D(new Image());
        this.picture.setTexture(assetManager, texture, true);

        fitSceneToWindowSize();
    }

    @Override
    public void requestPreferredSize(final int width, final int height) {
    }

    @Override
    public boolean requestFocus() {
        return true;
    }

    /**
     * Sets the last timestamp of context resizing.
     *
     * @param time the last timestamp of context resizing.
     */
    private void setLastResized(final long time) {
        this.lastResized = time;
    }

    /**
     * Gets the last timestamp of context resizing.
     *
     * @return the last timestamp of context resizing.
     */
    private long getLastResized() {
        return lastResized;
    }

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public JmeContext getJmeContext() {
        return jmeContext;
    }

    @Override
    public CursorDisplayProvider getCursorProvider() {
        return cursorProvider;
    }

    /**
     * Gets the lock to control transferring frames.
     *
     * @return the lock.
     */
    private AsyncReadSyncWriteLock getImageLock() {
        return imageLock;
    }

    @Override
    public JmeFXInputListener getInputListener() {
        return inputListener;
    }

    /**
     * Gets the image of jME presentation of javaFX frame.
     *
     * @return the image of jME presentation of javaFX frame.
     */
    private Image getJmeImage() {
        return jmeImage;
    }

    /**
     * Gets the image node to present javaFX scene.
     *
     * @return the image node to present javaFX scene.
     */
    private Picture getJmeNode() {
        return picture;
    }

    @Override
    public int getPositionX() {
        return positionX;
    }

    @Override
    public void setPositionX(final int positionX) {
        this.positionX = positionX;
    }

    @Override
    public int getPositionY() {
        return positionY;
    }

    @Override
    public void setPositionY(final int positionY) {
        this.positionY = positionY;
    }

    /**
     * Gets the image node to present javaFX scene.
     *
     * @return the image node to present javaFX scene.
     */
    private Picture getPicture() {
        return picture;
    }

    @Override
    public int getSceneHeight() {
        return sceneHeight;
    }

    @Override
    public void setSceneHeight(final int sceneHeight) {
        this.sceneHeight = sceneHeight;
    }

    @Override
    public int getSceneWidth() {
        return sceneWidth;
    }

    @Override
    public void setSceneWidth(final int sceneWidth) {
        this.sceneWidth = sceneWidth;
    }

    @Override
    public float getPixelScaleFactor() {
        return 1.0F;
    }

    /**
     * Gets the function to reorder pixels.
     *
     * @return the function to reorder pixels.
     */
    private Function<ByteBuffer, Void> getReorderData() {
        return reorderData;
    }

    @Override
    public Group getRootNode() {
        return rootNode;
    }

    @Override
    public Scene getScene() {
        return scene;
    }

    @Override
    public EmbeddedSceneInterface getSceneInterface() {
        return sceneInterface;
    }

    @Override
    public void setSceneInterface(final EmbeddedSceneInterface sceneInterface) {
        this.sceneInterface = sceneInterface;
    }

    @Override
    public EmbeddedWindow getEmbeddedWindow() {
        return embeddedWindow;
    }

    @Override
    public void setEmbeddedWindow(final EmbeddedWindow embeddedWindow) {
        this.embeddedWindow = embeddedWindow;
    }

    @Override
    public EmbeddedStageInterface getStageInterface() {
        return stageInterface;
    }

    @Override
    public void setStageInterface(final EmbeddedStageInterface stageInterface) {
        this.stageInterface = stageInterface;
    }

    /**
     * Gets the data buffer of javaFX frame on javaFX side.
     *
     * @return the data buffer.
     */
    private ByteBuffer getFxData() {
        return fxData;
    }

    /**
     * Gets the data of javaFX frame on the jME side.
     *
     * @return the data of javaFX frame on the jME side.
     */
    private ByteBuffer getJmeData() {
        return jmeData;
    }

    /**
     * Gets the temp data to transfer frames between javaFX and jME.
     *
     * @return the temp data to transfer frames between javaFX and jME.
     */
    private ByteBuffer getTempData() {
        return tempData;
    }

    /**
     * Gets the int presentation of the tempData.
     *
     * @return the int presentation of the tempData.
     */
    private IntBuffer getTempIntData() {
        return tempIntData;
    }

    /**
     * Gets the texture to present javaFX scene.
     *
     * @return the texture to present javaFX scene.
     */
    private Texture2D getTexture() {
        return texture;
    }

    /**
     * Gets the the count of waited frames.
     *
     * @return the count of waited frames.
     */
    private AtomicInteger getWaitCount() {
        return waitCount;
    }

    @Override
    public void grabFocus() {

        final EmbeddedStageInterface stageInterface = getStageInterface();
        if (isFocused() || stageInterface == null) return;

        stageInterface.setFocused(true, AbstractEvents.FOCUSEVENT_ACTIVATED);
        setFocused(true);

        LOGGER.debug("got focused.");
    }

    @Override
    public void fitSceneToWindowSize() {

        final long time = System.currentTimeMillis();
        if (time - getLastResized() < MIN_RESIZE_INTERVAL) return;

        final JmeContext jmeContext = getJmeContext();

        final int winWidth = JmeWindowUtils.getWidth(jmeContext);
        final int winHeight = JmeWindowUtils.getHeight(jmeContext);

        final AsyncReadSyncWriteLock lock = getImageLock();
        lock.syncLock();
        try {

            final int textureWidth = Math.max(winWidth, 64);
            final int textureHeight = Math.max(winHeight, 64);

            final Picture picture = getPicture();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Fit the scene to window size from [" + getSceneWidth() + "x" + getSceneHeight() + "] to " +
                                "[" + textureWidth + "x" + textureHeight + "]");
            }

            picture.setWidth(textureWidth);
            picture.setHeight(textureHeight);

            final ByteBuffer fxData = getFxData();
            if (fxData != null) {
                BufferUtils.destroyDirectBuffer(fxData);
            }

            final ByteBuffer tempData = getTempData();
            if (tempData != null) {
                BufferUtils.destroyDirectBuffer(tempData);
            }

            final ByteBuffer jmeData = getJmeData();
            if (jmeData != null) {
                BufferUtils.destroyDirectBuffer(jmeData);
            }

            final Image jmeImage = getJmeImage();
            if (jmeImage != null) {
                jmeImage.dispose();
            }

            this.fxData = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
            this.tempData = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
            this.tempIntData = getTempData().asIntBuffer();
            this.jmeData = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
            this.jmeImage = new Image(nativeFormat.get(), textureWidth, textureHeight, getJmeData(), ColorSpace.sRGB);

            final Texture2D texture = getTexture();
            texture.setImage(getJmeImage());

            setSceneHeight(textureHeight);
            setSceneWidth(textureWidth);

            final EmbeddedStageInterface stageInterface = getStageInterface();
            final EmbeddedSceneInterface sceneInterface = getSceneInterface();

            if (stageInterface != null && sceneInterface != null) {
                JfxPlatform.runInFxThread(() -> {
                    stageInterface.setSize(textureWidth, textureHeight);
                    sceneInterface.setSize(textureWidth, textureHeight);
                    hostInterface.repaint();
                });
            }

        } catch (final Exception e) {
            LOGGER.warn("error", e);
        } finally {
            lock.syncUnlock();
        }

        setLastResized(time);
    }

    @Override
    public void move(final int positionX, final int positionY) {
        setPositionX(positionX);
        setPositionY(positionY);

        final EmbeddedStageInterface stageInterface = getStageInterface();
        if (stageInterface == null) {
            return;
        }

        JfxPlatform.runInFxThread(() -> stageInterface.setLocation(getPositionX(), getPositionY()));
    }

    private void initFx() {
        Platform.startup(() -> {
            switch (Pixels.getNativeFormat()) {
                case Pixels.Format.BYTE_ARGB:
                    try {
                        nativeFormat.complete(Format.ARGB8);
                        reorderData = null;
                    } catch (final Exception exc1) {
                        nativeFormat.complete(Format.ABGR8);
                        reorderData = JmeFxContainerImpl::reorder_ARGB82ABGR8;
                    }
                    break;
                case Pixels.Format.BYTE_BGRA_PRE:
                    try {
                        nativeFormat.complete(Format.BGRA8);
                        reorderData = null;
                    } catch (final Exception exc2) {
                        nativeFormat.complete(Format.ABGR8);
                        reorderData = JmeFxContainerImpl::reorder_BGRA82ABGR8;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Not supported javaFX pixel format " + Pixels.getNativeFormat());
            }
        });
        Platform.setImplicitExit(false);
    }

    @Override
    public boolean isCovered(final int x, final int y) {

        final Image jmeImage = getJmeImage();
        final int sceneWidth = getSceneWidth();

        if (jmeImage == null || x < 0 || x >= sceneWidth) {
            return false;
        } else if (y < 0 || y >= getSceneHeight()) {
            return false;
        }

        final ByteBuffer data = jmeImage.getData(0);
        data.limit(data.capacity());

        final int alpha = data.get(3 + 4 * (y * sceneWidth + x));

        data.limit(0);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("is covered " + x + ", " + y + " = " + (alpha != 0));
        }

        return alpha != 0;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    /**
     * @param focused true if the windows has focused.
     */
    private void setFocused(final boolean focused) {
        this.focused = focused;
    }

    /**
     * @return true if this container is supported fullscreen.
     */
    public boolean isFullScreenSupport() {
        return fullScreenSupport;
    }

    @Override
    public boolean isNeedWriteToJme() {
        return waitCount.get() > 0;
    }

    /**
     * @return true if the cursor is visible.
     */
    public boolean isVisibleCursor() {
        return visibleCursor;
    }

    /**
     * @param visibleCursor true if the cursor is visible.
     */
    public void setVisibleCursor(final boolean visibleCursor) {
        this.visibleCursor = visibleCursor;
    }

    @Override
    public void loseFocus() {

        final EmbeddedStageInterface stagePeer = getStageInterface();
        if (!isFocused() || stagePeer == null) return;

        stagePeer.setFocused(false, AbstractEvents.FOCUSEVENT_DEACTIVATED);

        setFocused(false);

        LOGGER.debug("lost focused.");
    }

    @Override
    public void requestRedraw() {

        long time = 0;

        if (LOGGER.isDebugEnabled()) {
            time = System.currentTimeMillis();
            LOGGER.debug("Started paint FX scene...");
        }

        final EmbeddedSceneInterface sceneInterface = getSceneInterface();
        if (sceneInterface == null) return;

        // final ByteBuffer tempData = notNull(getTempData());
        final ByteBuffer tempData = getTempData();
        tempData.clear();

        final int sceneWidth = getSceneWidth();
        final int sceneHeight = getSceneHeight();

        // if (!sceneInterface.getPixels(notNull(getTempIntData()), sceneWidth, sceneHeight)) {
        if (!sceneInterface.getPixels(getTempIntData(), sceneWidth, sceneHeight)) {
            return;
        }

        tempData.flip();
        tempData.limit(sceneWidth * sceneHeight * 4);

        final AsyncReadSyncWriteLock imageLock = getImageLock();
        imageLock.syncLock();
        try {

            // final ByteBuffer fxData = notNull(getFxData());
            final ByteBuffer fxData = getFxData();
            fxData.clear();
            fxData.put(tempData);
            fxData.flip();

            final Function<ByteBuffer, Void> reorderData = getReorderData();

            if (reorderData != null) {
                reorderData.apply(fxData);
                fxData.position(0);
            }

        } catch (final Exception exc) {
            LOGGER.warn(exc.getMessage(), exc);
        } finally {
            imageLock.syncUnlock();
        }

        final AtomicInteger waitCount = getWaitCount();
        waitCount.incrementAndGet();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("finished paint FX scene(" + (System.currentTimeMillis() - time) + "ms.).");
        }
    }

    @Override
    public void setScene(final Scene newScene, final Group rootNode) {
        this.rootNode = rootNode;
        JfxPlatform.runInFxThread(() -> setSceneImpl(newScene));
    }

    /**
     * Set a new scene to javaFX container.
     *
     * @param newScene the new scene.
     */
    private void setSceneImpl(final Scene newScene) {

        if (embeddedWindow != null && newScene == null) {
            embeddedWindow.hide();
            embeddedWindow = null;
        }

        // final Application application = notNull(getApplication());
        final Application application = getApplication();
        application.enqueue(() -> {
            picture.setCullHint(newScene == null ? CullHint.Always : CullHint.Never);
            return null;
        });

        this.scene = newScene;

        if (embeddedWindow == null && newScene != null) {
            embeddedWindow = new EmbeddedWindow(hostInterface);
        }

        if (embeddedWindow == null) {
            return;
        }

        embeddedWindow.setScene(newScene);

        if (!embeddedWindow.isShowing()) {
            embeddedWindow.show();
        }
    }

    @Override
    public void requestShowingCursor(final CursorFrame cursorFrame) {
        cursorProvider.show(cursorFrame);
    }

    @Override
    public Void writeToJme() {

        final AtomicInteger waitCount = getWaitCount();
        final int currentCount = waitCount.get();

        long time = 0;

        if (LOGGER.isDebugEnabled()) {
            time = System.currentTimeMillis();
            LOGGER.debug("Started writing FX data to JME...");
        }

        // final ByteBuffer jmeData = notNull(getJmeData());
        final ByteBuffer jmeData = getJmeData();
        jmeData.clear();

        final AsyncReadSyncWriteLock imageLock = getImageLock();
        imageLock.syncLock();
        try {
            // jmeData.put(notNull(getFxData()));
            jmeData.put(getFxData());
        } finally {
            imageLock.syncUnlock();
        }

        jmeData.flip();

        // final Image jmeImage = notNull(getJmeImage());
        final Image jmeImage = getJmeImage();
        jmeImage.setUpdateNeeded();

        // waitCount.subAndGet(currentCount);
        waitCount.set(waitCount.get() - currentCount);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Finished writing FX data to JME(" + (System.currentTimeMillis() - time) + "ms.).");
        }

        return null;
    }

    @Override
    public void requestEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return true if the javaFx is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
}