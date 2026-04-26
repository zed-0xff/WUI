package me.zed_0xff.WUI;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * Drives a {@link Window} from an external render loop with no GLFW event loop of its own.
 *
 * <p><b>Thread model</b>: create on any thread; call {@link #run()} each frame <em>from the
 * render thread</em>; block the caller with {@link #await()} until the dialog is dismissed.
 *
 * <p>The {@link Supplier} is invoked on the <em>first</em> {@link #run()} call so all
 * GL/GLFW class-loading and cursor setup happens safely on the render thread.
 *
 * <pre>{@code
 *   Session[] ref = { null };
 *   ref[0] = new Session(win, () ->
 *       new Window(0, 0, 400, 200, "Title")
 *           .addControl(w -> new Button(w, 10, 10, 100, 30, "OK") {
 *               protected void onClick() { ref[0].dismiss(); }
 *           }));
 *   renderThread.setFrameTask(ref[0]);
 *   ref[0].await();
 *   renderThread.setFrameTask(null);
 * }</pre>
 */
public final class Session implements Runnable {

    private final long glfwWin;
    private final Supplier<Window> factory;
    private final CountDownLatch latch = new CountDownLatch(1);

    private Window window;
    private int scale = 0; // 0 means auto-detect
    private boolean prevLeft;
    private float dimAlpha = 0.55f;
    private boolean autoCenter = true;

    public Session(long glfwWin, Supplier<Window> factory) {
        this.glfwWin = glfwWin;
        this.factory = factory;
    }

    /**
     * Alpha of the full-screen dim quad drawn behind the window (default 0.55).
     * Set to 0 for a standalone app where there is no underlying scene to obscure.
     */
    public Session setDimAlpha(float a) { dimAlpha = a; return this; }

    /**
     * Whether to center the window in the viewport on first frame (default true).
     * Set to false when the factory positions the window explicitly.
     */
    public Session setAutoCenter(boolean b) { autoCenter = b; return this; }

    /**
     * Set the UI scale (default 1).
     * Set to 0 to auto-detect the scale based on the framebuffer size.
     */
    public Session setScale(int s) { scale = s; return this; }

    // -------------------------------------------------------------------------
    // Called from any thread
    // -------------------------------------------------------------------------

    /** Unblock {@link #await()}. Idempotent. */
    public void dismiss() {
        latch.countDown();
    }

    public boolean isDone() {
        return latch.getCount() == 0;
    }

    /**
     * Block until {@link #dismiss()} is called.
     * Restores the interrupted flag if interrupted while waiting.
     */
    public void await() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // Called from the render thread each frame
    // -------------------------------------------------------------------------

    @Override
    public void run() {
        if (isDone()) return;

        if (window == null) {
            if (scale < 1) {
                scale = Utils.detectScale(glfwWin);
            }
            CursorMgr.create();
            window = factory.get();
            // Auto-center unless the factory positioned the window explicitly.
            if (autoCenter) {
                int[] sz = fbSize();
                window.x = (sz[0] / scale - window.width)  / 2;
                window.y = (sz[1] / scale - window.height) / 2;
            }
        }

        int[] sz = fbSize();
        int vW = sz[0] / scale, vH = sz[1] / scale;

        renderOverlay(sz[0], sz[1], vW, vH);
        pollMouse(vW, vH);

        if (GLFW.glfwWindowShouldClose(glfwWin)) {
            dismiss();
        }
    }

    // -------------------------------------------------------------------------
    // Private
    // -------------------------------------------------------------------------

    private void renderOverlay(int fbW, int fbH, int vW, int vH) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, fbW, fbH);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, vW, vH, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        if (dimAlpha > 0) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(0, 0, 0, dimAlpha);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(0,  0 ); GL11.glVertex2f(vW,  0);
            GL11.glVertex2f(vW, vH); GL11.glVertex2f(0,  vH);
            GL11.glEnd();
        }

        window.render(scale);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private void pollMouse(int vW, int vH) {
        double[] cx = new double[1], cy = new double[1];
        GLFW.glfwGetCursorPos(glfwWin, cx, cy);
        double[] fb = Utils.cursorToFramebuffer(glfwWin, cx[0], cy[0]);
        int mx = (int)(fb[0] / scale), my = (int)(fb[1] / scale);

        boolean left = GLFW.glfwGetMouseButton(glfwWin, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (left != prevLeft) {
            window.handleMouseButton(glfwWin, GLFW.GLFW_MOUSE_BUTTON_LEFT,
                    left ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE, mx, my);
        }
        // Always forward cursor position: updates hover cursors even when no button is held.
        window.handleCursorPos(glfwWin, mx, my, vW, vH);
        prevLeft = left;
    }

    private int[] fbSize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(glfwWin, w, h);
            return new int[]{ w.get(0), h.get(0) };
        }
    }
}
