package me.zed_0xff.WUI;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

/**
 * Custom WUI cursor handles for hosts that drive WUI without {@link CursorMgr}.
 *
 * <p>Call from the host's GLFW/render context thread. Falls back to standard
 * GLFW cursors when the cursor atlas cannot be loaded.
 */
public final class HostCursorProvider {
    private static boolean initialized;
    private static long arrow;
    private static long hand;
    private static long resizeH;
    private static long resizeV;
    private static long resizeNWSE;
    private static long resizeNESW;

    private HostCursorProvider() {}

    public static long handleFor(int cursorKind) {
        ensureInitialized();
        switch (cursorKind) {
            case Window.HOST_CURSOR_ARROW: return arrow;
            case Window.HOST_CURSOR_HAND: return hand;
            case Window.HOST_CURSOR_RESIZE_H: return resizeH;
            case Window.HOST_CURSOR_RESIZE_V: return resizeV;
            case Window.HOST_CURSOR_RESIZE_NWSE: return resizeNWSE;
            case Window.HOST_CURSOR_RESIZE_NESW: return resizeNESW;
            default: return 0;
        }
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;

        Atlas atlas = ControlStyle.cursorAtlas();
        if (atlas != null && atlas.isLoaded() && atlas.tiles != null) {
            arrow = createCursor(atlas, "arrow", GLFW.GLFW_ARROW_CURSOR);
            hand = createCursor(atlas, "hand", GLFW.GLFW_HAND_CURSOR);
            resizeH = createCursor(atlas, "resizeH", GLFW.GLFW_HRESIZE_CURSOR);
            resizeV = createCursor(atlas, "resizeV", GLFW.GLFW_VRESIZE_CURSOR);
            resizeNWSE = createCursor(atlas, "resizeNWSE", GLFW.GLFW_RESIZE_NWSE_CURSOR);
            resizeNESW = createCursor(atlas, "resizeNESW", GLFW.GLFW_RESIZE_NESW_CURSOR);
        }

        if (arrow == 0) arrow = standard(GLFW.GLFW_ARROW_CURSOR);
        if (hand == 0) hand = standard(GLFW.GLFW_HAND_CURSOR);
        if (resizeH == 0) resizeH = standard(GLFW.GLFW_HRESIZE_CURSOR);
        if (resizeV == 0) resizeV = standard(GLFW.GLFW_VRESIZE_CURSOR);
        if (resizeNWSE == 0) resizeNWSE = standard(GLFW.GLFW_RESIZE_NWSE_CURSOR);
        if (resizeNWSE == 0) resizeNWSE = standard(GLFW.GLFW_CROSSHAIR_CURSOR);
        if (resizeNESW == 0) resizeNESW = standard(GLFW.GLFW_RESIZE_NESW_CURSOR);
        if (resizeNESW == 0) resizeNESW = standard(GLFW.GLFW_CROSSHAIR_CURSOR);
    }

    private static long createCursor(Atlas atlas, String name, int fallbackShape) {
        Atlas.TileJson tile = atlas.tiles.get(name);
        if (!atlas.fits(tile)) {
            return standard(fallbackShape);
        }
        int hx = tile.getMetaInt("hx", tile.w / 2);
        int hy = tile.getMetaInt("hy", tile.h / 2);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage img = GLFWImage.malloc(stack);
            img.set(tile.w, tile.h, atlas.cellToRgba(tile));
            return GLFW.glfwCreateCursor(img, hx, hy);
        }
    }

    private static long standard(int shape) {
        return GLFW.glfwCreateStandardCursor(shape);
    }
}
