package me.zed_0xff.XUI;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public abstract class ButtonBase extends TextControl {
    boolean pressed;

    public ButtonBase(Window window, int x, int y, int w, int h, String text) {
        super(window, x, y, w, h, text);
    }

    @Override
    public long cursorAt(int mx, int my) {
        return (visible && enabled && isActiveAt(mx, my)) ? CursorMgr.hand() : 0;
    }

    @Override
    public int hostCursorAt(int mx, int my) {
        return (visible && enabled && isActiveAt(mx, my)) ? Window.HOST_CURSOR_HAND : Window.HOST_CURSOR_DEFAULT;
    }

    @Override
    public void handleMouseButton(int action, int mx, int my) {
        if (!visible || !enabled) {
            if (action == GLFW.GLFW_RELEASE) pressed = false;
            return;
        }
        if (action == GLFW.GLFW_PRESS && isActiveAt(mx, my)) {
            pressed = true;
        } else if (action == GLFW.GLFW_RELEASE) {
            if (pressed && isActiveAt(mx, my)) onClick();
            pressed = false;
        }
    }

    protected void onClick() {}

    protected abstract String styleName();

    protected Iterable<ControlStyle.State> visualStates() {
        int flags = (hovered ? ControlStyle.VISUAL_HOVERED : 0)
                  | (pressed ? ControlStyle.VISUAL_PRESSED : 0);
        return ControlStyle.visualStates(styleName(), flags);
    }

    @Override
    public void render(int originX, int originY) {
        int bx = originX + x, by = originY + y;
        renderStates(styleName(), visualStates(), bx, by, null);
        renderBorder(bx, by, width, height);
        if (text != null && !text.isEmpty()) {
            ControlStyle.Area label = getArea("label");
            Font f = font();
            withTexture(f.fontTex, () -> {
                glColor(styledTextColor(label, textColor));
                drawAlignedText(f, bx, by, width, height, label);
            });
        }
    }

    protected void renderStates(String styleName, Iterable<ControlStyle.State> states, int bx, int by, Color fill) {
        for (ControlStyle.State state : states) {
            if (state == null) {
                continue;
            }
            if ("ninepatch".equals(state.type)) {
                ElementDecor decor = ElementDecor.forState(styleName, state);
                decor.render(bx, by, width, height, fill);
            } else if ("image".equals(state.type) && state.rect != null) {
                renderImageState(state, bx, by, getArea("image"));
            } else {
                Color bg = ControlStyle.bgColor(state);
                if (bg != null) {
                    fillRect(bx, by, width, height, bg);
                }
            }
        }
    }

    private void renderImageState(ControlStyle.State state, int bx, int by, ControlStyle.Area area) {
        Atlas atlas = ControlStyle.atlasFor(state);
        if (atlas == null || !atlas.isLoaded()) {
            return;
        }
        if (atlas.texId == 0) atlas.texId = atlas.uploadTexture();
        Atlas.TileJson tile = tile(state.rect);
        if (atlas.texId == 0 || !atlas.fits(tile)) {
            return;
        }
        withTexture(atlas.texId, () -> {
            int w = tile.w, h = tile.h;
            if (area != null) {
                w = area.w; h = area.h;
            }
            GL11.glColor3f(1f, 1f, 1f); // reset tint color before drawing an image
            drawTexRect(bx, by, w, h, tile, atlas.w, atlas.h);
        });
    }

    private static Atlas.TileJson tile(ControlStyle.RectSpec rect) {
        Atlas.TileJson t = new Atlas.TileJson();
        t.x = rect.x; t.y = rect.y; t.w = rect.w; t.h = rect.h;
        return t;
    }
}
