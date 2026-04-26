package me.zed_0xff.WUI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.google.gson.Gson;

public class Font {
    Map<Integer, GlyphJson> glyphById = new HashMap<>();
    Map<Long, Integer> kernAmount = new HashMap<>();
    GlyphJson spaceGlyph;
    int atlasW;
    int atlasH;
    int fontTex;
    FaceJson face;

    static final class FontJson {
        AtlasJson atlas;
        FaceJson face;
        List<GlyphJson> glyphs;
        List<KerningJson> kernings;
    }

    static final class AtlasJson {
        int width, height;
        String image;
    }

    static final class FaceJson {
        String family;
        int size;
        int lineHeight;
        int base;
        int[] padding;
        int[] spacing;
    }

    static final class GlyphJson {
        int id, x, y, w, h, xo, yo, xa;
    }

    static final class KerningJson {
        int first, second, amount;
    }

    public Font() {
        FontJson cfg = Utils.readJson("/font.json", new Gson(), FontJson.class);
        if (cfg == null) throw new IllegalStateException("failed reading /font.json from classpath");
        if (cfg.glyphs == null || cfg.glyphs.isEmpty()) throw new IllegalStateException("no glyphs in font.json");

        Atlas a = Atlas.loadResource("/" + cfg.atlas.image, cfg.atlas.width, cfg.atlas.height);
        if (a == null) throw new RuntimeException("failed loading font atlas: " + cfg.atlas.image);
        atlasW = a.w;
        atlasH = a.h;
        fontTex = a.uploadTexture();

        for (GlyphJson g : cfg.glyphs) glyphById.put(g.id, g);
        spaceGlyph = glyphById.getOrDefault(32, cfg.glyphs.get(0));
        if (cfg.kernings != null) {
            for (KerningJson k : cfg.kernings) kernAmount.put(Utils.packPair(k.first, k.second), k.amount);
        }
        face = cfg.face;
    }

    /** Horizontal advance of the first line, in font pixels (same rules as {@link #drawText}). */
    int measureTextAdvancePx(String s) {
        if (s == null || s.isEmpty()) return 0;
        int relX = 0, prevCp = -1;
        for (int off = 0; off < s.length();) {
            int cp = s.codePointAt(off);
            off += Character.charCount(cp);
            if (cp == '\n') break;
            if (prevCp >= 0) relX += kerningDelta(prevCp, cp);
            relX += glyphById.getOrDefault(cp, spaceGlyph).xa;
            prevCp = cp;
        }
        return relX;
    }

    /** Draw {@code s} starting at ({@code x}, {@code y}), top-left origin, y downward. */
    void drawText(int x, int y, String s) {
        int relX = 0, relLineY = 0, prevCp = -1;
        int lineSkip = face.lineHeight;

        GL11.glBegin(GL11.GL_QUADS);
        for (int off = 0; off < s.length();) {
            int cp = s.codePointAt(off);
            off += Character.charCount(cp);
            if (cp == '\n') { relX = 0; relLineY += lineSkip; prevCp = -1; continue; }
            if (prevCp >= 0) relX += kerningDelta(prevCp, cp);
            GlyphJson g = glyphById.getOrDefault(cp, spaceGlyph);
            if (g.w > 0 && g.h > 0) {
                float sx = x + relX + g.xo, sy = y + relLineY + g.yo;
                Element.glTexQuad(sx, sy, sx + g.w, sy + g.h,
                    g.x / (float) atlasW,         g.y / (float) atlasH,
                    (g.x + g.w) / (float) atlasW, (g.y + g.h) / (float) atlasH);
            }
            relX += g.xa;
            prevCp = cp;
        }
        GL11.glEnd();
    }

    private int kerningDelta(int prevCp, int cp) {
        Integer k = kernAmount.get(Utils.packPair(prevCp, cp));
        return k != null ? k : 0;
    }

    /**
     * Draw a single-line string centered horizontally within {@code boxW} and vertically around {@code centerY}.
     * X centering uses advance (xa + kerning). Y centering uses face.base within face.lineHeight.
     *
     * @param boxX    left of box
     * @param centerY absolute vertical center for the text
     * @param boxW    width of box
     * @param s       text to draw
     */
    void drawTextCentered(int boxX, int centerY, int boxW, String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        int tw = measureTextAdvancePx(s);
        int tx = boxX + (boxW - tw) / 2;

        int baselineY = centerY + face.base - face.lineHeight / 2;
        int ty = baselineY - face.base;

        drawText(tx, ty, s);
    }
}
