package me.zed_0xff.XUI;

public class CheckBox extends ToggleBase {
    public CheckBox(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h, text);
    }

    @Override protected String styleName() { return "checkbox"; }
}
