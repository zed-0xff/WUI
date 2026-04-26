package me.zed_0xff.WUI;

public class Button extends ButtonBase {
    static final ElementDecor _normalDeco  = new ElementDecor("button");
    static final ElementDecor _pressedDeco = new ElementDecor("buttonDown");

    Color bgColor = Color.GRAY;

    public Button(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h, text);
    }

    @Override
    public void render(int originX, int originY) {
        int bx = originX + x, by = originY + y;
        ElementDecor deco = (pressed && _pressedDeco.isLoaded()) ? _pressedDeco : _normalDeco;
        deco.render(bx, by, width, height, bgColor);
        if (text != null && !text.isEmpty()) {
            Font f = font();
            withTexture(f.fontTex, () -> {
                glColor(textColor);
                f.drawTextCentered(bx, by + deco.textY, width, text);
            });
        }
    }
}
