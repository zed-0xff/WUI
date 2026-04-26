package me.zed_0xff.XUI;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** GLFW cursor handles. Call {@link #create} after GLFW init, {@link #destroy} on shutdown. */
public final class CursorMgr {
    // Tile names expected in theme["cursors"], in load order.
    private static final String[] TILE_NAMES = {
        "arrow", "resizeH", "resizeV", "resizeNWSE", "resizeNESW", "text", "hand", "clock"
    };
    private static final Map<String, Long> handles = new LinkedHashMap<>();
    private static boolean initialized;
    private CursorMgr() {}

    static long arrow()   { return handles.getOrDefault("arrow",      0L); }
    static long resizeH() { return handles.getOrDefault("resizeH",    0L); }
    static long resizeV() { return handles.getOrDefault("resizeV",    0L); }
    static long curNWSE() { return handles.getOrDefault("resizeNWSE", 0L); }
    static long curNESW() { return handles.getOrDefault("resizeNESW", 0L); }
    static long text()    { return handles.getOrDefault("text",       0L); }
    static long hand()    { return handles.getOrDefault("hand",       0L); }
    static long clock()   { return handles.getOrDefault("clock",      0L); }

    /** Load custom cursors from classpath; falls back to GLFW standard cursors on any failure. */
    public static void init() {
        if (initialized) return;
        initialized = true;

        Atlas atlas = ControlStyle.cursorAtlas();
        if (atlas == null || !atlas.isLoaded() || atlas.tiles == null
                || !atlas.tiles.keySet().containsAll(Arrays.asList(TILE_NAMES))) {
            createStandard(); return;
        }

        long[] h = new long[TILE_NAMES.length];
        for (int i = 0; i < TILE_NAMES.length; i++) {
            Atlas.TileJson tile = atlas.tiles.get(TILE_NAMES[i]);
            if (!atlas.fits(tile)) { destroyRange(h, i); createStandard(); return; }
            int hx = tile.getMetaInt("hx", tile.w / 2);
            int hy = tile.getMetaInt("hy", tile.h / 2);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                GLFWImage img = GLFWImage.malloc(stack);
                img.set(tile.w, tile.h, atlas.cellToRgba(tile));
                h[i] = GLFW.glfwCreateCursor(img, hx, hy);
            }
            if (h[i] == 0) { destroyRange(h, i); createStandard(); return; }
        }
        for (int i = 0; i < TILE_NAMES.length; i++) handles.put(TILE_NAMES[i], h[i]);
    }

    public static void destroy() {
        if (!initialized) return;
        initialized = false;
        HashSet<Long> seen = new HashSet<>();
        for (long c : handles.values())
            if (c != 0 && seen.add(c)) GLFW.glfwDestroyCursor(c);
        handles.clear();
    }

    /** Set cursor; {@code 0} maps to {@link #arrow()}. */
    public static void set(long win, long cursor) {
        GLFW.glfwSetCursor(win, cursor == 0 ? arrow() : cursor);
    }

    /** Reset to OS default (use when mouse is outside all windows). */
    public static void setDefault(long win) { GLFW.glfwSetCursor(win, 0); }

    private static void createStandard() {
        handles.put("arrow",      GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
        handles.put("resizeH",    GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR));
        handles.put("resizeV",    GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR));
        handles.put("text",       GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR));
        handles.put("hand",       GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
        handles.put("clock",      GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
        long nwse = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR);
        long nesw = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR);
        if (nwse == 0) nwse = GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR);
        if (nwse == 0) nwse = resizeH();
        if (nesw == 0) nesw = GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR);
        if (nesw == 0) nesw = resizeV();
        handles.put("resizeNWSE", nwse);
        handles.put("resizeNESW", nesw);
    }

    private static void destroyRange(long[] h, int count) {
        for (int j = 0; j < count; j++) if (h[j] != 0) GLFW.glfwDestroyCursor(h[j]);
    }
}
