package me.zed_0xff.WUI;

import org.lwjgl.opengl.GL11;

public abstract class ToggleBase extends ButtonBase {
    boolean checked;

    public ToggleBase(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h, text);
    }

    protected abstract String styleName();

    /** Draw the icon when the atlas is not loaded. */
    protected abstract void renderFallback(int bx, int by);

    @Override
    protected void onClick() { checked = !checked; }

    @Override
    protected boolean isActiveAt(int mx, int my) {
        if (mx < x || my < y || my >= y + height) return false;
        int iconW = labelX();
        int textW = (text != null && !text.isEmpty()) ? font().measureTextAdvancePx(text) : 0;
        return mx < x + iconW + textW;
    }

    @Override
    public void render(int originX, int originY) {
        int bx = originX + x, by = originY + y;
        boolean rendered = false;

        for (ControlStyle.State state : ControlStyle.toggleStates(styleName(), checked, pressed)) {
            Atlas a = ControlStyle.atlasFor(state);
            if (a != null && a.isLoaded() && state != null && state.rect != null) {
                if (a.texId == 0) a.texId = a.uploadTexture();
                Atlas.TileJson t = tile(state.rect);
                if (t != null && a.texId != 0) {
                    rendered = true;
                    withTexture(a.texId, () -> {
                        GL11.glColor3f(1f, 1f, 1f);
                        drawTexRect(bx, by, t.w, t.h, t, a.w, a.h);
                    });
                }
            }
        }
        if (!rendered) {
            renderFallback(bx, by);
        }

        if (text != null && !text.isEmpty()) {
            int textX = labelX();
            int textY = labelY();
            Font f = font();
            withTexture(f.fontTex, () -> {
                glColor(textColor);
                f.drawText(bx + textX, by + textY, text);
            });
        }
    }

    private int labelX() {
        ControlStyle.Control control = ControlStyle.control(styleName());
        ControlStyle.Area label = control != null && control.areas != null ? control.areas.get("label") : null;
        return label != null && label.hasX() ? label.x(0) : height + 2;
    }

    private int labelY() {
        ControlStyle.Control control = ControlStyle.control(styleName());
        ControlStyle.Area label = control != null && control.areas != null ? control.areas.get("label") : null;
        return label != null && label.hasY() ? label.y(0) : 0;
    }

    private static Atlas.TileJson tile(ControlStyle.RectSpec rect) {
        Atlas.TileJson t = new Atlas.TileJson();
        t.x = rect.x; t.y = rect.y; t.w = rect.w; t.h = rect.h;
        return t;
    }
}
