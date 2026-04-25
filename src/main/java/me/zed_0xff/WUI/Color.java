package me.zed_0xff.WUI;

import org.lwjgl.opengl.GL11;

public record Color(int value) {
    public static final Color BLACK = new Color(0x000000);
    public static final Color GRAY  = new Color(0xc0c0c0);
    public static final Color NAVY  = new Color(0x0000b8);
    public static final Color WHITE = new Color(0xffffff);

    public float getRf() { return ((value >> 16) & 0xff) / 255.0f; }
    public float getGf() { return ((value >>  8) & 0xff) / 255.0f; }
    public float getBf() { return  (value        & 0xff) / 255.0f; }

    /** Apply this color as the current GL draw color. */
    public void applyGl() {
        GL11.glColor3ub((byte)((value >> 16) & 0xff), (byte)((value >> 8) & 0xff), (byte)(value & 0xff));
    }
}
