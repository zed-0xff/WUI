package me.zed_0xff.XUI;

public class Label extends TextControl {
    public Label(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h, text);
    }

    @Override
    public void render(int originX, int originY) {
        if (text == null || text.isEmpty()) return;
        int tx = originX + x, ty = originY + y;
        ControlStyle.Area label = getArea("label");
        renderAreaBorders(tx, ty, width, height);
        Font f = font();
        withTexture(f.fontTex, () -> {
            glColor(styledTextColor(label, textColor));
            drawAlignedText(f, tx, ty, width, height, label);
        });
    }
}
