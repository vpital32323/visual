package com.troizy.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/** Меню в стиле Evolution: стеклянное окно, боковая панель, карточки модулей, плавные анимации. */
public class GlassScreen extends Screen {
    private static final String[] TABS = {"Combat", "Render", "Misc", "Cosmetics", "Crosshair", "Themes", "Config"};
    private static final int[] SWATCH = {0xFFFFFF, 0x3DDC84, 0xFFE94D, 0xFFB020, 0xFF7A30, 0xFF4D4D, 0xFF5FD2, 0x35E0FF};
    private static final int[][] PRESETS = {{0, 0xFFFFFF}, {2, 0xFFFFFF}, {3, 0xFFFFFF}, {0, 0x35E0FF}, {1, 0x3DDC84}, {4, 0xFF4D4D}};
    private static final int SW = 118;
    private static final int TXT = 0xFFB4B8C4;

    private record Hit(int x, int y, int w, int h, DoubleConsumer left, Runnable right, boolean drag) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private int tab = 0;
    private float p = 0f;              // прогресс открытия 0..1
    private boolean closing = false;
    private float tabAge = 0f, dt = 0.016f, scroll = 0f, scrollTarget = 0f, contentH = 0f;
    private long last = System.nanoTime();
    private float sc = 1f, slide = 0f;
    private int mx, my, vy, vb;
    private String search = "";
    private boolean searchFocus = false;
    private final Map<String, Float> hov = new HashMap<>();
    private final Set<String> expanded = new HashSet<>();
    private final List<Hit> hits = new ArrayList<>();
    private Hit dragging;

    public GlassScreen() { super(Text.literal(Settings.NAME)); }

    @Override public boolean shouldPause() { return false; }

    @Override public void removed() { Settings.save(); }

    @Override public void close() { closing = true; }

    // ---------- помощники ----------
    private int acc() { return Settings.accent(); }

