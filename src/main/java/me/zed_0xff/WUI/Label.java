package me.zed_0xff.WUI;

public class Label extends TextControl {
    public Label(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h, text);
    }

    @Override
    public void render(int originX, int originY) {
        if (text == null || text.isEmpty()) return;
        int tx = originX + x, ty = originY + y;
        withTexture(font.fontTex, () -> {
            glColor(textColor);
            font.drawText(tx, ty, text);
        });
    }
}
