package me.zed_0xff.WUI;

import org.lwjgl.glfw.GLFW;

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
}