    private boolean over(int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    private float anim(String id, float target) {
        float v = Gfx.smooth(hov.getOrDefault(id, target), target, dt, 14f);
        hov.put(id, v);
        return v;
    }

    private void hit(int x, int y, int w, int h, DoubleConsumer left, Runnable right, boolean drag) {
        hits.add(new Hit(x, y, w, h, left, right, drag));
    }

    /** Клик только внутри видимой области списка. */
    private void vhit(int x, int y, int w, int h, DoubleConsumer left, Runnable right, boolean drag) {
        int ny = Math.max(y, vy), nb = Math.min(y + h, vb);
        if (nb <= ny) return;
        hits.add(new Hit(x, ny, w, nb - ny, left, right, drag));
    }

    private int header(DrawContext c, String t, int x, int y) {
        Gfx.circle(c, x + 2, y + 5, 2, 0xFF000000 | acc());
        Gfx.textC(c, t, x + 9, y + 5, 0xA0FFFFFF);
        return y + 15;
    }

    private int tgl(DrawContext c, String id, String label, boolean val, Consumer<Boolean> set, int x, int y, int w, boolean active) {
        float h = anim(id + "h", over(x, y, w, 18) ? 1 : 0);
        float k = anim(id, val ? 1 : 0);
        Gfx.textC(c, label, x, y + 9, Gfx.mix(0xFFB4B8C4, 0xFFFFFFFF, h));
        int tx = x + w - 26;
        Gfx.rrect(c, tx, y + 3, 24, 12, 6, Gfx.mix(0x55FFFFFF, 0xFF000000 | acc(), k));
        Gfx.circle(c, tx + 7 + (int) (10 * k), y + 9, 4, 0xFFFFFFFF);
        if (active) vhit(x, y, w, 18, v -> set.accept(!val), null, false);
        return y + 20;
    }

    private int sld(DrawContext c, String id, String label, int min, int max, int val, IntConsumer set, int x, int y, int w, boolean active) {
        Gfx.textC(c, label, x, y + 5, TXT);
        Gfx.textRight(c, "" + val, x + w, y + 1, 0xFFFFFFFF);
        int by = y + 15;
        float f = (val - min) / (float) Math.max(1, max - min);
        float fa = anim(id, f);
        int fw = Math.max(4, (int) (w * fa));
        Gfx.rrect(c, x, by, w, 4, 2, 0x40FFFFFF);
        Gfx.rrect(c, x, by, fw, 4, 2, 0xFF000000 | acc());
        Gfx.circle(c, x + (int) (w * fa), by + 2, 5, 0xFFFFFFFF);
        if (active) vhit(x - 4, y + 8, w + 8, 20, m -> {
            float t = (float) Math.max(0, Math.min(1, (m - x) / w));
            set.accept(min + Math.round(t * (max - min)));
        }, null, true);
        return y + 28;
    }

    private void swatch(DrawContext c, String id, int rgb, int x, int y, boolean sel, boolean themeDot, Runnable r, boolean active) {
        float k = anim(id, over(x - 2, y - 2, 16, 16) ? 1 : 0);
        if (sel || k > 0.05f) Gfx.circle(c, x + 6, y + 6, 7, sel ? 0xFFFFFFFF : Gfx.rgba(0xFFFFFF, (int) (120 * k)));
        Gfx.circle(c, x + 6, y + 6, 5, 0xFF000000 | rgb);
        if (themeDot) Gfx.circle(c, x + 6, y + 6, 2, 0xFFFFFFFF);
        if (active) vhit(x - 2, y - 2, 16, 16, m -> r.run(), null, false);
    }

    private int clr(DrawContext c, String id, String label, int cur, boolean themeOpt, IntConsumer set, int x, int y, int w, boolean active) {
        Gfx.textC(c, label, x, y + 5, TXT);
        int sx = x, sy = y + 12;
        if (themeOpt) {
            swatch(c, id + "t", Settings.accent(), sx, sy, cur == -1, true, () -> set.accept(-1), active);
            sx += 17;
        }
        for (int i = 0; i < SWATCH.length; i++) {
            final int col = SWATCH[i];
            swatch(c, id + i, col, sx + i * 17, sy, cur == col, false, () -> set.accept(col), active);
        }
        return y + 30;
    }

    private void btn(DrawContext c, String id, String label, int x, int y, int w, int h, boolean sel, Runnable r) {
        float k = anim(id, over(x, y, w, h) ? 1 : 0);
        Gfx.rrect(c, x, y, w, h, 5, Gfx.rgba(acc(), (int) ((sel ? 0x55 : 0x18) + 0x28 * k)));
        Gfx.outline(c, x, y, w, h, 5, sel ? (0xCC000000 | acc()) : 0x30FFFFFF);
        if (label != null) Gfx.textC(c, label, x + (w - Gfx.width(label)) / 2, y + h / 2, 0xFFFFFFFF);
        vhit(x, y, w, h, m -> r.run(), null, false);
    }

    // ---------- рендер ----------
    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        dt = Math.min(0.05f, (now - last) / 1e9f);
        last = now;

        float speed = Settings.animSpeed / 100f;
        if (!Settings.anims) p = closing ? 0f : 1f;
        else p = Gfx.clamp01(p + (closing ? -dt / 0.24f : dt / 0.42f) * speed);
        if (closing && p <= 0f) {
            if (client != null) client.setScreen(null);
            return;
        }
        tabAge += dt;
        scroll = Gfx.smooth(scroll, scrollTarget, dt, 16f);

        float e = closing ? p * p * (3f - 2f * p) : Gfx.easeOutCubic(p);
        sc = closing ? 0.9f + 0.1f * e : 0.86f + 0.14f * Gfx.easeOutBack(p);
        slide = (1f - e) * 22f;
        float alpha = Gfx.clamp01(e * 1.35f);
        mx = (int) ((mouseX - width / 2f) / sc + width / 2f);
        my = (int) ((mouseY - slide - height / 2f) / sc + height / 2f);
        hits.clear();

        Gfx.alpha = alpha;
        c.fillGradient(0, 0, width, height, Gfx.a(0xB005060A), Gfx.a(Gfx.rgba(Gfx.shade(acc(), 0.22f), 0x80)));

        c.getMatrices().push();
        c.getMatrices().translate(width / 2f, height / 2f + slide, 0);
        c.getMatrices().scale(sc, sc, 1f);
        c.getMatrices().translate(-width / 2f, -height / 2f, 0);
        try { draw(c); } finally {
            c.getMatrices().pop();
            Gfx.alpha = 1f;
        }
    }

