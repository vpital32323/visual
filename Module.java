package com.troizy.visuals;

import java.util.ArrayList;
import java.util.List;

public final class Module {
    public enum Cat {
        COMBAT("Combat"), RENDER("Render"), MISC("Misc"), COSMETICS("Cosmetics"), CROSSHAIR("Crosshair");
        public final String title;
        Cat(String t) { title = t; }
    }

    public final String id, name, desc;
    public final Cat cat;
    public final boolean def;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    // анимации (меню и список)
    public float expand, hov, sw, listAnim;

    public Module(String id, String name, String desc, Cat cat, boolean def) {
        this.id = id; this.name = name; this.desc = desc; this.cat = cat;
        this.def = def; this.enabled = def;
    }

    public Module bool(String key, String label, boolean d) {
        settings.add(new Setting(key, label, Setting.Type.BOOL, 0, 1, d ? 1 : 0));
        return this;
    }

    public Module num(String key, String label, int min, int max, int d) {
        settings.add(new Setting(key, label, Setting.Type.NUM, min, max, d));
        return this;
    }

    /** Цвет, по умолчанию берётся из темы меню. */
    public Module color(String key, String label) { return color(key, label, -1); }

    public Module color(String key, String label, int d) {
        settings.add(new Setting(key, label, Setting.Type.COLOR, 0, 0, d));
        return this;
    }

    public Setting get(String key) {
        for (Setting s : settings) if (s.key.equals(key)) return s;
        throw new IllegalArgumentException("No setting " + key + " in " + id);
    }

    public boolean b(String key) { return get(key).b(); }
    public int i(String key) { return get(key).v; }
    public int color(String key) { return get(key).color(); }

    public void reset() {
        enabled = def;
        for (Setting s : settings) s.v = s.def;
    }
}
