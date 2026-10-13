package com.troizy.visuals;

import com.troizy.visuals.Module.Cat;

import java.util.ArrayList;
import java.util.List;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    private static Module reg(Module m) { ALL.add(m); return m; }

    // ---------- Combat ----------
    public static final Module TARGET = reg(new Module("targethud", "Target HUD",
            "Health and distance of the target you look at", Cat.COMBAT, true)
            .num("hold", "Hold time (sec)", 1, 10, 3).bool("dist", "Show distance", true));
    public static final Module CPS = reg(new Module("cps", "CPS Counter",
            "Clicks per second of the attack button", Cat.COMBAT, true));
    public static final Module ARMOR = reg(new Module("armorhud", "Armor HUD",
            "Armor and held item with durability", Cat.COMBAT, false)
            .bool("held", "Show held item", true).bool("pct", "Show durability %", true));
    public static final Module ITEMS = reg(new Module("itemcount", "Item Counter",
            "Counts totems, gapples, pearls and arrows", Cat.COMBAT, false)
            .bool("totem", "Totems", true).bool("gapple", "Golden apples", true)
            .bool("pearl", "Ender pearls", true).bool("arrow", "Arrows", false));
    public static final Module SPRINT = reg(new Module("sprint", "Toggle Sprint",
            "Sprints automatically while you walk forward", Cat.COMBAT, false));

    // ---------- Render ----------
    public static final Module RINGS = reg(new Module("rings", "Puddles",
            "Rings under your feet that grow and fade like puddles", Cat.RENDER, true)
            .color("color", "Color").num("size", "Size (x0.1 blocks)", 6, 40, 14)
            .num("life", "Lifetime (ticks)", 10, 60, 28).num("interval", "Spawn interval (ticks)", 2, 20, 6)
            .bool("others", "Also on other players", false));
    public static final Module HIGHLIGHT = reg(new Module("highlight", "Player Highlight",
            "Glowing pulsing ring under other players", Cat.RENDER, false)
            .color("color", "Color").num("radius", "Radius (x0.1 blocks)", 6, 20, 10)
            .num("range", "Range (blocks)", 8, 64, 40).bool("pulse", "Pulse", true));
    public static final Module ZOOM = reg(new Module("zoom", "Zoom",
            "Hold the zoom key (C by default) to zoom in smoothly", Cat.RENDER, true)
            .num("level", "Zoom level (x)", 2, 10, 4).bool("smooth", "Smooth", true));
    public static final Module CROSSHAIR = reg(new Module("crosshair", "Crosshair",
            "Custom crosshair", Cat.CROSSHAIR, true)
            .num("style", "Style", 0, 5, 0).color("color", "Color", 0xFFFFFF).bool("chroma", "Chroma", false)
            .num("size", "Size", 1, 12, 5).num("gap", "Gap", 0, 10, 3).num("thick", "Thickness", 1, 3, 1));

    // ---------- Misc ----------
    public static final Module TOPBAR = reg(new Module("topbar", "Top Bar",
            "Glass bar with time, name, FPS and ping", Cat.MISC, true)
            .bool("time", "Show time", true).bool("pill", "Show name pill", true)
            .bool("fps", "Show FPS", true).bool("ping", "Show ping", true));
    public static final Module LIST = reg(new Module("modlist", "Module List",
            "Shows enabled modules on the side of the screen", Cat.MISC, true)
            .bool("right", "Right side", true).num("scale", "Size (%)", 60, 160, 100)
            .color("color", "Color").bool("bg", "Background", true).bool("stripe", "Edge stripe", true));
    public static final Module INFO = reg(new Module("info", "Info HUD",
            "Coordinates, speed and direction", Cat.MISC, false)
            .bool("xyz", "Coordinates", true).bool("speed", "Speed", true).bool("dir", "Direction", true));
    public static final Module KEYS = reg(new Module("keys", "Keystrokes",
            "WASD, jump and mouse buttons on screen", Cat.MISC, false));
    public static final Module DEATH = reg(new Module("death", "Death Point",
            "Remembers where you died", Cat.MISC, false));

    // ---------- Cosmetics ----------
    public static final Module HAT = reg(new Module("hat", "China Hat",
            "Round Chinese hat above the head", Cat.COSMETICS, true)
            .color("color", "Color").num("size", "Size (%)", 50, 160, 100)
            .bool("spin", "Animated shading", true).bool("others", "Also on other players", false));
    public static final Module CAPE = reg(new Module("cape", "Animated Cape",
            "Flowing glowing cape", Cat.COSMETICS, true)
            .color("color", "Color").num("length", "Length (%)", 60, 140, 100)
            .num("wave", "Wave strength", 0, 100, 55).bool("flow", "Color flow", true)
            .bool("others", "Also on other players", false));

    private Modules() {}
}
