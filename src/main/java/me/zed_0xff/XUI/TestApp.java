package me.zed_0xff.XUI;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

/** Standalone test application for the XUI library. */
public final class TestApp {
    static int winW = 640, winH = 480;

    public static void main(String[] args) {
        int dlgW = 420, dlgH = 260, dlgX = 80, dlgY = 48;
        int scale = 0;
        boolean autoCenter = false;
        String theme = "win31";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    System.out.println("Usage: TestApp [--scale N] [--x N] [--y N] [--w N] [--h N]");
                    System.out.println("  -s, --scale N          UI scale (default: auto)");
                    System.out.println("  -x N                   dialog X position (default: centered)");
                    System.out.println("  -y N                   dialog Y position (default: centered)");
                    System.out.println("  -w, --width  N         dialog width  (default 420)");
                    System.out.println("  -h, --height N         dialog height (default 260)");
                    System.out.println("  -c, --center           center the dialog (default: false)");
                    System.out.println("  -t, --theme            theme name (default: " + theme + ")");
                    return;
                case "-s": case "--scale":             scale = Integer.parseInt(args[++i]); break;
                case "-x": case "--x":                  dlgX = Integer.parseInt(args[++i]); break;
                case "-y": case "--y":                  dlgY = Integer.parseInt(args[++i]); break;
                case "-w": case "--w": case "--width":  dlgW = Integer.parseInt(args[++i]); break;
                case "-h": case "--h": case "--height": dlgH = Integer.parseInt(args[++i]); break;
                case "-c": case "--center":             autoCenter = true; break;
                case "-t": case "--theme":              theme = args[++i]; break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(1);
            }
        }

        long win = initGlfw();
        final int dx = dlgX, dy = dlgY, dw = dlgW, dh = dlgH;

        Session[] ref = { null };
        ref[0] = new Session(win, () ->
            new Window(dx, dy, dw, dh, "Window")
                .addControl(w -> new Label(   w, 10,  10, 100, 20, "label"))
                .addControl(w -> new Button(  w, 10,  30, 100, 40, "OK") { @Override protected void onClick() { ref[0].dismiss(); } })
                .addControl(w -> new CheckBox(w, 120,  10, 100, 20, "test"))
                .addRadioGroup(g -> { g.button(120, 30, 100, 20, "R1"); g.button(160, 30, 100, 20, "R2"); })
                .addRadioGroup(g -> { g.button(120, 50, 100, 20, "R3"); g.button(160, 50, 100, 20, "R4"); })
                .setStatus("This is a test dialog. It should be centered and scaled according to the arguments.")
        )
        .setDimAlpha(0)
        .setTheme(theme)
        .setScale(scale)
        .setAutoCenter(autoCenter);

        while (!GLFW.glfwWindowShouldClose(win)) {
            GL11.glClearColor(Color.GRAY.getRf(), Color.GRAY.getGf(), Color.GRAY.getBf(), 1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            ref[0].run();
            GLFW.glfwSwapBuffers(win);
            GLFW.glfwPollEvents();
            if (ref[0].isDone()) break;
        }

        Element.disposeFont();
        CursorMgr.destroy();
        GLFW.glfwDestroyWindow(win);
        GLFW.glfwTerminate();
    }

    private static long initGlfw() {
        if (!GLFW.glfwInit()) throw new IllegalStateException("glfwInit failed");
        long win = GLFW.glfwCreateWindow(winW, winH, "XUI Test", 0, 0);
        if (win == 0) throw new RuntimeException("glfwCreateWindow failed");
        GLFW.glfwMakeContextCurrent(win);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        return win;
    }
}
