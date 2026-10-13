package com.troizy.visuals;

import net.minecraft.client.MinecraftClient;

public final class Zoom {
    private static float factor = 1f;
    private static long last = System.nanoTime();

    /** Вызывается из миксина каждый кадр: возвращает текущий коэффициент зума. */
    public static float factor() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        MinecraftClient mc = MinecraftClient.getInstance();
        Module m = Modules.ZOOM;
        boolean on = m.enabled && VisualsClient.ZOOM != null && VisualsClient.ZOOM.isPressed()
                && mc.currentScreen == null;
        float target = on ? m.i("level") : 1f;
        if (m.b("smooth")) factor += (target - factor) * (1f - (float) Math.exp(-dt * 14f));
        else factor = target;
        return factor;
    }
}
