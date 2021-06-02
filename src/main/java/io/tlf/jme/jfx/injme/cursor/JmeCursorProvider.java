package io.tlf.jme.jfx.injme.cursor;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.InputManager;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.CursorType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * http://www.rw-designer.com/cursor-set/proton by juanello <br> A cursorProvider that simulates the
 * native JFX one and tries to behave similar,<br> using native cursors and 2D surface logic.
 *
 * @author empire
 * @author Trevor Flynn
 */
public class JmeCursorProvider implements CursorDisplayProvider {

    private final static Logger LOGGER = Logger.getLogger(JmeCursorProvider.class.getName());

    private final Map<CursorType, JmeCursor> cache = new ConcurrentHashMap<>();
    private final AssetManager assetManager;
    private final InputManager inputManager;
    private final Application application;

    public JmeCursorProvider(final Application application, final AssetManager assetManager,
                             final InputManager inputManager) {
        this.assetManager = assetManager;
        this.inputManager = inputManager;
        this.application = application;
    }

    @Override
    public void prepare(final CursorType cursorType) {

        JmeCursor jmeCursor = null;

        switch (cursorType) {
            case CROSSHAIR:
                jmeCursor = (JmeCursor) assetManager.loadAsset("Interface/Cursor/aero_cross.cur");
                break;
            case DEFAULT:
                jmeCursor = (JmeCursor) assetManager.loadAsset("Interface/Cursor/aero_arrow.cur");
                break;
            case E_RESIZE:
            case H_RESIZE:
            case W_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("Interface/Cursor/aero_ew.cur");
                break;
            case HAND:
                jmeCursor = (JmeCursor) assetManager.loadAsset("Interface/Cursor/aero_link.cur");
                break;
            case MOVE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("Interface/Cursor/aero_move.cur");
                break;
            case NE_RESIZE:
            case SW_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("Interface/Cursor/aero_nesw.cur");
                break;
            case NW_RESIZE:
            case SE_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("Interface/Cursor/aero_nwse.cur");
                break;
            case N_RESIZE:
            case V_RESIZE:
            case S_RESIZE:
                jmeCursor = (JmeCursor) assetManager.loadAsset("Interface/Cursor/aero_ns.cur");
                break;
            case TEXT:
                jmeCursor = (JmeCursor) assetManager.loadAsset("Interface/Cursor/aero_text.cur");
                break;
            case WAIT:
                jmeCursor = (JmeCursor) assetManager.loadAsset("Interface/Cursor/aero_busy.ani");
                break;
            case CLOSED_HAND:
            case OPEN_HAND:
            case DISAPPEAR:
            case IMAGE:
            case NONE:
            default:
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
            LOGGER.warning("Unknown Cursor: " + cursorType);
            cursorType = CursorType.DEFAULT;
        }

        final JmeCursor toDisplay = cache.get(cursorType);

        if (toDisplay != null) {
            application.enqueue(() -> {
                inputManager.setMouseCursor(toDisplay);
            });
        }
    }
}
