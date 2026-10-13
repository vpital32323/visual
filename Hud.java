package com.troizy.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Весь HUD: прицел, верхняя панель, виджеты слева, цель, клавиши и список модулей. */
public final class Hud {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int H = 16, GAP = 5;

    public static float dt = 0.016f;
    private static long lastNs = System.nanoTime();

    // состояние
    private static final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private static boolean lastAttack;
    private static LivingEntity target;
    private static long targetTime;
    private static float hpAnim = 1f;
    private static double lastX, lastZ, speed;
    private static boolean wasDead;
    private static int[] deathPos;
    private static final float[] keyAnim = new float[8];

    private Hud() {}

    // ---------- тик ----------
    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        boolean down = mc.options.attackKey.isPressed();
        long now = System.currentTimeMillis();
        if (down && !lastAttack) clicks.addLast(now);
        lastAttack = down;
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) clicks.pollFirst();

        Entity t = mc.targetedEntity;
        if (t instanceof LivingEntity le && le.isAlive() && t != p) { target = le; targetTime = now; }

        double dx = p.getX() - lastX, dz = p.getZ() - lastZ;
        speed = speed * 0.7 + Math.sqrt(dx * dx + dz * dz) * 20.0 * 0.3;
        lastX = p.getX(); lastZ = p.getZ();

        boolean dead = p.getHealth() <= 0f;
        if (dead && !wasDead) deathPos = new int[]{(int) p.getX(), (int) p.getY(), (int) p.getZ()};
        wasDead = dead;

        if (Modules.SPRINT.enabled && mc.currentScreen == null && p.input.movementForward > 0.8f
                && !p.isSneaking() && !p.horizontalCollision && !p.isUsingItem()
                && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }

    // ---------- главный рендер ----------
    public static void render(DrawContext c) {
        long ns = System.nanoTime();
        dt = Math.min(0.1f, (ns - lastNs) / 1e9f);
        lastNs = ns;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        Gfx.alpha = 1f;

        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        if (Modules.CROSSHAIR.enabled && mc.options.getPerspective().isFirstPerson()) {
            Crosshair.drawCurrent(c, sw / 2, sh / 2);
        }

        topBar(c, mc);
        leftWidgets(c, mc);
        targetHud(c, mc, sw, sh);
        keystrokes(c, mc, sh);
        moduleList(c, sw);
        Gfx.alpha = 1f;
    }

    // ---------- верхняя панель ----------
    private static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e == null ? 0 : e.getLatency();
    }

    /** Цветная стеклянная плашка. Возвращает ширину. */
    public static int pill(DrawContext c, int x, int y, String text) {
        int acc = Settings.accent();
        int w = Gfx.width(text) + 28;
        for (int i = 4; i >= 1; i--) Gfx.rrect(c, x - i, y - i, w + i * 2, H + i * 2, Settings.radius + i, Gfx.rgba(acc, 10));
        Gfx.glass(c, x, y, w, H, Settings.radius, acc, 0x78, Gfx.rgba(acc, 0xD0));
        float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 350.0);
        Gfx.circle(c, x + 9, y + H / 2, 3, Gfx.rgba(0xFFFFFF, (int) (255 * pulse)));
        Gfx.textC(c, text, x + 18, y + H / 2, 0xFFFFFFFF);
        return w;
    }

    private static void topBar(DrawContext c, MinecraftClient mc) {
        Module m = Modules.TOPBAR;
        if (!m.enabled) return;
        boolean menu = mc.currentScreen instanceof GlassScreen;
        String time = LocalTime.now().format(FMT);
        String pillText = menu ? "Menu opened" : Settings.NAME;
        String fps = mc.getCurrentFps() + " fps";
        int ping = ping(mc);
        String png = ping + " ms";

        boolean sT = m.b("time"), sP = m.b("pill"), sF = m.b("fps"), sG = m.b("ping");
        int wTime = sT ? Gfx.width(time) + 16 : 0;
        int wPill = sP ? Gfx.width(pillText) + 28 : 0;
        int wStat = 0;
        if (sF) wStat += Gfx.width(fps) + 8;
        if (sG) wStat += 16 + Gfx.width(png) + 8;
        if (wStat > 0) wStat += 8;

        int count = (wTime > 0 ? 1 : 0) + (wPill > 0 ? 1 : 0) + (wStat > 0 ? 1 : 0);
        if (count == 0) return;
        int total = wTime + wPill + wStat + GAP * (count - 1);
        int x = (c.getScaledWindowWidth() - total) / 2;
        int y = 5;
        int body = Settings.bodyAlpha();

        if (wTime > 0) {
            Gfx.glass(c, x, y, wTime, H, Settings.radius, 0x0B1210, body, 0x38FFFFFF);
            Gfx.textC(c, time, x + 8, y + H / 2, 0xFFFFFFFF);
            x += wTime + GAP;
        }
        if (wPill > 0) {
            pill(c, x, y, pillText);
            x += wPill + GAP;
        }
        if (wStat > 0) {
            Gfx.glass(c, x, y, wStat, H, Settings.radius, 0x0B1210, body, 0x38FFFFFF);
            int px = x + 8;
            if (sF) {
                Gfx.textC(c, fps, px, y + H / 2, 0xFFFFFFFF);
                px += Gfx.width(fps) + 8;
            }
            if (sG) {
                int level = ping < 60 ? 4 : ping < 120 ? 3 : ping < 200 ? 2 : 1;
                int col = level >= 3 ? 0xFF3DDC84 : level == 2 ? 0xFFFFD84D : 0xFFFF4D4D;
                for (int i = 0; i < 4; i++) {
                    int bh = 3 + i * 2;
                    Gfx.rrect(c, px + i * 3, y + 12 - bh, 2, bh, 1, i < level ? col : 0x40FFFFFF);
                }
                Gfx.textC(c, png, px + 16, y + H / 2, 0xFFFFFFFF);
            }
        }
    }

    // ---------- виджеты слева ----------
    private static void panel(DrawContext c, int x, int y, int w, int h) {
        Gfx.glass(c, x, y, w, h, Settings.radius, 0x0B1210, Settings.bodyAlpha(), 0x38FFFFFF);
    }

    private static int count(ClientPlayerEntity p, Item... items) {
        int n = 0;
        List<ItemStack> all = new ArrayList<>(p.getInventory().main);
        all.addAll(p.getInventory().offHand);
        for (ItemStack s : all) for (Item it : items) if (s.isOf(it)) n += s.getCount();
        return n;
    }

    private static void leftWidgets(DrawContext c, MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        int x = 6, y = 28, acc = Settings.accent();

        if (Modules.CPS.enabled) {
            String s = clicks.size() + " CPS";
            int w = Gfx.width(s) + 16;
            panel(c, x, y, w, H);
            Gfx.rrect(c, x + 5, y + 5, 2, H - 10, 1, 0xFF000000 | acc);
            Gfx.textC(c, s, x + 11, y + H / 2, 0xFFFFFFFF);
            y += H + 4;
        }

        if (Modules.INFO.enabled) {
            Module m = Modules.INFO;
            List<String> lines = new ArrayList<>();
            if (m.b("xyz")) lines.add(String.format("XYZ  %d  %d  %d", (int) Math.floor(p.getX()), (int) Math.floor(p.getY()), (int) Math.floor(p.getZ())));
            if (m.b("speed")) lines.add(String.format("%.1f blocks/s", speed));
            if (m.b("dir")) {
                String d = p.getHorizontalFacing().asString();
                lines.add(Character.toUpperCase(d.charAt(0)) + d.substring(1));
            }
            if (!lines.isEmpty()) {
                int w = 0;
                for (String l : lines) w = Math.max(w, Gfx.width(l));
                w += 16;
                int h = lines.size() * 12 + 6;
                panel(c, x, y, w, h);
                for (int i = 0; i < lines.size(); i++) Gfx.textC(c, lines.get(i), x + 8, y + 9 + i * 12, 0xFFFFFFFF);
                y += h + 4;
            }
        }

        if (Modules.DEATH.enabled && deathPos != null) {
            String s = "Died  " + deathPos[0] + "  " + deathPos[1] + "  " + deathPos[2];
            int w = Gfx.width(s) + 16;
            panel(c, x, y, w, H);
            Gfx.rrect(c, x + 5, y + 5, 2, H - 10, 1, 0xFFFF4D4D);
            Gfx.textC(c, s, x + 11, y + H / 2, 0xFFFFFFFF);
            y += H + 4;
        }

        if (Modules.ARMOR.enabled) {
            Module m = Modules.ARMOR;
            List<ItemStack> st = new ArrayList<>();
            for (int i = 3; i >= 0; i--) st.add(p.getInventory().armor.get(i));
            if (m.b("held")) st.add(p.getMainHandStack());
            int rows = 0;
            for (ItemStack s : st) if (!s.isEmpty()) rows++;
            if (rows > 0) {
                int w = m.b("pct") ? 62 : 28;
                int h = rows * 18 + 4;
                panel(c, x, y, w, h);
                int iy = y + 3;
                for (ItemStack s : st) {
                    if (s.isEmpty()) continue;
                    c.drawItem(s, x + 5, iy);
                    if (m.b("pct")) {
                        if (s.isDamageable()) {
                            int pct = 100 - s.getDamage() * 100 / s.getMaxDamage();
                            int col = pct > 50 ? 0xFF3DDC84 : pct > 20 ? 0xFFFFD84D : 0xFFFF4D4D;
                            Gfx.textC(c, pct + "%", x + 26, iy + 8, col);
                        } else if (s.getCount() > 1) {
                            Gfx.textC(c, "x" + s.getCount(), x + 26, iy + 8, 0xFFFFFFFF);
                        }
                    }
                    iy += 18;
                }
                y += h + 4;
            }
        }

        if (Modules.ITEMS.enabled) {
            Module m = Modules.ITEMS;
            List<ItemStack> icons = new ArrayList<>();
            List<Integer> nums = new ArrayList<>();
            if (m.b("totem")) { icons.add(new ItemStack(Items.TOTEM_OF_UNDYING)); nums.add(count(p, Items.TOTEM_OF_UNDYING)); }
            if (m.b("gapple")) { icons.add(new ItemStack(Items.GOLDEN_APPLE)); nums.add(count(p, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)); }
            if (m.b("pearl")) { icons.add(new ItemStack(Items.ENDER_PEARL)); nums.add(count(p, Items.ENDER_PEARL)); }
            if (m.b("arrow")) { icons.add(new ItemStack(Items.ARROW)); nums.add(count(p, Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW)); }
            if (!icons.isEmpty()) {
                int h = icons.size() * 18 + 4;
                panel(c, x, y, 52, h);
                for (int i = 0; i < icons.size(); i++) {
                    c.drawItem(icons.get(i), x + 5, y + 3 + i * 18);
                    int n = nums.get(i);
                    Gfx.textC(c, "x" + n, x + 26, y + 11 + i * 18, n > 0 ? 0xFFFFFFFF : 0x80FFFFFF);
                }
            }
        }
    }

    // ---------- цель ----------
    private static void targetHud(DrawContext c, MinecraftClient mc, int sw, int sh) {
        if (!Modules.TARGET.enabled || target == null) return;
        float hold = Modules.TARGET.i("hold") * 1000f;
        long ms = System.currentTimeMillis() - targetTime;
        float fade = ms < hold ? 1f : 1f - (ms - hold) / 300f;
        if (fade <= 0f || !target.isAlive()) { target = null; return; }

        String name = target.getName().getString();
        boolean showDist = Modules.TARGET.b("dist");
        String dist = String.format("%.1fm", mc.player.distanceTo(target));
        int w = Math.max(120, Gfx.width(name) + (showDist ? Gfx.width(dist) + 8 : 0) + 20);
        int h = 32;
        int x = sw / 2 + 30, y = sh / 2 + 12;
        float ratio = Gfx.clamp01(target.getHealth() / Math.max(1f, target.getMaxHealth()));
        hpAnim = Gfx.smooth(hpAnim, ratio, dt, 10f);

        Gfx.alpha = fade;
        panel(c, x, y, w, h);
        Gfx.textC(c, name, x + 8, y + 10, 0xFFFFFFFF);
        if (showDist) Gfx.textRight(c, dist, x + w - 8, y + 6, 0xA0FFFFFF);
        int bx = x + 8, by = y + 20, bw = w - 16 - 34;
        Gfx.rrect(c, bx, by, bw, 5, 2, 0x40FFFFFF);
        int col = Gfx.mix(0xFFFF4D4D, 0xFF3DDC84, hpAnim);
        Gfx.rrect(c, bx, by, Math.max(3, (int) (bw * hpAnim)), 5, 2, col);
        Gfx.textC(c, String.format("%.1f", target.getHealth()), bx + bw + 6, by + 3, 0xFFFFFFFF);
        Gfx.alpha = 1f;
    }

    // ---------- клавиши ----------
    private static void key(DrawContext c, int idx, boolean down, String label, int x, int y, int w, int h) {
        keyAnim[idx] = Gfx.smooth(keyAnim[idx], down ? 1 : 0, dt, 18f);
        float k = keyAnim[idx];
        Gfx.glass(c, x, y, w, h, 4, 0x0B1210, Settings.bodyAlpha(), 0x38FFFFFF);
        if (k > 0.02f) Gfx.rrect(c, x + 1, y + 1, w - 2, h - 2, 4, Gfx.rgba(Settings.accent(), (int) (150 * k)));
        if (label != null) Gfx.textC(c, label, x + (w - Gfx.width(label)) / 2, y + h / 2, 0xFFFFFFFF);
    }

    private static void keystrokes(DrawContext c, MinecraftClient mc, int sh) {
        if (!Modules.KEYS.enabled) return;
        int x = 6, y = sh - 76;
        key(c, 0, mc.options.forwardKey.isPressed(), "W", x + 20, y, 18, 18);
        key(c, 1, mc.options.leftKey.isPressed(), "A", x, y + 20, 18, 18);
        key(c, 2, mc.options.backKey.isPressed(), "S", x + 20, y + 20, 18, 18);
        key(c, 3, mc.options.rightKey.isPressed(), "D", x + 40, y + 20, 18, 18);
        key(c, 4, mc.options.jumpKey.isPressed(), null, x, y + 40, 58, 10);
        key(c, 5, mc.options.attackKey.isPressed(), "LMB", x, y + 52, 28, 14);
        key(c, 6, mc.options.useKey.isPressed(), "RMB", x + 30, y + 52, 28, 14);
    }

    // ---------- список включённых модулей ----------
    private static void moduleList(DrawContext c, int sw) {
        Module lm = Modules.LIST;
        for (Module m : Modules.ALL) m.listAnim = Gfx.smooth(m.listAnim, m.enabled ? 1 : 0, dt, 10f);
        if (!lm.enabled) return;

        float sc = lm.i("scale") / 100f;
        boolean right = lm.b("right"), bg = lm.b("bg"), stripe = lm.b("stripe");
        List<Module> es = new ArrayList<>();
        for (Module m : Modules.ALL) if (m != lm && m.listAnim > 0.02f) es.add(m);
        es.sort((a, b) -> Integer.compare(Gfx.width(b.name), Gfx.width(a.name)));

        c.getMatrices().push();
        c.getMatrices().scale(sc, sc, 1f);
        int swS = (int) (sw / sc);
        int y = 6, bh = 13;
        boolean themed = lm.get("color").v == -1 && Settings.themeChroma;
        float t = (System.currentTimeMillis() % 6000L) / 6000f;
        for (int i = 0; i < es.size(); i++) {
            Module m = es.get(i);
            float e = Gfx.easeOutCubic(m.listAnim);
            int tw = Gfx.width(m.name), bw = tw + 12;
            int xo = (int) ((1f - e) * (bw + 8));
            int x = right ? swS - 4 - bw + xo : 4 - xo;
            int col = themed ? Gfx.hsb((t + i * 0.04f) % 1f, 0.65f, 1f) : lm.color("color");
            Gfx.alpha = e;
            if (bg) Gfx.rrect(c, x, y, bw, bh, 3, 0x96000000);
            if (stripe) Gfx.rrect(c, right ? x + bw - 2 : x, y + 2, 2, bh - 4, 1, 0xFF000000 | col);
            Gfx.textC(c, m.name, x + (right ? 5 : 7), y + bh / 2, 0xFF000000 | col);
            y += (int) ((bh + 2) * e);
        }
        Gfx.alpha = 1f;
        c.getMatrices().pop();
    }
}
