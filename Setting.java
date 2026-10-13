package com.troizy.visuals;

/** Одна настройка модуля: переключатель, число или цвет. Всё хранится в int. */
public final class Setting {
    public enum Type { BOOL, NUM, COLOR }

    public final String key, label;
    public final Type type;
    public final int min, max, def;
    public int v;

    public Setting(String key, String label, Type type, int min, int max, int def) {
        this.key = key; this.label = label; this.type = type;
        this.min = min; this.max = max; this.def = def; this.v = def;
    }

    public boolean b() { return v != 0; }

    /** Цвет с учётом темы: -1 значит "цвет темы". */
    public int color() { return Settings.resolve(v); }
}
