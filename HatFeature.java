package com.troizy.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/** Круглая китайская шляпа над головой. Цвет берётся из темы или из настройки. */
public class HatFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final int N = 28;

    public HatFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
        super(ctx);
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity p,
                       float limbAngle, float limbDistance, float tickDelta, float animProgress, float headYaw, float headPitch) {
        Module m = Modules.HAT;
        if (!m.enabled || p.isInvisible()) return;
        if (p != MinecraftClient.getInstance().player && !m.b("others")) return;

        float s = m.i("size") / 100f;
        int base = m.color("color");
        boolean spin = m.b("spin");
        float t = p.age + tickDelta;

        ms.push();
        getContextModel().head.rotate(ms);
        Matrix4f mat = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());

        float yb = -0.52f;                // низ шляпы (чуть выше макушки; в модели ось Y вниз)
        float rad = 0.58f * s, height = 0.3f * s;
        float apexY = yb - height;
        int apexCol = Gfx.mix(0xFF000000 | base, 0xFFFFFFFF, 0.35f) & 0xFFFFFF;

        for (int i = 0; i < N; i++) {
            double a0 = Math.PI * 2 * i / N, a1 = Math.PI * 2 * (i + 1) / N;
            float shade = 0.62f + 0.38f * (float) Math.sin(i * 0.9 + (spin ? t * 0.12 : 0));
            int col = Gfx.shade(base, shade);
            float x0 = (float) Math.cos(a0) * rad, z0 = (float) Math.sin(a0) * rad;
            float x1 = (float) Math.cos(a1) * rad, z1 = (float) Math.sin(a1) * rad;
            // боковая грань конуса
            CosmeticUtil.quad(vc, mat,
                    x0, yb, z0, col, 235,
                    x1, yb, z1, col, 235,
                    0f, apexY, 0f, apexCol, 235,
                    0f, apexY, 0f, apexCol, 235);
            // светлая кайма по краю
            float ri = rad - 0.035f * s;
            int edge = Gfx.mix(0xFF000000 | base, 0xFFFFFFFF, 0.5f) & 0xFFFFFF;
            CosmeticUtil.quad(vc, mat,
                    (float) Math.cos(a0) * ri, yb, (float) Math.sin(a0) * ri, edge, 250,
                    x0, yb, z0, edge, 250,
                    x1, yb, z1, edge, 250,
                    (float) Math.cos(a1) * ri, yb, (float) Math.sin(a1) * ri, edge, 250);
        }
        ms.pop();
    }
}
