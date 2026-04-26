package me.zed_0xff.WUI;

import org.lwjgl.opengl.GL11;

/**
 * 9-slice decoration — "cap corners" layout: corners keep their own size,
 * side thickness comes from the middle slices.
 */
final class ElementDecor {
    private final String name;
    private int texture, atlasW, atlasH;

    private Atlas.TileJson topLeft, topCenter, topRight;
    private Atlas.TileJson middleLeft, middleCenter, middleRight;
    private Atlas.TileJson bottomLeft, bottomCenter, bottomRight;

    private int leftW, rightW, topH, bottomH;
    private int topLeftW, topLeftH, topRightW, topRightH;
    private int bottomLeftW, bottomLeftH, bottomRightW, bottomRightH;

    public ElementDecor(String name) {
        this(name, "default");
    }

    public ElementDecor(String name, String state) {
        this.name = name;
        loadFromStyle(name, state);
    }

    public boolean isLoaded() { return texture != 0; }

    public void dispose() {
        if (texture != 0) { GL11.glDeleteTextures(texture); texture = 0; }
    }

    public Rect contentRect(int x, int y, int w, int h) {
        return new Rect(x + leftW, y + topH, Math.max(0, w - leftW - rightW), Math.max(0, h - topH - bottomH));
    }

    /** Draw decor + fill content; fallback: fill + black outline. */
    public void render(int x, int y, int w, int h, Color fill) {
        if (isLoaded()) {
            renderInternal(x, y, w, h);
            Element.fillRect(contentRect(x, y, w, h), fill);
        } else {
            Element.fillRect(x, y, w, h, fill);
            Element.outlineRect(x, y, w, h, 1, Color.BLACK);
        }
    }

    void renderInternal(int x, int y, int w, int h) {
        if (texture == 0) return;
        int innerW  = Math.max(0, w - leftW - rightW);
        int innerH  = Math.max(0, h - topH - bottomH);
        int topMidW = Math.max(0, w - topLeftW - topRightW);
        int botMidW = Math.max(0, w - bottomLeftW - bottomRightW);

        Element.withTexture(texture, () -> {
            GL11.glColor3f(1f, 1f, 1f);
            blit(topLeft,      x,                    y,                topLeftW,    topLeftH);
            blit(topCenter,    x + topLeftW,         y,                topMidW,     topH);
            blit(topRight,     x + w - topRightW,    y,                topRightW,   topRightH);
            blit(middleLeft,   x,                    y + topH,         leftW,       innerH);
            blit(middleCenter, x + leftW,            y + topH,         innerW,      innerH);
            blit(middleRight,  x + w - rightW,       y + topH,         rightW,      innerH);
            blit(bottomLeft,   x,                    y + h - bottomLeftH, bottomLeftW, bottomLeftH);
            blit(bottomCenter, x + bottomLeftW,      y + h - bottomH,  botMidW,      bottomH);
            blit(bottomRight,  x + w - bottomRightW, y + h - bottomRightH, bottomRightW, bottomRightH);
        });
    }

    private void loadFromStyle(String name, String stateName) {
        ControlStyle.State state = ControlStyle.state(name, stateName);
        Atlas atlas = ControlStyle.atlasFor(state);
        if (state == null || state.patch == null || atlas == null || !atlas.isLoaded()) {
            warn("style load failed");
            return;
        }

        texture = atlas.uploadTexture();
        if (texture == 0) { warn("texture upload failed"); return; }
        atlasW = atlas.w; atlasH = atlas.h;

        ControlStyle.Patch p = state.patch;
        leftW = p.left;
        rightW = p.right;
        topH = p.top;
        bottomH = p.bottom;
        topLeftW = p.topLeftW();
        topLeftH = p.topLeftH();
        topRightW = p.topRightW();
        topRightH = p.topRightH();
        bottomLeftW = p.bottomLeftW();
        bottomLeftH = p.bottomLeftH();
        bottomRightW = p.bottomRightW();
        bottomRightH = p.bottomRightH();

        topLeft      = tile(0, 0, topLeftW, topLeftH);
        topCenter    = tile(topLeftW, 0, atlasW - topLeftW - topRightW, p.top);
        topRight     = tile(atlasW - topRightW, 0, topRightW, topRightH);
        middleLeft   = tile(0, p.top, p.left, atlasH - p.top - p.bottom);
        middleCenter = tile(p.left, p.top, atlasW - p.left - p.right, atlasH - p.top - p.bottom);
        middleRight  = tile(atlasW - p.right, p.top, p.right, atlasH - p.top - p.bottom);
        bottomLeft   = tile(0, atlasH - bottomLeftH, bottomLeftW, bottomLeftH);
        bottomCenter = tile(bottomLeftW, atlasH - p.bottom, atlasW - bottomLeftW - bottomRightW, p.bottom);
        bottomRight  = tile(atlasW - bottomRightW, atlasH - bottomRightH, bottomRightW, bottomRightH);
    }

    private static Atlas.TileJson tile(int x, int y, int w, int h) {
        Atlas.TileJson t = new Atlas.TileJson();
        t.x = x; t.y = y; t.w = w; t.h = h;
        return t;
    }

    private void blit(Atlas.TileJson r, int sx, int sy, int sw, int sh) {
        if (sw <= 0 || sh <= 0) return;
        Element.drawTexRect(sx, sy, sw, sh, r, atlasW, atlasH);
    }

    private void warn(String msg) { System.err.println("ElementDecor(" + name + "): " + msg); }
}
