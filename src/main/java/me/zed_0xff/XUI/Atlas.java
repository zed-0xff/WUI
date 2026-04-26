package me.zed_0xff.XUI;

import com.google.gson.Gson;

import org.lwjgl.BufferUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

/**
 * Loaded image atlas shared by {@link ElementDecor} and {@link CursorMgr}.
 * All assets are loaded from the JAR classpath (e.g. {@code /window.json}).
 */
final class Atlas {
    final BufferedImage img;
    final int w, h;
    java.util.Map<String, TileJson> tiles;
    java.util.Map<String, String> metadata;
    int texId;

    private Atlas(BufferedImage img, int w, int h) {
        this.img = img; this.w = w; this.h = h;
    }

    /** Load atlas by name: reads {@code /{name}.json} + its image from the classpath. */
    Atlas(String name) {
        JsonBase cfg = Utils.readJson("/" + name + ".json", new Gson(), JsonBase.class);
        Atlas a = fromConfig(cfg, "/");
        this.img = a.img; this.w = a.w; this.h = a.h;
        this.tiles = a.tiles; this.metadata = a.metadata;
    }

    Atlas(JsonBase cfg) {
        Atlas a = fromConfig(cfg, "/");
        this.img = a.img; this.w = a.w; this.h = a.h;
        this.tiles = a.tiles; this.metadata = a.metadata;
    }

    Atlas(JsonBase cfg, String basePath) {
        Atlas a = fromConfig(cfg, basePath);
        this.img = a.img; this.w = a.w; this.h = a.h;
        this.tiles = a.tiles; this.metadata = a.metadata;
    }

    private static Atlas fromConfig(JsonBase cfg, String basePath) {
        BufferedImage loaded = null;
        int lw = 0, lh = 0;
        if (cfg != null && cfg.image != null && cfg.atlas != null) {
            Atlas a = loadResource(resourcePath(basePath, cfg.image), cfg.atlas.width, cfg.atlas.height);
            if (a != null) { loaded = a.img; lw = a.w; lh = a.h; }
        }
        Atlas out = new Atlas(loaded, lw, lh);
        out.tiles = cfg != null ? cfg.tiles : null;
        out.metadata = cfg != null ? cfg.metadata : null;
        return out;
    }

    private static String resourcePath(String basePath, String image) {
        if (image.startsWith("/")) {
            return image;
        }
        String base = basePath != null && !basePath.isEmpty() ? basePath : "/";
        return base.endsWith("/") ? base + image : base + "/" + image;
    }

    boolean isLoaded() { return img != null; }

    int getMetaInt(String key, int fallback) {
        return parseMeta(metadata, key, fallback);
    }

    static int parseMeta(java.util.Map<String, String> map, String key, int fallback) {
        if (map == null) return fallback;
        String v = map.get(key);
        if (v == null) return fallback;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return fallback; }
    }

    /**
     * Load a PNG from the classpath and verify it matches the declared atlas dimensions.
     * @return loaded Atlas, or {@code null} on any error
     */
    static Atlas loadResource(String resourcePath, int atlasW, int atlasH) {
        if (atlasW < 1 || atlasH < 1) { warn("invalid size " + atlasW + "x" + atlasH); return null; }
        BufferedImage img;
        try (InputStream is = Atlas.class.getResourceAsStream(resourcePath)) {
            if (is == null) { warn("resource not found: " + resourcePath); return null; }
            img = ImageIO.read(is);
        } catch (IOException e) {
            warn("failed reading " + resourcePath + ": " + e.getMessage()); return null;
        }
        if (img == null) { warn("ImageIO returned null for " + resourcePath); return null; }
        if (img.getWidth() != atlasW || img.getHeight() != atlasH) {
            warn("png " + img.getWidth() + "x" + img.getHeight()
                + " != declared " + atlasW + "x" + atlasH + " (" + resourcePath + ")");
            return null;
        }
        return new Atlas(img, atlasW, atlasH);
    }

    /** True if {@code t} has positive dimensions and lies entirely within the atlas. */
    boolean fits(TileJson t) {
        return t != null && t.w >= 1 && t.h >= 1
            && t.x >= 0 && t.y >= 0
            && t.x + t.w <= w && t.y + t.h <= h;
    }

    /** Upload the atlas image as an OpenGL texture and return the texture ID (0 on failure). */
    int uploadTexture() {
        return Utils.uploadRgbaTexture2d(img);
    }

    /** Extract one tile cell as an RGBA8888 {@link ByteBuffer}, top-row first (GLFW / GL convention). */
    ByteBuffer cellToRgba(TileJson t) {
        ByteBuffer buf = BufferUtils.createByteBuffer(t.w * t.h * 4);
        for (int y = 0; y < t.h; y++)
            for (int x = 0; x < t.w; x++)
                Utils.putArgbAsRgba(buf, img.getRGB(t.x + x, t.y + y));
        buf.flip();
        return buf;
    }

    // --- shared JSON model ---

    static class JsonBase {
        String image;
        SizeJson atlas;
        java.util.Map<String, TileJson> tiles;
        java.util.Map<String, String> metadata;
    }

    static final class SizeJson {
        int width, height;
    }

    static class TileJson {
        int x, y, w, h;
        java.util.Map<String, String> metadata;

        int getMetaInt(String key, int fallback) {
            return parseMeta(metadata, key, fallback);
        }
    }

    private static void warn(String msg) {
        System.err.println("Atlas: " + msg);
    }
}
