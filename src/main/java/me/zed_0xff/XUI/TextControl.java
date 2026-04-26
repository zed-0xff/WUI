package me.zed_0xff.XUI;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** A {@link Control} that carries a text label and text color. */
public abstract class TextControl extends Control {
    String text;
    Color textColor = Color.BLACK;

    public TextControl(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h);
        this.text = text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    protected String styleName() {
        return "label";
    }

    protected void drawAlignedText(Font f, int boxX, int boxY, int boxW, int boxH, ControlStyle.Area area) {
        drawAlignedString(f, boxX, boxY, boxW, boxH, area, text);
    }

    static void drawAlignedString(Font f, int boxX, int boxY, int boxW, int boxH, ControlStyle.Area area, String s) {
        if (s == null || s.isEmpty()) {
            return;
        }

        Rect box = resolveAreaRect(boxX, boxY, boxW, boxH, area);
        int labelX = box.x();
        int labelY = box.y();
        int labelW = box.w();
        int labelH = box.h();
        if (labelW <= 0 || labelH <= 0) {
            return;
        }
        int textW = f.measureTextAdvancePx(s);

        String align = area != null && area.text != null && area.text.align != null ? area.text.align : "left";
        String valign = area != null && area.text != null && area.text.valign != null ? area.text.valign : "top";

        int tx = labelX;
        if ("center".equals(align)) {
            tx = labelX + (labelW - textW) / 2;
        } else if ("right".equals(align)) {
            tx = labelX + labelW - textW;
        }

        int ty = labelY;
        if ("center".equals(valign)) {
            ty = labelY + (labelH - f.face.lineHeight) / 2;
        } else if ("bottom".equals(valign)) {
            ty = labelY + labelH - f.face.lineHeight;
        }

        IntBuffer vp = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
        int fbH = vp.get(3);
        int clipScale = Utils.uiScale();
        int sx = labelX * clipScale;
        int sy = fbH - (labelY + labelH) * clipScale;
        int sw = labelW * clipScale;
        int sh = labelH * clipScale;

        boolean hadScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        IntBuffer previousScissor = BufferUtils.createIntBuffer(4);
        if (hadScissor) {
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, previousScissor);
            int px = previousScissor.get(0);
            int py = previousScissor.get(1);
            int pw = previousScissor.get(2);
            int ph = previousScissor.get(3);
            int ix = Math.max(sx, px);
            int iy = Math.max(sy, py);
            int iw = Math.min(sx + sw, px + pw) - ix;
            int ih = Math.min(sy + sh, py + ph) - iy;
            if (iw <= 0 || ih <= 0) {
                return;
            }
            sx = ix; sy = iy; sw = iw; sh = ih;
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
        try {
            f.drawText(tx, ty, s);
        } finally {
            if (hadScissor) {
                GL11.glScissor(
                    previousScissor.get(0),
                    previousScissor.get(1),
                    previousScissor.get(2),
                    previousScissor.get(3)
                );
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }
    }

    static Rect resolveAreaRect(int boxX, int boxY, int boxW, int boxH, ControlStyle.Area area) {
        int labelX = boxX;
        int labelY = boxY;
        int labelW = boxW;
        int labelH = boxH;
        if (area != null) {
            int left = area.left(0);
            int top = area.top(0);
            int right = area.right(0);
            int bottom = area.bottom(0);

            labelW = area.w(Math.max(0, boxW - left - right));
            labelH = area.h(Math.max(0, boxH - top - bottom));

            if (area.hasX()) {
                labelX = boxX + area.x(0);
            } else if (area.hasLeft()) {
                labelX = boxX + left;
            } else if (area.hasRight()) {
                labelX = boxX + boxW - right - labelW;
            }

            if (area.hasY()) {
                labelY = boxY + area.y(0);
            } else if (area.hasTop()) {
                labelY = boxY + top;
            } else if (area.hasBottom()) {
                labelY = boxY + boxH - bottom - labelH;
            }
        }
        return new Rect(labelX, labelY, labelW, labelH);
    }

    static Color styledTextColor(ControlStyle.Area area, Color fallback) {
        String color = area != null && area.text != null ? area.text.color : null;
        if (color == null || color.isEmpty()) {
            return fallback;
        }
        try {
            if (color.charAt(0) == '#') {
                return new Color(Integer.parseInt(color.substring(1), 16));
            }
            return new Color(Integer.parseInt(color, 16));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
