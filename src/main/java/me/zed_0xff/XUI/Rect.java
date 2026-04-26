package me.zed_0xff.XUI;

/**
 * Simple integer rectangle in logical UI coordinates.
 */
public record Rect(int x, int y, int w, int h) {
    public boolean isEmpty() { return w <= 0 || h <= 0; }
    public boolean contains(int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}

