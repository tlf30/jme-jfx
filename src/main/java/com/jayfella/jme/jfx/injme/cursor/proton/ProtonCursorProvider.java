package com.jayfella.jme.jfx.injme.cursor.proton;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.InputManager;
import com.jayfella.jme.jfx.injme.cursor.CursorDisplayProvider;
import com.jayfella.jme.jfx.util.JfxPlatform;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.CursorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * http://www.rw-designer.com/cursor-set/proton by juanello <br> A cursorProvider that simulates the
 * native JFX one and tries to behave similar,<br> using native cursors and 2D surface logic.
 *
 * @author empire
 */
public class ProtonCursorProvider implements CursorDisplayProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JfxPlatform.class);
    private Map<CursorType, JmeCursor> cache = new ConcurrentHashMap<>();
    private AssetManager assetManager;
    private InputManager inputManager;
    private Application application;

    public ProtonCursorProvider(final Application application, final AssetManager assetManager,
                                final InputManager inputManager) {
        this.assetManager = assetManager;
        this.inputManager = inputManager;
        this.application = application;
        assetManager.registerLocator("", ClasspathLocator.class);
    }

    @Override
    public void prepare(final CursorType cursorType) {

        JmeCursor jmeCursor = null;

        switch (cursorType) {
            case CLOSED_HAND:
                break;
            case CROSSHAIR:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_cross.cur");
                break;
            case DEFAULT:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_arrow.cur");
                break;
            case DISAPPEAR:
                break;
            case E_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_ew.cur");
                break;
            case HAND:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_link.cur");
                break;
            case H_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_ew.cur");
                break;
            case IMAGE:
                break;
            case MOVE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_move.cur");
                break;
            case NE_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_nesw.cur");
                break;
            case NONE:
                break;
            case NW_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_nwse.cur");
                break;
            case N_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_ns.cur");
                break;
            case OPEN_HAND:
                break;
            case SE_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_nwse.cur");
                break;
            case SW_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_nesw.cur");
                break;
            case S_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_ns.cur");
                break;
            case TEXT:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_text.cur");
                break;
            case V_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_ns.cur");
                break;
            case WAIT:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_busy.ani");
                break;
            case W_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("com/jme3/jfx/cursor/proton/aero_ew.cur");
                break;
        }

        if (jmeCursor != null) {
            cache.putIfAbsent(cursorType, jmeCursor);
        }
    }

    @Override
    public void show(final CursorFrame cursorFrame) {

        CursorType cursorType = cursorFrame.getCursorType();

        if (cache.get(cursorType) == null) {
            // LOGGER.debug(this, cursorType, type -> "Unknown Cursor! " + type);
            LOGGER.debug("Unknown Cursor! " + cursorType);
            cursorType = CursorType.DEFAULT;
        }

        final JmeCursor toDisplay = cache.get(cursorType);

        if (toDisplay != null) {
            application.enqueue((Callable<Void>) () -> {
                inputManager.setMouseCursor(toDisplay);
                return null;
            });
        }
    }
}
