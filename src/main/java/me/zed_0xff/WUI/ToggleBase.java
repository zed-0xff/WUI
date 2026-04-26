package me.zed_0xff.WUI;

import org.lwjgl.opengl.GL11;

public abstract class ToggleBase extends ButtonBase {
    boolean checked;

    public ToggleBase(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h, text);
    }

    protected abstract Atlas getAtlas();

    /** Draw the icon when the atlas is not loaded. */
    protected abstract void renderFallback(int bx, int by);

    @Override
    protected void onClick() { checked = !checked; }

    @Override
    protected boolean isActiveAt(int mx, int my) {
        if (mx < x || my < y || my >= y + height) return false;
        int iconW = getAtlas().getMetaInt("textX", height + 2);
        int textW = (text != null && !text.isEmpty()) ? font().measureTextAdvancePx(text) : 0;
        return mx < x + iconW + textW;
    }

    @Override
    public void render(int originX, int originY) {
        Atlas a = getAtlas();
        int bx = originX + x, by = originY + y;

        if (a.isLoaded()) {
            if (a.texId == 0) a.texId = a.uploadTexture();
            String tileName = checked ? (pressed ? "checkedClicked" : "checked")
                                      : (pressed ? "clicked"        : "default");
            Atlas.TileJson t = a.tiles.get(tileName);
            if (t != null && a.texId != 0) {
                withTexture(a.texId, () -> {
                    GL11.glColor3f(1f, 1f, 1f);
                    drawTexRect(bx, by, t.w, t.h, t, a.w, a.h);
                });
            }
        } else {
            renderFallback(bx, by);
        }

        if (text != null && !text.isEmpty()) {
            int textX = a.getMetaInt("textX", height + 2);
            int textY = a.getMetaInt("textY", 0);
            Font f = font();
            withTexture(f.fontTex, () -> {
                glColor(textColor);
                f.drawText(bx + textX, by + textY, text);
            });
        }
    }
}
