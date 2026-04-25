package me.zed_0xff.WUI;

class Label extends Control {
    String text;
    Color textColor = Color.BLACK;

    public Label(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h);
        this.text = text;
    }

    @Override
    public void render(int fontTex, int originX, int originY) {
        if (text == null || text.isEmpty()) return;
        int tx = originX + x;
        int ty = originY + y;
        withTexture(fontTex, () -> {
            glColor(textColor);
            font.drawText(tx, ty, text);
        });
    }
}
