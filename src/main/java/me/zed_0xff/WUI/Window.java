package me.zed_0xff.WUI;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Win3.1-style faux window: title drag, resize from edges/corners, resize cursors on hover.
 * Coordinates match {@link TestApp}: origin top-left, y downward (framebuffer pixels).
 */
public class Window extends Element {
    public static final int HOST_CURSOR_DEFAULT = 0;
    public static final int HOST_CURSOR_ARROW = 1;
    public static final int HOST_CURSOR_HAND = 2;
    public static final int HOST_CURSOR_RESIZE_H = 3;
    public static final int HOST_CURSOR_RESIZE_V = 4;
    public static final int HOST_CURSOR_RESIZE_NWSE = 5;
    public static final int HOST_CURSOR_RESIZE_NESW = 6;

    Color bgColor = Color.WHITE;
    String title, status;
    protected final List<Control> controls = new ArrayList<>();

    static final ElementDecor _deco = new ElementDecor("window");

    static final int GRIP  = 8;
    static final int EDGE  = 6;
    static final int MIN_W = 120;
    static final int MIN_H = 80;
    static final int titleBarHeight = 23; // equal to top*.height in window.json

    private enum ResizeGrip {
        NONE, N, S, E, W, NE, NW, SE, SW
    }

    boolean dragging;
    int dragGrabDx;
    int dragGrabDy;

    ResizeGrip activeResize = ResizeGrip.NONE;
    int resizeSnapX;
    int resizeSnapY;
    int resizeSnapW;
    int resizeSnapH;

    public Window(int x, int y, int width, int height, String title) {
        super(x, y, width, height);
        this.title = title;
    }

    public Window addControl(java.util.function.Function<Window, Control> factory) {
        Control control = factory.apply(this);
        if (control == null) throw new IllegalArgumentException("Factory returned null");
        controls.add(control);
        return this;
    }

    public Window addRadioGroup(java.util.function.Consumer<RadioGroup> builder) {
        RadioGroup g = new RadioGroup(this);
        builder.accept(g);
        controls.addAll(g.buttons);
        return this;
    }

    public Window setTitle(String s)        { this.title = s;  return this; }
    public Window setStatus(String s)       { this.status = s; return this; }
    public Window setPosition(int x, int y) { this.x = x; this.y = y; return this; }

    public boolean contains(int mx, int my) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    public boolean containsTitleBar(int mx, int my) {
        return titleRect().contains(mx, my);
    }

    /** Title strip excluding edge grips so resize takes priority on corners/top border. */
    private boolean titleDragZone(int mx, int my) {
        if (!containsTitleBar(mx, my)) {
            return false;
        }
        Rect title = titleRect();
        if (mx < x + GRIP || mx >= x + width - GRIP) {
            return false;
        }
        return my >= title.y() + EDGE;
    }

    private Rect titleRect() {
        ControlStyle.Area title = styledArea("title");
        return title != null
                ? TextControl.resolveAreaRect(x, y, width, height, title)
                : new Rect(x, y, width, titleBarHeight);
    }

    private ResizeGrip hitTestResize(int mx, int my) {
        int x0 = x;
        int y0 = y;
        int x1 = x + width;
        int y1 = y + height;

        boolean onN = my >= y0 && my < y0 + EDGE;
        boolean onS = my > y1 - EDGE && my <= y1;
        boolean onW = mx >= x0 && mx < x0 + EDGE;
        boolean onE = mx > x1 - EDGE && mx <= x1;

        if (onN && mx < x0 + GRIP) return ResizeGrip.NW;
        if (onN && mx > x1 - GRIP) return ResizeGrip.NE;
        if (onS && mx < x0 + GRIP) return ResizeGrip.SW;
        if (onS && mx > x1 - GRIP) return ResizeGrip.SE;
        if (onN) return ResizeGrip.N;
        if (onS) return ResizeGrip.S;
        if (onW) return ResizeGrip.W;
        if (onE) return ResizeGrip.E;
        return ResizeGrip.NONE;
    }

    private static long cursorForGrip(ResizeGrip g) {
        switch (g) {
            case N: case S: return CursorMgr.resizeV();
            case E: case W: return CursorMgr.resizeH();
            case NW: case SE: return CursorMgr.curNWSE();
            case NE: case SW: return CursorMgr.curNESW();
            default: return CursorMgr.arrow();
        }
    }

    private boolean startDragOrResize(int mx, int my) {
        ResizeGrip g = hitTestResize(mx, my);
        if (g != ResizeGrip.NONE) {
            activeResize = g;
            resizeSnapX = x; resizeSnapY = y;
            resizeSnapW = width; resizeSnapH = height;
            return true;
        }
        if (titleDragZone(mx, my)) {
            dragging = true;
            dragGrabDx = mx - x;
            dragGrabDy = my - y;
            return true;
        }
        return false;
    }