    private void draw(DrawContext c) {
        int W = Math.min(580, width - 16), H = Math.min(340, height - 16);
        int x0 = (width - W) / 2, y0 = (height - H) / 2, r = Settings.radius + 4;
        int acc = acc();

        for (int i = 8; i >= 1; i--) Gfx.rrect(c, x0 - i, y0 - i, W + i * 2, H + i * 2, r + i, Gfx.rgba(acc, 4));
        Gfx.glass(c, x0, y0, W, H, r, 0x0A0C12, Settings.bodyAlpha() + 24, Gfx.rgba(acc, 0x80));

        // --- боковая панель ---
        int sx = x0 + 8, sy = y0 + 8, sh = H - 16;
        Gfx.rrect(c, sx, sy, SW, sh, r - 2, 0x38000000);
        Gfx.outline(c, sx, sy, SW, sh, r - 2, 0x1CFFFFFF);
        Gfx.circle(c, sx + 16, sy + 18, 6, 0xFF000000 | acc);
        Gfx.circle(c, sx + 16, sy + 18, 3, 0x66FFFFFF);
        float ns = Math.min(1f, (SW - 36) / (float) Math.max(1, Gfx.width(Settings.NAME)));
        Gfx.textScaled(c, Settings.NAME, sx + 28, sy + 18 - (int) (4 * ns), 0xFFFFFFFF, ns);
        Gfx.fill(c, sx + 8, sy + 34, sx + SW - 8, sy + 35, 0x22FFFFFF);

        for (int i = 0; i < TABS.length; i++) {
            int ty = sy + 42 + i * 24;
            boolean selected = i == tab && search.isEmpty();
            float hv = anim("tabh" + i, over(sx + 6, ty, SW - 12, 20) ? 1 : 0);
            float se = anim("tabs" + i, selected ? 1 : 0);
            Gfx.rrect(c, sx + 6, ty, SW - 12, 20, 5, Gfx.rgba(acc, (int) (0x50 * se + 0x1E * hv)));
            if (se > 0.02f) Gfx.rrect(c, sx + 6, ty + 4, 2, 12, 1, Gfx.rgba(acc, (int) (255 * se)));
            Gfx.circle(c, sx + 19, ty + 10, 2, Gfx.mix(0x80FFFFFF, 0xFF000000 | acc, se));
            Gfx.textC(c, TABS[i], sx + 28 + (int) (3 * hv), ty + 10, Gfx.mix(0xFFA8ACB8, 0xFFFFFFFF, Math.max(se, hv)));
            if (i < 4) {
                int n = 0;
                for (Module m : Modules.ALL) if (m.cat.ordinal() == i && m.enabled) n++;
                if (n > 0) Gfx.textScaled(c, "" + n, sx + SW - 18, ty + 6, 0x90FFFFFF, 0.8f);
            }
            final int idx = i;
            hit(sx + 6, ty, SW - 12, 20, m -> { search = ""; searchFocus = false; setTab(idx); }, null, false);
        }
        Gfx.fill(c, sx + 8, sy + sh - 34, sx + SW - 8, sy + sh - 33, 0x22FFFFFF);
        String user = MinecraftClient.getInstance().getSession().getUsername();
        Gfx.textC(c, user, sx + 10, sy + sh - 23, 0xFFD0D3DC);
        Gfx.textC(c, "Right Shift", sx + 10, sy + sh - 11, 0x70FFFFFF);

        // --- шапка контента ---
        int cx0 = sx + SW + 10, cw = x0 + W - 8 - cx0, cy0 = y0 + 10;
        Gfx.circle(c, cx0 + 3, cy0 + 11, 3, 0xFF000000 | acc);
        String title = search.isEmpty() ? TABS[tab] : "Search";
        Gfx.textScaled(c, title, cx0 + 11, cy0 + 5, 0xFFFFFFFF, 1.2f);

        int sbw = Math.min(150, cw / 2), sbx = cx0 + cw - sbw, sby = cy0 + 2;
        float sf = anim("searchf", searchFocus ? 1 : 0);
        Gfx.rrect(c, sbx, sby, sbw, 18, 5, Gfx.mix(0x28FFFFFF, Gfx.rgba(acc, 0x40), sf));
        Gfx.outline(c, sbx, sby, sbw, 18, 5, Gfx.mix(0x30FFFFFF, 0xCC000000 | acc, sf));
        if (search.isEmpty() && !searchFocus) Gfx.textC(c, "Search", sbx + 8, sby + 9, 0x70FFFFFF);
        else {
            String shown = search + ((System.currentTimeMillis() / 450) % 2 == 0 && searchFocus ? "|" : "");
            Gfx.textC(c, shown, sbx + 8, sby + 9, 0xFFFFFFFF);
        }
        hit(sbx, sby, sbw, 18, m -> searchFocus = true, null, false);
        Gfx.fill(c, cx0, cy0 + 26, cx0 + cw, cy0 + 27, 0x22FFFFFF);

        // --- область списка ---
        vy = cy0 + 32;
        vb = y0 + H - 10;
        c.enableScissor(cx0 - 3, vy, cx0 + cw + 3, vb);
        try {
            int top = vy - Math.round(scroll);
            int end;
            if (!search.isEmpty() || tab < 4) end = moduleList(c, cx0, top, cw);
            else {
                float tf = Gfx.easeOutCubic(tabAge / 0.3f);
                float keep = Gfx.alpha;
                Gfx.alpha = keep * tf;
                int ox = cx0 + (int) ((1f - tf) * 12);
                end = switch (tab) {
                    case 4 -> crosshairTab(c, ox, top, cw);
                    case 5 -> themesTab(c, ox, top, cw - 4);
                    default -> configTab(c, ox, top, cw - 4);
                };
                Gfx.alpha = keep;
            }
            contentH = end - top;
        } finally {
            c.disableScissor();
        }
        float maxScroll = Math.max(0f, contentH - (vb - vy));
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        if (maxScroll > 1f) {
            int trackH = vb - vy;
            int th = Math.max(16, (int) (trackH * trackH / (trackH + maxScroll)));
            int ty = vy + (int) ((trackH - th) * (scroll / maxScroll));
            Gfx.rrect(c, cx0 + cw + 1, ty, 2, th, 1, Gfx.rgba(acc, 0xA0));
        }
    }

