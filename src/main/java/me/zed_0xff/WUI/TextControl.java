package me.zed_0xff.WUI;

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
}
