package me.zed_0xff.XUI;

import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Parsed control visuals from the active theme. */
final class ControlStyle {
    private static String themeName;
    private static Theme theme;
    private static final Map<String, Atlas> atlases = new HashMap<>();
    private static Atlas cursorAtlas;

    private ControlStyle() {}

    static void setThemeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("XUI theme name is not set");
        }
        if (name.equals(themeName)) {
            return;
        }
        themeName = name;
        theme = loadTheme(themeName);
        atlases.clear();
        cursorAtlas = null;
        Element.disposeFont();
    }

    static Control control(String name) {
        return resolveControl(name);
    }

    static State state(String controlName, String stateName) {
        Control control = control(controlName);
        return control != null ? resolveState(control, stateName) : null;
    }

    static Area area(String controlName, String areaName) {
        Control control = control(controlName);
        return control != null && control.areas != null ? control.areas.get(areaName) : null;
    }

    static Atlas atlasFor(State state) {
        if (state == null || state.image == null || state.image.name == null
                || state.image.width < 1 || state.image.height < 1) {
            return null;
        }
        String key = state.image.name + "@" + state.image.width + "x" + state.image.height;
        Atlas atlas = atlases.get(key);
        if (atlas == null) {
            atlas = Atlas.loadResource(themeResource(state.image.name + ".png"),
                    state.image.width, state.image.height);
            if (atlas != null) {
                atlases.put(key, atlas);
            }
        }
        return atlas;
    }

    static Atlas cursorAtlas() {
        Theme theme = requireTheme();
        if (theme.cursors == null) {
            return null;
        }
        if (cursorAtlas == null) {
            cursorAtlas = new Atlas(theme.cursors, themeDir());
        }
        return cursorAtlas;
    }

    static String defaultFontName() {
        Theme theme = requireTheme();
        if (theme.fonts == null || theme.fonts.isEmpty()) {
            throw new IllegalStateException("XUI theme has no fonts");
        }
        return theme.fonts.keySet().iterator().next();
    }

    static String fontJsonResource(String fontName) {
        return fontResource(fontName, fontName + ".json");
    }

    static String fontResource(String fontName, String resourceName) {
        if (resourceName.startsWith("/")) {
            return resourceName;
        }
        return themeDir() + "/fonts/" + resourceName;
    }

    static List<State> visualStates(String controlName, boolean selected, boolean pressed) {
        Control control = control(controlName);
        if (control == null) {
            return List.of();
        }

        State base = resolveState(control, "default");
        ArrayList<State> states = new ArrayList<>();
        if (base != null) {
            states.add(base);
        }
        if (selected) {
            applyActiveState(control, states, "selected");
        }
        if (pressed) {
            applyActiveState(control, states, "pressed");
        }

        return states;
    }

    private static Theme loadTheme(String name) {
        String path = themeDir(name) + "/theme.json";
        Theme theme = Utils.readJson(path, new Gson(), Theme.class);
        if (java.util.Objects.isNull(theme)) {
            throw new IllegalStateException("failed reading " + path + " from classpath");
        }
        return theme;
    }

    private static String themeResource(String resourceName) {
        return resourceName.startsWith("/") ? resourceName : themeDir() + "/" + resourceName;
    }

    private static String themeDir() {
        if (themeName == null) {
            throw new IllegalStateException("XUI theme name is not set");
        }
        return themeDir(themeName);
    }

    private static String themeDir(String name) {
        return "/themes/" + name;
    }

    private static Control resolveControl(String name) {
        Theme theme = requireTheme();
        if (theme.controls == null) {
            return null;
        }
        Control control = theme.controls.get(name);
        if (control == null) {
            return null;
        }
        if (control.extendsName != null) {
            Control parent = resolveControl(control.extendsName);
            control = merge(parent, control);
        }
        Control defaults = theme.controls.get("default");
        if (defaults != null && !"default".equals(name)) {
            control = merge(defaults, control);
        }
        return control;
    }

    private static Theme requireTheme() {
        if (theme == null) {
            throw new IllegalStateException("XUI theme name is not set");
        }
        return theme;
    }

    private static Control merge(Control parent, Control child) {
        if (parent == null) {
            return child;
        }
        Control out = new Control();
        out.type = child.type != null ? child.type : parent.type;
        out.image = mergeImage(parent.image, child.image);
        out.patch = child.patch != null ? child.patch : parent.patch;
        out.areas = parent.areas != null ? new HashMap<>(parent.areas) : null;
        if (child.areas != null) {
            if (out.areas == null) out.areas = new HashMap<>();
            out.areas.putAll(child.areas);
        }
        out.states = parent.states != null ? new HashMap<>(parent.states) : null;
        if (child.states != null) {
            if (out.states == null) out.states = new HashMap<>();
            out.states.putAll(child.states);
        }
        return out;
    }

    private static State resolveState(Control control, String stateName) {
        if (control == null) {
            return null;
        }
        State state = control.states != null ? control.states.get(stateName) : null;
        if (state == null && control.states != null) {
            state = control.states.get("default");
        }
        if (state == null) {
            state = new State();
        }
        State parent = state.extendsName != null
                ? resolveState(control, state.extendsName)
                : (!"default".equals(stateName) ? resolveState(control, "default") : null);
        State out = merge(parent, state);
        out.type = out.type != null ? out.type : control.type;
        out.image = mergeImage(control.image, out.image);
        out.patch = out.patch != null ? out.patch : control.patch;
        return out;
    }

    private static void applyActiveState(Control control, List<State> states, String stateName) {
        if (control.states == null || !control.states.containsKey(stateName)) {
            return;
        }

        State active = resolveState(control, stateName);
        if (active == null) {
            return;
        }
        if (!active.modifier) {
            states.clear();
        }
        states.add(active);
    }

    private static State merge(State parent, State child) {
        if (parent == null) {
            return copyState(child);
        }
        State out = copyState(parent);
        if (child.type != null) out.type = child.type;
        out.image = mergeImage(out.image, child.image);
        if (child.rect != null) out.rect = child.rect;
        if (child.hotspot != null) out.hotspot = child.hotspot;
        if (child.tint != null) out.tint = child.tint;
        out.modifier = child.modifier || out.modifier;
        return out;
    }

    private static State copyState(State in) {
        State out = new State();
        if (in == null) {
            return out;
        }
        out.extendsName = in.extendsName;
        out.type = in.type;
        out.image = in.image;
        out.patch = in.patch;
        out.rect = in.rect;
        out.hotspot = in.hotspot;
        out.tint = in.tint;
        out.modifier = in.modifier;
        return out;
    }

    private static ImageSpec mergeImage(ImageSpec parent, ImageSpec child) {
        if (parent == null) {
            return child;
        }
        if (child == null) {
            return parent;
        }
        ImageSpec out = new ImageSpec();
        out.name = child.name != null ? child.name : parent.name;
        out.width = child.width > 0 ? child.width : parent.width;
        out.height = child.height > 0 ? child.height : parent.height;
        return out;
    }

    static final class Theme {
        Map<String, Control> controls;
        Atlas.JsonBase cursors;
        Map<String, Object> fonts;
    }

    static final class Control {
        String type;
        ImageSpec image;
        Patch patch;
        Map<String, Area> areas;
        Map<String, State> states;
        @com.google.gson.annotations.SerializedName("extends")
        String extendsName;
    }

    static final class State {
        @com.google.gson.annotations.SerializedName("extends")
        String extendsName;
        String type;
        ImageSpec image;
        Patch patch;
        RectSpec rect;
        Point hotspot;
        String tint;
        boolean modifier;
    }

    static final class ImageSpec {
        String name;
        @SerializedName(value = "w", alternate = "width")
        int width;
        @SerializedName(value = "h", alternate = "height")
        int height;
    }

    static final class Patch {
        int left, right, top, bottom;
        int[] topLeft, topRight, bottomLeft, bottomRight;

        int topLeftW() { return dim(topLeft, 0, left); }
        int topLeftH() { return dim(topLeft, 1, top); }
        int topRightW() { return dim(topRight, 0, right); }
        int topRightH() { return dim(topRight, 1, top); }
        int bottomLeftW() { return dim(bottomLeft, 0, left); }
        int bottomLeftH() { return dim(bottomLeft, 1, bottom); }
        int bottomRightW() { return dim(bottomRight, 0, right); }
        int bottomRightH() { return dim(bottomRight, 1, bottom); }

        private static int dim(int[] values, int idx, int fallback) {
            return values != null && values.length > idx && values[idx] > 0 ? values[idx] : fallback;
        }
    }

    static final class RectSpec {
        int x, y;
        @SerializedName(value = "w", alternate = "width")
        int w;
        @SerializedName(value = "h", alternate = "height")
        int h;
    }

    static final class Point {
        int x, y;
    }

    static final class Area {
        Integer x, y;
        @SerializedName(value = "w", alternate = "width")
        Integer w;
        @SerializedName(value = "h", alternate = "height")
        Integer h;

        Integer left, top, right, bottom;
        Text text;

        int x(int fallback) { return x != null ? x : fallback; }
        int y(int fallback) { return y != null ? y : fallback; }
        int w(int fallback) { return w != null ? w : fallback; }
        int h(int fallback) { return h != null ? h : fallback; }
        int left(int fallback) { return left != null ? left : fallback; }
        int top(int fallback) { return top != null ? top : fallback; }
        int right(int fallback) { return right != null ? right : fallback; }
        int bottom(int fallback) { return bottom != null ? bottom : fallback; }
        boolean hasX() { return x != null; }
        boolean hasY() { return y != null; }
        boolean hasW() { return w != null; }
        boolean hasH() { return h != null; }
        boolean hasLeft() { return left != null; }
        boolean hasTop() { return top != null; }
        boolean hasRight() { return right != null; }
        boolean hasBottom() { return bottom != null; }
    }

    static final class Text {
        String align;
        String valign;
        String color;
    }
}
