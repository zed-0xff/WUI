package me.zed_0xff.WUI;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

public final class HelloWorld {
    static final int WIN_W = 640;
    static final int WIN_H = 480;

    private static final int[] SCALES = {1, 2, 3};
    private static volatile int scaleIdx = 1;

    /** UI scale: ortho and mouse are in logical px; one logical px maps to this many framebuffer px. */
    static int uiScale() {
        return SCALES[scaleIdx % SCALES.length];
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    /** Init GLFW, create window, set up GL context and one-time GL state. */
    private static long initGlfw() {
        if (!GLFW.glfwInit()) throw new IllegalStateException("glfwInit failed");
        long win = GLFW.glfwCreateWindow(WIN_W, WIN_H, "Desktop", 0, 0);
        if (win == 0) throw new RuntimeException("glfwCreateWindow failed");
        GLFW.glfwMakeContextCurrent(win);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();
        initGlState();
        return win;
    }

    /** Init WUI: load cursors, build the window and its controls. */
    private static Window buildUI() {
        CursorMgr.create();
        return new Window(80, 48, 420, 260, "Window")
                .addControl(w -> new Button(  w, 10, 10, 100, 20, "OK"))
                .addControl(w -> new CheckBox(w, 10, 40, 100, 20, "test"))
                .addRadioGroup(g -> {
                    g.button(10, 70, 100, 20, "R1");
                    g.button(50, 70, 100, 20, "R2");
                })
                .addRadioGroup(g -> {
                    g.button(10, 90, 100, 20, "R3");
                    g.button(50, 90, 100, 20, "R4");
                });
    }

    /** Wire GLFW input/display callbacks to the WUI window. */
    private static void registerCallbacks(long glWindow, Window window) {
        GLFW.glfwSetMouseButtonCallback(glWindow, (win, button, action, mods) -> {
            double[] cx = new double[1], cy = new double[1];
            GLFW.glfwGetCursorPos(win, cx, cy);
            double[] f = Utils.cursorToFramebuffer(win, cx[0], cy[0]);
            int scale = uiScale();
            window.handleMouseButton(win, button, action, (int)(f[0] / scale), (int)(f[1] / scale));
        });

        GLFW.glfwSetCursorPosCallback(glWindow, (win, xpos, ypos) -> {
            double[] f = Utils.cursorToFramebuffer(win, xpos, ypos);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer fbw = stack.mallocInt(1), fbh = stack.mallocInt(1);
                GLFW.glfwGetFramebufferSize(win, fbw, fbh);
                int scale = uiScale();
                window.handleCursorPos(win, (int)(f[0] / scale), (int)(f[1] / scale),
                        fbw.get(0) / scale, fbh.get(0) / scale);
            }
        });

        GLFW.glfwSetFramebufferSizeCallback(glWindow, (win, w, h) -> applyFrameProjection(win));

        GLFW.glfwSetWindowRefreshCallback(glWindow, win -> {
            renderFrame(win, window);
            GLFW.glfwSwapBuffers(win);
        });
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        long glWindow = initGlfw();
        Window window = buildUI();
        registerCallbacks(glWindow, window);

        GLFW.glfwSetWindowTitle(glWindow, "Desktop — " + uiScale() + "x");

        while (!GLFW.glfwWindowShouldClose(glWindow)) {
            renderFrame(glWindow, window);
            GLFW.glfwSwapBuffers(glWindow);
            GLFW.glfwPollEvents();
        }

        GL11.glDeleteTextures(Element.font.fontTex);
        GLFW.glfwSetCursor(glWindow, 0);
        CursorMgr.destroy();
        GLFW.glfwDestroyWindow(glWindow);
        GLFW.glfwTerminate();
    }

    // -------------------------------------------------------------------------
    // Rendering helpers
    // -------------------------------------------------------------------------

    static void renderFrame(long glWindow, Window window) {
        applyFrameProjection(glWindow);
        GL11.glClearColor(Color.GRAY.getRf(), Color.GRAY.getGf(), Color.GRAY.getBf(), 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        window.render();
    }

    /** One-time GL state; projection is updated per-frame to handle HiDPI. */
    private static void initGlState() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Viewport + ortho in framebuffer pixels (fixes 0.5x look on Retina until resize). */
    static void applyFrameProjection(long glWindow) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer fbw = stack.mallocInt(1), fbh = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(glWindow, fbw, fbh);
            int w = Math.max(1, fbw.get(0));
            int h = Math.max(1, fbh.get(0));
            int scale = uiScale();
            GL11.glViewport(0, 0, w, h);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0, w / scale, h / scale, 0, -1, 1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
        }
    }
}
