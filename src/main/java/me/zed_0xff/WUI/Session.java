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
    // Blocking render loop (caller IS the render thread)
    // -------------------------------------------------------------------------

    /**
     * Drive the session synchronously on the calling thread until dismissed.
     *
     * <p>Use this when the caller <em>is</em> the render thread and there is no
     * external frame loop to piggy-back on (e.g. called during game loading before
     * {@code UIManager.update()} starts).  The method calls {@link #run()} and
     * swaps buffers each iteration.
     *
     * <p>Must be called from the thread that owns the OpenGL context (the render
     * thread, e.g. {@code Java: MainThread} in Project Zomboid).  Do <em>not</em>
     * call {@code glfwPollEvents()} here — on macOS that must stay on the Cocoa
     * main thread (Thread 0) which already runs PZ's GLFW event pump.
     * {@link #pollMouse()} reads cached GLFW state so it works correctly without
     * an additional poll.
     */
    public void runLoop() {
        while (!isDone()) {
            run();
            GLFW.glfwSwapBuffers(glfwWin);
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                dismiss();
            }
        }
    }

    public static void init() {
        CursorMgr.init();
    }

    // -------------------------------------------------------------------------
    // Called from the render thread each frame
    // -------------------------------------------------------------------------

    @Override
    public void run() {
        if (isDone()) return;

        prepareGlfwThreadCheck();
        int[] sz = fbSize();
        ensureWindow(sz[0], sz[1], scale < 1 ? Utils.detectScale(glfwWin) : scale, true);
        int vW = sz[0] / scale, vH = sz[1] / scale;

        renderOverlay(sz[0], sz[1], vW, vH);
        pollMouse(vW, vH);

        if (GLFW.glfwWindowShouldClose(glfwWin)) {
            dismiss();
        }
    }

    /**
     * Render-only variant for hosts where touching {@code org.lwjgl.glfw.GLFW}
     * from the render thread is unsafe (Project Zomboid on macOS).
     *
     * <p>The caller supplies framebuffer dimensions and UI scale. This method
     * intentionally skips cursor creation, mouse polling, and window-close
     * polling.
     */
    public void runRenderOnly(int fbW, int fbH, int uiScale) {
        if (isDone()) return;

        ensureWindow(fbW, fbH, Math.max(1, uiScale), false);
        renderOverlay(fbW, fbH, fbW / scale, fbH / scale);
    }

    /**
     * Render-only variant with host-provided mouse input.
     *
     * <p>{@code mouseX/mouseY} are logical viewport coordinates, not framebuffer
     * pixels. The host is responsible for polling input without touching
     * {@code org.lwjgl.glfw.GLFW}.
     */
    public int runRenderOnlyWithHostInput(int fbW, int fbH, int uiScale,
            int mouseX, int mouseY, boolean leftDown) {
        if (isDone()) {
            return Window.HOST_CURSOR_DEFAULT;
        }

        ensureWindow(fbW, fbH, Math.max(1, uiScale), false);
        int vW = fbW / scale;
        int vH = fbH / scale;

        int action = leftDown && !prevLeft ? GLFW.GLFW_PRESS
                : (!leftDown && prevLeft ? GLFW.GLFW_RELEASE : -1);
        if (action != -1) {
            window.handleHostMouseButton(action, mouseX, mouseY);
        }
        int cursor = window.handleHostCursorPos(mouseX, mouseY, vW, vH, leftDown);
        prevLeft = leftDown;
        renderOverlay(fbW, fbH, vW, vH);
        return cursor;
    }

    // -------------------------------------------------------------------------
    // Private
    // -------------------------------------------------------------------------

    private void ensureWindow(int fbW, int fbH, int uiScale, boolean initCursors) {
        if (scale < 1) {
            scale = Math.max(1, uiScale);
        }
        if (window != null) {
            return;
        }

        if (initCursors) {
            CursorMgr.init();
        }
        window = factory.get();
        if (autoCenter) {
            window.setPosition(
                    (fbW / scale - window.width)  / 2,
                    (fbH / scale - window.height) / 2);
        }
    }

    private static void prepareGlfwThreadCheck() {
        // LWJGL3's GLFW static initializer checks whether the calling thread is
        // the macOS main thread (Thread 0) and calls jni_FatalError if not.
        // PZ uses lwjglx (an LWJGL2 shim) which bypasses org.lwjgl.glfw.GLFW, so
        // this class may be the first to touch GLFW from the GL/game thread.
        //
        // We cannot use Configuration.GLFW_CHECK_THREAD0.set(false) here because
        // accessing org.lwjgl.system.Configuration itself may trigger liblwjgl.dylib
        // class initialization which performs the same thread check. Setting the
        // system property is safe and is read lazily by Configuration.get().
        System.setProperty("org.lwjgl.glfw.checkThread0", "false");
    }

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
