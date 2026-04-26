package me.zed_0xff.WUI;

import com.google.gson.Gson;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

public class Utils {
    private static int uiScale = 1;

    public static int uiScale() {
        return uiScale;
    }

    static void setUiScale(int scale) {
        uiScale = Math.max(1, scale);
    }

    /** Parse a classpath JSON resource into {@code cls}. Returns {@code null} on any error. */
    static <T> T readJson(String resourcePath, Gson gson, Class<T> cls) {
        try (InputStream is = Utils.class.getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            return gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), cls);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Framebuffer-to-window pixel ratio (e.g. 2 on Retina/HiDPI, 1 otherwise).
     * Must be called from the render thread.
     */
    public static int detectScale(long win) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ww = stack.mallocInt(1), wh = stack.mallocInt(1);
            IntBuffer fw = stack.mallocInt(1), fh = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(win, ww, wh);
            GLFW.glfwGetFramebufferSize(win, fw, fh);
            int s = ww.get(0) > 0 ? fw.get(0) / ww.get(0) : 1;
            return Math.max(1, s);
        }
    }

    /** Map glfwGetCursorPos (window coords) → framebuffer pixel coords used by glOrtho. */
    public static double[] cursorToFramebuffer(long win, double cxWin, double cyWin) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer winW = stack.mallocInt(1), winH = stack.mallocInt(1);
            IntBuffer fbW  = stack.mallocInt(1), fbH  = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(win, winW, winH);
            GLFW.glfwGetFramebufferSize(win, fbW, fbH);
            int ww = winW.get(0), wh = winH.get(0);
            if (ww <= 0 || wh <= 0) return new double[]{ cxWin, cyWin };
            return new double[]{ cxWin * fbW.get(0) / ww, cyWin * fbH.get(0) / wh };
        }
    }

    static long packPair(int first, int second) {
        return ((long) first << 32) | (second & 0xffffffffL);
    }

    static void putArgbAsRgba(ByteBuffer buf, int argb) {
        buf.put((byte) ((argb >> 16) & 0xff));
        buf.put((byte) ((argb >> 8)  & 0xff));
        buf.put((byte) (argb         & 0xff));
        buf.put((byte) ((argb >> 24) & 0xff));
    }

    static int uploadRgbaTexture2d(BufferedImage img) {
        int tw = img.getWidth();
        int th = img.getHeight();
        ByteBuffer pixels = BufferUtils.createByteBuffer(tw * th * 4);
        for (int y = 0; y < th; y++)
            for (int x = 0; x < tw; x++)
                putArgbAsRgba(pixels, img.getRGB(x, y));
        pixels.flip();

        int tex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            tw,
            th,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            pixels
        );

        return tex;
    }
}
