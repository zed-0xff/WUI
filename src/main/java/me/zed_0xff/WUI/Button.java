package me.zed_0xff.WUI;

public class Button extends ButtonBase {
    public Button(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h, text);
    }

    @Override protected String styleName() { return "button"; }
}