    private void stopDragOrResize() {
        dragging = false;
        activeResize = ResizeGrip.NONE;
    }

    private void updateHoverCursor(long window, int mx, int my) {
        ResizeGrip g = hitTestResize(mx, my);
        if (g != ResizeGrip.NONE) {
            CursorMgr.set(window, cursorForGrip(g));
        } else if (contains(mx, my)) {
            CursorMgr.set(window, controlCursorAt(mx, my));
        } else {
            CursorMgr.setDefault(window);
        }
    }

    private long controlCursorAt(int mx, int my) {
        Rect contentRect = getContentRect();
        if (controls.isEmpty() || contentRect.isEmpty()) {
            return 0;
        }
        int cx = mx - contentRect.x(), cy = my - contentRect.y();
        for (Control c : controls) {
            if (!c.visible) {
                continue;
            }
            long cc = c.cursorAt(cx, cy);
            if (cc != 0) {
                return cc;
            }
        }
        return 0;
    }

    private int hostControlCursorAt(int mx, int my) {
        Rect contentRect = getContentRect();
        if (controls.isEmpty() || contentRect.isEmpty()) {
            return HOST_CURSOR_DEFAULT;
        }
        int cx = mx - contentRect.x(), cy = my - contentRect.y();
        for (Control c : controls) {
            if (!c.visible) {
                continue;
            }
            int cursor = c.hostCursorAt(cx, cy);
            if (cursor != HOST_CURSOR_DEFAULT) {
                return cursor;
            }
        }
        return HOST_CURSOR_DEFAULT;
    }

    private void applyResize(int mx, int my, int viewW, int viewH) {
        int sx = resizeSnapX;
        int sy = resizeSnapY;
        int sw = resizeSnapW;
        int sh = resizeSnapH;
        int bot = sy + sh;
        int right = sx + sw;

        switch (activeResize) {
            case SE: x = sx; y = sy; width = Math.max(MIN_W, mx - sx);   height = Math.max(MIN_H, my - sy); break;
            case NE: x = sx; y = my; width = Math.max(MIN_W, mx - sx);   height = Math.max(MIN_H, bot - y); break;
            case NW: x = mx; y = my; width = Math.max(MIN_W, right - x); height = Math.max(MIN_H, bot - y); break;
            case SW: x = mx; y = sy; width = Math.max(MIN_W, right - x); height = Math.max(MIN_H, my - sy); break;
            case E:  x = sx; width  = Math.max(MIN_W, mx - sx);    break;
            case W:  x = mx; width  = Math.max(MIN_W, right - mx); break;
            case S:  y = sy; height = Math.max(MIN_H, my - sy);    break;
            case N:  y = my; height = Math.max(MIN_H, bot - my);   break;
            default: break;
        }
        clampResizeInView(viewW, viewH);
    }

    private void dispatchMouseButtonToControls(int action, int mx, int my) {
        Rect contentRect = getContentRect();
        if (controls.isEmpty() || contentRect.isEmpty()) {
            return;
        }

        int rx = mx - contentRect.x();
        int ry = my - contentRect.y();
        if (action == GLFW.GLFW_PRESS) {
            for (Control c : controls) {
                if (c.visible && c.isActiveAt(rx, ry)) {
                    c.handleMouseButton(action, rx, ry);
                    break;
                }
            }
        } else {
            for (Control c : controls) {
                if (c.visible) {
                    c.handleMouseButton(action, rx, ry);
                }
            }
        }
    }

