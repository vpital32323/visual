package com.troizy.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;

/** Помощники для отрисовки: скруглённые прямоугольники, стекло, текст шрифтом Poppins Bold. */
public final class Gfx {
    /** Глобальная прозрачность (для анимаций) */
    public static float alpha = 1f;
    private static final Style UI = Style.EMPTY.withFont(new Identifier("visuals", "ui"));

    private static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

    // ---------- цвета и анимация ----------
    public static int a(int c) {
        int al = (int) (((c >>> 24) & 255) * alpha);
        return (al << 24) | (c & 0xFFFFFF);
    }

    public static int rgba(int rgb, int al) { return (Math.max(0, Math.min(255, al)) << 24) | (rgb & 0xFFFFFF); }

    public static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = 0;
        for (int s = 24; s >= 0; s -= 8) {
            int x = (c1 >>> s) & 255, y = (c2 >>> s) & 255;
            r |= ((int) (x + (y - x) * t) & 255) << s;
        }
        return r;
    }

    public static int hsb(float h, float s, float b) { return Color.HSBtoRGB(h, s, b) & 0xFFFFFF; }

    public static int shade(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * f));
        int b = Math.min(255, (int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    public static float smooth(float cur, float target, float dt, float speed) {
        float k = 1f - (float) Math.exp(-dt * speed * Settings.animSpeed / 100f);
        return Settings.anims ? cur + (target - cur) * k : target;
    }

    public static float clamp01(float t) { return Math.max(0f, Math.min(1f, t)); }
    public static float easeOutCubic(float t) { t = clamp01(t); float u = 1 - t; return 1 - u * u * u; }
    public static float easeInCubic(float t) { t = clamp01(t); return t * t * t; }
    public static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.4f, c3 = c1 + 1f, u = t - 1f;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    // ---------- формы ----------
    private static float inset(int r, int j) {
        if (j >= r) return 0f;
        double d = r - j - 0.5;
        return (float) (r - Math.sqrt(Math.max(0, r * r - d * d)));
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) {
        rrectV(c, x, y, w, h, r, col, col);
    }

    /** Скруглённый прямоугольник с вертикальным градиентом и сглаженными углами. */
    public static void rrectV(DrawContext c, int x, int y, int w, int h, int r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int col = a(top == bottom ? top : mix(top, bottom, h <= 1 ? 0 : i / (float) (h - 1)));
            int al = col >>> 24;
            if (al == 0) continue;
            float dx = inset(r, Math.min(i, h - 1 - i));
            int fx = (int) Math.floor(dx);
            float frac = dx - fx;
            if (frac < 0.03f) {
                c.fill(x + fx, y + i, x + w - fx, y + i + 1, col);
            } else {
                c.fill(x + fx + 1, y + i, x + w - fx - 1, y + i + 1, col);
                int ea = Math.max(0, Math.min(255, (int) (al * (1f - frac))));
                int ec = (ea << 24) | (col & 0xFFFFFF);
                c.fill(x + fx, y + i, x + fx + 1, y + i + 1, ec);
                c.fill(x + w - fx - 1, y + i, x + w - fx, y + i + 1, ec);
            }
        }
    }

    public static void outline(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        col = a(col);
        if ((col >>> 24) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int j = Math.min(i, h - 1 - i);
            int in = (int) Math.round(inset(r, j));
            if (j == 0) { c.fill(x + in, y + i, x + w - in, y + i + 1, col); continue; }
            int out = Math.max(in + 1, (int) Math.round(inset(r, j - 1)));
            c.fill(x + in, y + i, x + out, y + i + 1, col);
            c.fill(x + w - out, y + i, x + w - in, y + i + 1, col);
        }
    }

    public static void circle(DrawContext c, int cx, int cy, int r, int col) {
        rrect(c, cx - r, cy - r, r * 2, r * 2, r, col);
    }

    public static void fill(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        c.fill(x1, y1, x2, y2, a(col));
    }

    /** Стеклянная панель: мягкая тень, градиентное тело, блик сверху, светлая обводка. */
    public static void glass(DrawContext c, int x, int y, int w, int h, int r, int tint, int bodyAlpha, int outlineCol) {
        for (int i = 4; i >= 1; i--) rrect(c, x - i, y - i, w + i * 2, h + i * 2, r + i, rgba(0x000000, 14 / i + 3));
        rrectV(c, x, y, w, h, r, rgba(Gfx.mix(0xFF000000 | tint, 0xFFFFFFFF, 0.06f), bodyAlpha + 10),
                rgba(tint, bodyAlpha));
        rrectV(c, x + 1, y + 1, w - 2, Math.max(2, h / 3), r, 0x1CFFFFFF, 0x00FFFFFF);
        outline(c, x, y, w, h, r, outlineCol);
    }

    // ---------- текст ----------
    public static int width(String s) { return tr().getWidth(Text.literal(s).setStyle(UI)); }

    public static void text(DrawContext c, String s, int x, int y, int col) {
        col = a(col);
        if ((col >>> 24) < 4) return;
        c.drawText(tr(), Text.literal(s).setStyle(UI), x, y, col, false);
    }

    /** Текст, вертикально отцентрованный относительно cy. */
    public static void textC(DrawContext c, String s, int x, int cy, int col) { text(c, s, x, cy - 4, col); }

    public static void textRight(DrawContext c, String s, int right, int y, int col) { text(c, s, right - width(s), y, col); }

    public static void textScaled(DrawContext c, String s, int x, int y, int col, float sc) {
        c.getMatrices().push();
        c.getMatrices().translate(x, y, 0);
        c.getMatrices().scale(sc, sc, 1f);
        text(c, s, 0, 0, col);
        c.getMatrices().pop();
    }
}
