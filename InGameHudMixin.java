package com.troizy.visuals.mixin;

import com.troizy.visuals.Modules;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    // Прячем ванильный прицел, когда включён наш
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void visuals$hideVanillaCrosshair(DrawContext context, CallbackInfo ci) {
        if (Modules.CROSSHAIR.enabled) ci.cancel();
    }
}
