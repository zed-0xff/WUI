package me.zed_0xff.WUI;

/** A {@link Control} that carries a text label and text color. */
abstract class TextControl extends Control {
    String text;
    Color textColor = Color.BLACK;

    public TextControl(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h);
        this.text = text;
    }
}
