package com.troizy.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class VisualsClient implements ClientModInitializer {
    public static KeyBinding OPEN, ZOOM;

    @Override
    public void onInitializeClient() {
        Settings.load();
        OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visuals.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.visuals"));
        ZOOM = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visuals.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.visuals"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new GlassScreen());
            }
            Hud.tick(mc);
            Ripples.tick(mc);
        });

        HudRenderCallback.EVENT.register((ctx, tickDelta) -> Hud.render(ctx));

        WorldRenderEvents.AFTER_TRANSLUCENT.register(Ripples::render);

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof PlayerEntityRenderer per) {
                helper.register(new HatFeature(per));
                helper.register(new CapeFeature(per));
            }
        });
    }
}
