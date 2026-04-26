package me.zed_0xff.WUI;

import org.lwjgl.opengl.GL11;

public class CheckBox extends ToggleBase {
    public CheckBox(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h, text);
    }

    @Override protected String styleName() { return "checkbox"; }

    @Override
    protected void renderFallback(int bx, int by) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Element.outlineRect(bx, by, height, height, 1, Color.BLACK);
        if (checked) {
            Element.fillRect(bx + 3, by + 3, height - 6, height - 6, textColor);
        }
    }
}
