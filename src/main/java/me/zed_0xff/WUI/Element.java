package me.zed_0xff.WUI;

import org.lwjgl.opengl.GL11;

public abstract class Element {
    public int x, y, width, height;

    static Font font = new Font();

    public Element(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
    }

    static void glColor(Color c) { c.applyGl(); }

    static void fillRect(int x0, int y0, int w, int h, Color color) {
        glColor(color);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x0,     y0);
        GL11.glVertex2f(x0 + w, y0);
        GL11.glVertex2f(x0 + w, y0 + h);
        GL11.glVertex2f(x0,     y0 + h);
        GL11.glEnd();
    }

    static void fillRect(Rect r, Color color) {
        if (r != null) fillRect(r.x(), r.y(), r.w(), r.h(), color);
    }

    static void outlineRect(int x0, int y0, int w, int h, int lineWidth, Color color) {
        glColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x0,     y0);
        GL11.glVertex2f(x0 + w, y0);
        GL11.glVertex2f(x0 + w, y0 + h);
        GL11.glVertex2f(x0,     y0 + h);
        GL11.glEnd();
    }

    /**
     * Emit one textured quad's 4 vertices inside an active {@code GL_QUADS} block.
     * Callers are responsible for the surrounding {@code glBegin}/{@code glEnd}.
     */
    static void glTexQuad(float x0, float y0, float x1, float y1,
                          float u0, float v0, float u1, float v1) {
        GL11.glTexCoord2f(u0, v0); GL11.glVertex2f(x0, y0);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex2f(x1, y0);
        GL11.glTexCoord2f(u1, v1); GL11.glVertex2f(x1, y1);
        GL11.glTexCoord2f(u0, v1); GL11.glVertex2f(x0, y1);
    }

    /** Draw a textured rectangle with explicit UV coordinates. */
    static void drawTexRect(int sx, int sy, int sw, int sh, float u0, float v0, float u1, float v1) {
        GL11.glBegin(GL11.GL_QUADS);
        glTexQuad(sx, sy, sx + sw, sy + sh, u0, v0, u1, v1);
        GL11.glEnd();
    }

    /** Draw a tile from an atlas, stretching it to ({@code sw} × {@code sh}). */
    static void drawTexRect(int sx, int sy, int sw, int sh, Atlas.TileJson t, int aw, int ah) {
        drawTexRect(sx, sy, sw, sh,
            t.x / (float) aw,         t.y / (float) ah,
            (t.x + t.w) / (float) aw, (t.y + t.h) / (float) ah);
    }

    static void withTexture(int tex, Runnable r) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        r.run();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
}
