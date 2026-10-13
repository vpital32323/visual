package com.troizy.visuals;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Settings {
    /** Название твоих визуалов — меняется здесь в одном месте */
    public static String NAME = "Troizy Visuals";

    public static int themeColor = 0xB05CFF;
    public static boolean themeChroma = false;
    public static boolean anims = true;
    public static int animSpeed = 100; // 50..200 %
    public static int radius = 6;
    public static int glass = 55; // 0..100

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("visuals.properties");
    }

    public static int bodyAlpha() { return 0x30 + (int) (glass / 100f * 0x70); }

    /** Главный цвет темы (меню, шляпа, лужи, плащ по умолчанию). */
    public static int accent() {
        if (themeChroma) return Gfx.hsb((System.currentTimeMillis() % 6000L) / 6000f, 0.65f, 1f);
        return themeColor;
    }

    public static int resolve(int v) { return v == -1 ? accent() : v; }

    public static void reset() {
        themeColor = 0xB05CFF; themeChroma = false; anims = true; animSpeed = 100; radius = 6; glass = 55;
        for (Module m : Modules.ALL) m.reset();
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("themeColor", "" + themeColor);
        p.setProperty("themeChroma", "" + themeChroma);
        p.setProperty("anims", "" + anims);
        p.setProperty("animSpeed", "" + animSpeed);
        p.setProperty("radius", "" + radius);
        p.setProperty("glass", "" + glass);
        for (Module m : Modules.ALL) {
            p.setProperty("m." + m.id, "" + m.enabled);
            for (Setting s : m.settings) p.setProperty("m." + m.id + "." + s.key, "" + s.v);
        }
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "Troizy Visuals");
        } catch (IOException ignored) {}
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f)) {
            p.load(r);
        } catch (IOException e) { return; }
        themeColor = i(p, "themeColor", themeColor);
        themeChroma = b(p, "themeChroma", themeChroma);
        anims = b(p, "anims", anims);
        animSpeed = i(p, "animSpeed", animSpeed);
        radius = i(p, "radius", radius);
        glass = i(p, "glass", glass);
        for (Module m : Modules.ALL) {
            m.enabled = b(p, "m." + m.id, m.enabled);
            for (Setting s : m.settings) s.v = i(p, "m." + m.id + "." + s.key, s.v);
        }
    }

    private static boolean b(Properties p, String k, boolean d) { return Boolean.parseBoolean(p.getProperty(k, "" + d)); }

    private static int i(Properties p, String k, int d) {
        try { return Integer.parseInt(p.getProperty(k, "" + d)); } catch (NumberFormatException e) { return d; }
    }
}
