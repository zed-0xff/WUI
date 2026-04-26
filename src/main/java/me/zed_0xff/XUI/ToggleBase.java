package me.zed_0xff.XUI;

public abstract class ToggleBase extends ButtonBase {
    boolean checked;

    public ToggleBase(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h, text);
    }

    @Override
    protected void onClick() { checked = !checked; }

    @Override
    protected Iterable<ControlStyle.State> visualStates() {
        return ControlStyle.visualStates(styleName(), checked, pressed);
    }

    @Override
    protected boolean isActiveAt(int mx, int my) {
        if (mx < x || my < y || my >= y + height) return false;
        int iconW = labelX();
        int textW = (text != null && !text.isEmpty()) ? font().measureTextAdvancePx(text) : 0;
        return mx < x + iconW + textW;
    }

    private int labelX() {
        ControlStyle.Area label = getArea("label");
        return label != null && label.hasX() ? label.x(0) : height + 2;
    }

}