    /**
     * @return true if this press starts title drag or resize (caller may skip other click actions).
     * @param mx,my logical coords (framebuffer px / {@link TestApp#uiScale})
     */
    public boolean handleMouseButton(long window, int button, int action, int mx, int my) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        if (action == GLFW.GLFW_PRESS) {
            if (startDragOrResize(mx, my)) {
                return true;
            }
        }
        if (action == GLFW.GLFW_RELEASE) {
            stopDragOrResize();
            updateHoverCursor(window, mx, my);
        }
        dispatchMouseButtonToControls(action, mx, my);
        return false;
    }

    /**
     * Host-provided mouse button event for embedded renderers that cannot safely
     * call GLFW (for example Project Zomboid on macOS). Coordinates are in the
     * same logical viewport space as {@link #render(int)}.
     */
    public void handleHostMouseButton(int action, int mx, int my) {
        if (action == GLFW.GLFW_PRESS) {
            if (startDragOrResize(mx, my)) {
                return;
            }
        }
        if (action == GLFW.GLFW_RELEASE) {
            stopDragOrResize();
        }

        dispatchMouseButtonToControls(action, mx, my);
    }

    /**
     * Host-provided cursor update for embedded renderers that cannot safely call
     * GLFW from WUI. Returns one of {@code HOST_CURSOR_*}.
     */
    public int handleHostCursorPos(int mx, int my, int viewW, int viewH, boolean leftDown) {
        if (activeResize != ResizeGrip.NONE && leftDown) {
            applyResize(mx, my, viewW, viewH);
            return hostCursorForGrip(activeResize);
        }
        if (dragging && leftDown) {
            x = mx - dragGrabDx;
            y = my - dragGrabDy;
            clampPositionInView(viewW, viewH);
            return HOST_CURSOR_HAND;
        }

        ResizeGrip g = hitTestResize(mx, my);
        if (g != ResizeGrip.NONE) {
            return hostCursorForGrip(g);
        }
        if (titleDragZone(mx, my)) {
            return HOST_CURSOR_ARROW;
        }
        if (contains(mx, my)) {
            int cursor = hostControlCursorAt(mx, my);
            if (cursor != HOST_CURSOR_DEFAULT) {
                return cursor;
            }
            return HOST_CURSOR_ARROW;
        }
        return HOST_CURSOR_DEFAULT;
    }

    private static int hostCursorForGrip(ResizeGrip g) {
        switch (g) {
            case N: case S: return HOST_CURSOR_RESIZE_V;
            case E: case W: return HOST_CURSOR_RESIZE_H;
            case NW: case SE: return HOST_CURSOR_RESIZE_NWSE;
            case NE: case SW: return HOST_CURSOR_RESIZE_NESW;
            default: return HOST_CURSOR_ARROW;
        }
    }

    /** @param viewW,viewH logical viewport size (matches ortho after UI scale). */
    public void handleCursorPos(long window, int mx, int my, int viewW, int viewH) {
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (activeResize != ResizeGrip.NONE && leftDown) {
            applyResize(mx, my, viewW, viewH);
            CursorMgr.set(window, cursorForGrip(activeResize));
            return;
        }
        if (dragging && leftDown) {
            x = mx - dragGrabDx;
            y = my - dragGrabDy;
            clampPositionInView(viewW, viewH);
            CursorMgr.set(window, 0);
            return;
        }
        updateHoverCursor(window, mx, my);
    }

    /** Move only: keep window on-screen without changing {@link #width} / {@link #height}. */
    private void clampPositionInView(int viewW, int viewH) {
        if (x + width > viewW) x = viewW - width;
        if (y + height > viewH) y = viewH - height;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
    }

    /** After resize: cap size to the desktop and enforce {@link #MIN_W}×{@link #MIN_H}, then position. */
    private void clampResizeInView(int viewW, int viewH) {
        x      = Math.max(0, x);
        y      = Math.max(0, y);
        width  = Math.min(width, Math.max(0, viewW - x));
        width  = Math.max(MIN_W, width);
        height = Math.min(height, Math.max(0, viewH - y));
        height = Math.max(MIN_H, height);
        x      = Math.max(0, Math.min(x, viewW - width));
        y      = Math.max(0, Math.min(y, viewH - height));
    }

    public Rect getContentRect() {
        if (_deco.isLoaded()) {
            return _deco.contentRect(x, y, width, height);
        } else {
            return new Rect(x, y + titleBarHeight, width, height - titleBarHeight);
        }
    }

    protected void renderControlsClipped(int scale) {
        Rect contentRect = getContentRect();
        if (controls.isEmpty() || contentRect.isEmpty()) {
            return;
        }

        IntBuffer vp = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
        int fbH = vp.get(3);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            contentRect.x() * scale,
            fbH - (contentRect.y() + contentRect.h()) * scale,
            contentRect.w() * scale,
            contentRect.h() * scale
        );
        for (Control c : controls) {
            if (c.visible) c.render(contentRect.x(), contentRect.y());
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * Render using an explicit logical-to-framebuffer pixel {@code scale}.
     * Pass 1 for a 1:1 mapping; pass {@link Utils#detectScale} for HiDPI-aware rendering.
     */
    public void render(int scale) {
        TextControl.setClipScale(scale);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        _deco.render(x, y, width, height, bgColor);

        Font f = font();
        withTexture(f.fontTex, () -> {
            drawWindowText(f, title, styledArea("title"));
            drawWindowText(f, status, styledArea("status"));
        });

        // Render child controls clipped to the content rect.
        renderControlsClipped(scale);
    }

    private void drawWindowText(Font f, String s, ControlStyle.Area area) {
        if (s == null || s.isEmpty() || area == null) {
            return;
        }
        glColor(TextControl.styledTextColor(area, Color.BLACK));
        TextControl.drawAlignedString(f, x, y, width, height, area, s);
    }

    private static ControlStyle.Area styledArea(String areaName) {
        ControlStyle.Control control = ControlStyle.control("window");
        return control != null && control.areas != null ? control.areas.get(areaName) : null;
    }
}
