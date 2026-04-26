package me.zed_0xff.XUI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.lwjgl.opengl.GL11;

public class Color {
    public static final Color BLACK = new Color(0x000000);
    public static final Color GRAY  = new Color(0xc0c0c0);
    public static final Color NAVY  = new Color(0x0000b8);
    public static final Color WHITE = new Color(0xffffff);

    private final float r, g, b, a;

    public Color(int value) {
        this(
            ((value >> 16) & 0xff) / 255.0f,
            ((value >>  8) & 0xff) / 255.0f,
            ( value        & 0xff) / 255.0f,
            1.0f
        );
    }

    public Color(float r, float g, float b, float a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
    }

    public int value() {
        return (toByte(r) << 16) | (toByte(g) << 8) | toByte(b);
    }

    static Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(Color.class, (com.google.gson.JsonDeserializer<Color>) (json, type, ctx) ->
                        fromJson(json, BLACK))
                .create();
    }

    static Color fromJson(JsonElement json, Color fallback) {
        if (json == null || json.isJsonNull()) {
            return fallback;
        }
        if (json.isJsonPrimitive()) {
            return parse(json.getAsString(), fallback);
        }
        if (!json.isJsonObject()) {
            return fallback;
        }
        JsonObject obj = json.getAsJsonObject();
        return new Color(
                component(obj, "r", fallback.r),
                component(obj, "g", fallback.g),
                component(obj, "b", fallback.b),
                component(obj, "a", fallback.a));
    }

    public static Color parse(String color, Color fallback) {
        if (color == null || color.isEmpty()) {
            return fallback;
        }
        try {
            if (color.charAt(0) == '#') {
                return new Color(Integer.parseInt(color.substring(1), 16));
            }
            return new Color(Integer.parseInt(color, 16));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public float getRf() { return r; }
    public float getGf() { return g; }
    public float getBf() { return b; }
    public float getAf() { return a; }

    /** Apply this color as the current GL draw color. */
    public void applyGl() {
        GL11.glColor4f(r, g, b, a);
    }

    private static float component(JsonObject obj, String name, float fallback) {
        JsonElement value = obj.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsFloat() : fallback;
    }

    private static int toByte(float value) {
        return Math.round(clamp(value) * 255.0f);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