    private void setTab(int i) {
        if (tab != i) { tab = i; tabAge = 0f; scroll = 0f; scrollTarget = 0f; }
    }

    // ---------- карточки модулей ----------
    private int moduleList(DrawContext c, int x, int top, int w) {
        List<Module> mods = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Module m : Modules.ALL) {
            if (m.cat == Module.Cat.CROSSHAIR) continue;
            if (q.isEmpty() ? m.cat.ordinal() == tab : m.name.toLowerCase(Locale.ROOT).contains(q)) mods.add(m);
        }
        int y = top;
        for (int i = 0; i < mods.size(); i++) y += card(c, mods.get(i), x, y, w, i) + 6;
        if (mods.isEmpty()) Gfx.textC(c, "Nothing found", x + 4, y + 10, 0x80FFFFFF);
        return y;
    }

    private int settingsHeight(Module m) {
        int h = 8;
        for (Setting s : m.settings) h += s.type == Setting.Type.BOOL ? 20 : s.type == Setting.Type.NUM ? 28 : 30;
        return h;
    }

    private int card(DrawContext c, Module m, int x, int y, int w, int idx) {
        final int h0 = 36;
        boolean hasSet = !m.settings.isEmpty();
        boolean isOpen = expanded.contains(m.id);
        m.expand = Gfx.smooth(m.expand, isOpen ? 1 : 0, dt, 14f);
        int sh = settingsHeight(m);
        int h = h0 + (hasSet ? Math.round(m.expand * sh) : 0);

        float ca = Gfx.easeOutCubic((tabAge - idx * 0.05f) / 0.3f);
        int yo = Math.round((1f - ca) * 14);
        y += yo;
        if (y > vb || y + h < vy) return h;      // за пределами видимой области

        float keep = Gfx.alpha;
        Gfx.alpha = keep * ca;
        int acc = acc();
        boolean hover = over(x, y, w, h0) && my >= vy && my < vb;
        m.hov = Gfx.smooth(m.hov, hover ? 1 : 0, dt, 14f);
        m.sw = Gfx.smooth(m.sw, m.enabled ? 1 : 0, dt, 14f);

        Gfx.rrect(c, x, y, w, h, 7, Gfx.mix(Gfx.mix(0x16FFFFFF, 0x24FFFFFF, m.hov), Gfx.rgba(acc, 0x34), m.sw));
        Gfx.outline(c, x, y, w, h, 7, Gfx.mix(0x22FFFFFF, Gfx.rgba(acc, 0xB0), m.sw));
        Gfx.textC(c, m.name, x + 12, y + 12, Gfx.mix(0xFFC4C8D4, 0xFFFFFFFF, Math.max(m.sw, m.hov)));
        Gfx.textScaled(c, m.desc, x + 12, y + 21, 0x80FFFFFF, 0.78f);

        int tx = x + w - 28 - (hasSet ? 24 : 0), ty = y + (h0 - 14) / 2;
        Gfx.rrect(c, tx, ty, 28, 14, 7, Gfx.mix(0x55FFFFFF, 0xFF000000 | acc, m.sw));
        Gfx.circle(c, tx + 7 + (int) (14 * m.sw), ty + 7, 5, 0xFFFFFFFF);

        if (hasSet) {
            int cx = x + w - 15, cy = y + h0 / 2;
            boolean up = m.expand > 0.5f;
            int col = Gfx.mix(0xA0FFFFFF, 0xFFFFFFFF, m.hov);
            for (int k = 0; k < 4; k++) {
                int yy = up ? cy + 1 - k : cy - 2 + k;
                Gfx.fill(c, cx - 3 + k, yy, cx + 4 - k, yy + 1, col);
            }
            vhit(x + w - 30, y, 30, h0, v -> toggleExpand(m), () -> toggleExpand(m), false);
            vhit(x, y, w - 30, h0, v -> m.enabled = !m.enabled, () -> toggleExpand(m), false);
        } else {
            vhit(x, y, w, h0, v -> m.enabled = !m.enabled, null, false);
        }

        if (hasSet && m.expand > 0.02f) {
            float k = Gfx.clamp01(m.expand);
            Gfx.alpha = keep * ca * k;
            Gfx.fill(c, x + 10, y + h0 - 2, x + w - 10, y + h0 - 1, 0x1EFFFFFF);
            boolean active = m.expand > 0.9f;
            int ix = x + 12, iw = w - 24, sy = y + h0 + 4;
            for (Setting s : m.settings) {
                String id = m.id + "." + s.key;
                switch (s.type) {
                    case BOOL -> sy = tgl(c, id, s.label, s.b(), v -> s.v = v ? 1 : 0, ix, sy, iw, active);
                    case NUM -> sy = sld(c, id, s.label, s.min, s.max, s.v, v -> s.v = v, ix, sy, iw, active);
                    case COLOR -> sy = clr(c, id, s.label, s.v, s.def == -1, v -> s.v = v, ix, sy, iw, active);
                }
            }
        }
        Gfx.alpha = keep;
        return h;
    }

    private void toggleExpand(Module m) {
        if (!expanded.remove(m.id)) expanded.add(m.id);
    }

    // ---------- вкладка Crosshair ----------
    private int crosshairTab(DrawContext c, int x, int y, int w) {
        Module m = Modules.CROSSHAIR;
        boolean prev = w >= 340;
        int cw = prev ? w - 116 : w;
        int startY = y;

        y = tgl(c, "cOn", "Enabled", m.enabled, v -> m.enabled = v, x, y, cw, true);

        y = header(c, "Presets", x, y + 2);
        int bw = (cw - 5 * 4) / 6;
        for (int i = 0; i < 6; i++) {
            int bx = x + i * (bw + 4);
            final int[] pr = PRESETS[i];
            boolean sel = m.i("style") == pr[0] && m.get("color").v == pr[1] && !m.b("chroma");
            btn(c, "pre" + i, null, bx, y, bw, 22, sel, () -> {
                m.get("style").v = pr[0]; m.get("color").v = pr[1]; m.get("chroma").v = 0;
            });
            Crosshair.draw(c, bx + bw / 2, y + 11, pr[0], pr[1], 3, 2, 1);
        }
        y += 26;

        y = header(c, "Style", x, y);
        for (int i = 0; i < Crosshair.STYLES; i++) {
            int bx = x + i * (bw + 4);
            final int st = i;
            btn(c, "sty" + i, null, bx, y, bw, 22, m.i("style") == i, () -> m.get("style").v = st);
            Crosshair.draw(c, bx + bw / 2, y + 11, i, Crosshair.currentColor(), 3, 2, 1);
        }
        y += 26;

        y = header(c, "Quick colors", x, y);
        for (int i = 0; i < SWATCH.length; i++) {
            final int col = SWATCH[i];
            swatch(c, "qc" + i, col, x + i * 17, y, !m.b("chroma") && m.get("color").v == col, false,
                    () -> { m.get("color").v = col; m.get("chroma").v = 0; }, true);
        }
        y += 20;
        y = tgl(c, "chroma", "Chroma (rainbow)", m.b("chroma"), v -> m.get("chroma").v = v ? 1 : 0, x, y, cw, true);
        y = sld(c, "xs", "Size", 1, 12, m.i("size"), v -> m.get("size").v = v, x, y + 2, cw, true);
        y = sld(c, "xg", "Gap", 0, 10, m.i("gap"), v -> m.get("gap").v = v, x, y, cw, true);
        y = sld(c, "xt", "Thickness", 1, 3, m.i("thick"), v -> m.get("thick").v = v, x, y, cw, true);

        if (prev) {
            int px = x + cw + 14, py = startY + 4, ps = 100;
            Gfx.rrectV(c, px, py, ps, ps, 8, 0xFF6C93C4, 0xFF2B3C55);
            Gfx.fill(c, px, py + ps / 2 + 8, px + ps, py + ps, 0x00000000);
            Gfx.outline(c, px, py, ps, ps, 8, 0x40FFFFFF);
            c.getMatrices().push();
            c.getMatrices().translate(px + ps / 2f, py + ps / 2f, 0);
            c.getMatrices().scale(3f, 3f, 1f);
            Crosshair.drawCurrent(c, 0, 0);
            c.getMatrices().pop();
            Gfx.textC(c, "Preview", px + (ps - Gfx.width("Preview")) / 2, py + ps + 10, 0x80FFFFFF);
        }
        return y;
    }

    // ---------- вкладка Themes ----------
    private int themesTab(DrawContext c, int x, int y, int w) {
        y = header(c, "Accent color", x, y);
        for (int i = 0; i < SWATCH.length; i++) {
            final int col = SWATCH[i];
            swatch(c, "ac" + i, col, x + i * 17, y, !Settings.themeChroma && Settings.themeColor == col, false,
                    () -> { Settings.themeColor = col; Settings.themeChroma = false; }, true);
        }
        y += 22;

        // ползунок оттенка с радужной дорожкой
        float[] hsb = Color.RGBtoHSB((Settings.themeColor >> 16) & 255, (Settings.themeColor >> 8) & 255, Settings.themeColor & 255, null);
        Gfx.textC(c, "Hue", x, y + 5, TXT);
        int by = y + 15;
        for (int k = 0; k < w; k++) Gfx.fill(c, x + k, by, x + k + 1, by + 4, 0xFF000000 | Gfx.hsb(k / (float) w, 0.7f, 1f));
        float ha = anim("hue", hsb[0]);
        Gfx.circle(c, x + (int) (w * ha), by + 2, 5, 0xFFFFFFFF);
        vhit(x - 4, y + 8, w + 8, 20, m -> {
            float t = (float) Math.max(0, Math.min(1, (m - x) / w));
            Settings.themeColor = Gfx.hsb(t, 0.7f, 1f);
            Settings.themeChroma = false;
        }, null, true);
        y += 28;

        y = tgl(c, "rainbow", "Rainbow theme", Settings.themeChroma, v -> Settings.themeChroma = v, x, y, w, true);
        y = sld(c, "glass", "Glass opacity", 0, 100, Settings.glass, v -> Settings.glass = v, x, y + 2, w, true);
        y = sld(c, "radius", "Corner radius", 2, 10, Settings.radius, v -> Settings.radius = v, x, y, w, true);
        y = tgl(c, "anims", "Animations", Settings.anims, v -> Settings.anims = v, x, y, w, true);
        y = sld(c, "aspeed", "Animation speed (%)", 50, 200, Settings.animSpeed, v -> Settings.animSpeed = v, x, y + 2, w, true);

        y = header(c, "Preview", x, y + 2);
        Hud.pill(c, x + 4, y + 2, "Menu opened");
        return y + 26;
    }

    // ---------- вкладка Config ----------
    private int configTab(DrawContext c, int x, int y, int w) {
        int bw = (w - 8) / 3;
        btn(c, "sv", "Save", x, y, bw, 24, false, Settings::save);
        btn(c, "ld", "Load", x + bw + 4, y, bw, 24, false, Settings::load);
        btn(c, "rs", "Reset all", x + (bw + 4) * 2, y, bw, 24, false, Settings::reset);
        Gfx.textC(c, "Config file: config/visuals.properties", x, y + 40, 0x90FFFFFF);
        Gfx.textC(c, "Auto-saved when the menu closes", x, y + 54, 0x90FFFFFF);
        int n = 0;
        for (Module m : Modules.ALL) if (m.enabled) n++;
        Gfx.textC(c, "Enabled modules: " + n + " / " + Modules.ALL.size(), x, y + 74, 0x90FFFFFF);
        return y + 90;
    }

    // ---------- ввод ----------
    private double tmx(double rawX) { return (rawX - width / 2.0) / sc + width / 2.0; }

    private double tmy(double rawY) { return (rawY - slide - height / 2.0) / sc + height / 2.0; }

    @Override
    public boolean mouseClicked(double rawX, double rawY, int button) {
        if (closing) return true;
        double x = tmx(rawX), y = tmy(rawY);
        searchFocus = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.in(x, y)) continue;
            if (button == 0 && h.left() != null) {
                h.left().accept(x);
                if (h.drag()) dragging = h;
            } else if (button == 1 && h.right() != null) {
                h.right().run();
            }
            return true;
        }
        return super.mouseClicked(rawX, rawY, button);
    }

    @Override
    public boolean mouseDragged(double rawX, double rawY, int button, double dx, double dy) {
        if (dragging != null) { dragging.left().accept(tmx(rawX)); return true; }
        return super.mouseDragged(rawX, rawY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double rawX, double rawY, int button) {
        dragging = null;
        return super.mouseReleased(rawX, rawY, button);
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double amount) {
        scrollTarget = Math.max(0f, scrollTarget - (float) amount * 30f);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && chr >= 32 && search.length() < 20) {
            search += chr;
            scroll = 0f; scrollTarget = 0f;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocus) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                scroll = 0f; scrollTarget = 0f;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocus = false;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || VisualsClient.OPEN.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
