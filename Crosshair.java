package com.troizy.visuals;

import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public final class Crosshair {
    public static final int STYLES = 6;

    public static int currentColor() {
        Module m = Modules.CROSSHAIR;
        if (m.b("chroma")) return Color.HSBtoRGB((System.currentTimeMillis() % 3000L) / 3000f, 0.75f, 1f) & 0xFFFFFF;
        return m.color("color");
    }

    private static void f(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        c.fill(x1, y1, x2, y2, Gfx.a(col));
    }

    /** 0 плюс, 1 плюс+точка, 2 точка, 3 круг, 4 круг+точка, 5 стрелка */
    public static void draw(DrawContext c, int cx, int cy, int style, int rgb, int size, int gap, int t) {
        int col = 0xFF000000 | (rgb & 0xFFFFFF);
        int l = -(t / 2);
        switch (style) {
            case 0, 1 -> {
                f(c, cx + l, cy - gap - size, cx + l + t, cy - gap, col);
                f(c, cx + l, cy + 1 + gap, cx + l + t, cy + 1 + gap + size, col);
                f(c, cx - gap - size, cy + l, cx - gap, cy + l + t, col);
                f(c, cx + 1 + gap, cy + l, cx + 1 + gap + size, cy + l + t, col);
                if (style == 1) f(c, cx, cy, cx + 1, cy + 1, col);
            }
            case 2 -> f(c, cx - t, cy - t, cx + t + 1, cy + t + 1, col);
            case 3, 4 -> {
                int r = gap + size / 2 + 2;
                int steps = Math.max(24, r * 8);
                for (int i = 0; i < steps; i++) {
                    double ang = Math.PI * 2 * i / steps;
                    int px = cx + (int) Math.round(r * Math.cos(ang));
                    int py = cy + (int) Math.round(r * Math.sin(ang));
                    f(c, px, py, px + t, py + t, col);
                }
                if (style == 4) f(c, cx, cy, cx + 1, cy + 1, col);
            }
            case 5 -> {
                int top = cy - gap - size / 2;
                for (int i = 0; i <= size; i++) {
                    f(c, cx - i, top + i, cx - i + t, top + i + t, col);
                    f(c, cx + i - t + 1, top + i, cx + i + 1, top + i + t, col);
                }
            }
            default -> {}
        }
    }

    public static void drawCurrent(DrawContext c, int cx, int cy) {
        Module m = Modules.CROSSHAIR;
        draw(c, cx, cy, m.i("style"), currentColor(), m.i("size"), m.i("gap"), m.i("thick"));
    }
}
