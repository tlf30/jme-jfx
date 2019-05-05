package com.jayfella.jme.jfx.injme.util;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.lwjgl.LwjglWindow;
import org.lwjgl.glfw.GLFW;

import java.nio.IntBuffer;

import static com.jme3.util.BufferUtils.createIntBuffer;
import static java.lang.ThreadLocal.withInitial;

// import org.jetbrains.annotations.NotNull;

/**
 * The utility class to work with jME contexts.
 *
 * @author JavaSaBr
 */
public class JmeWindowUtils {

    private static final ThreadLocal<IntBuffer> LOCAL_FIRST_INT_BUFFER = withInitial(() -> createIntBuffer(1));
    private static final ThreadLocal<IntBuffer> LOCAL_SECOND_INT_BUFFER = withInitial(() -> createIntBuffer(1));

    public static int getX(JmeContext context) {

        final LwjglWindow lwjglContext = (LwjglWindow) context;
        final long windowHandle = lwjglContext.getWindowHandle();

        final IntBuffer x = LOCAL_FIRST_INT_BUFFER.get();
        final IntBuffer y = LOCAL_SECOND_INT_BUFFER.get();
        x.clear();
        y.clear();

        GLFW.glfwGetWindowPos(windowHandle, x, y);

        return x.get(0);
    }

    public static int getY(final JmeContext context) {

        final LwjglWindow lwjglContext = (LwjglWindow) context;
        final long windowHandle = lwjglContext.getWindowHandle();

        final IntBuffer x = LOCAL_FIRST_INT_BUFFER.get();
        final IntBuffer y = LOCAL_SECOND_INT_BUFFER.get();
        x.clear();
        y.clear();

        GLFW.glfwGetWindowPos(windowHandle, x, y);

        return y.get(0);
    }

    public static int getWidth(final JmeContext context) {

        final LwjglWindow lwjglContext = (LwjglWindow) context;
        final long windowHandle = lwjglContext.getWindowHandle();

        final IntBuffer width = LOCAL_FIRST_INT_BUFFER.get();
        final IntBuffer height = LOCAL_SECOND_INT_BUFFER.get();
        width.clear();
        height.clear();

        GLFW.glfwGetWindowSize(windowHandle, width, height);

        return width.get(0);
    }

    public static int getHeight(final JmeContext context) {

        final LwjglWindow lwjglContext = (LwjglWindow) context;
        final long windowHandle = lwjglContext.getWindowHandle();

        final IntBuffer width = LOCAL_FIRST_INT_BUFFER.get();
        final IntBuffer height = LOCAL_SECOND_INT_BUFFER.get();
        width.clear();
        height.clear();

        GLFW.glfwGetWindowSize(windowHandle, width, height);

        return height.get(0);
    }

    public static boolean isFullscreen(final JmeContext jmeContext) {
        final AppSettings settings = jmeContext.getSettings();
        return settings.isFullscreen();
    }

    public static void requestFocus(final Application application) {
        final LwjglWindow lwjglContext = (LwjglWindow) application.getContext();
        GLFW.glfwShowWindow(lwjglContext.getWindowHandle());
    }
}
