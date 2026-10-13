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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

/** Анимированный плащ: колышется, отлетает назад при беге и переливается цветом. */
public class CapeFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final int COLS = 6, ROWS = 12;

    public CapeFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
        super(ctx);
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity p,
                       float limbAngle, float limbDistance, float tickDelta, float animProgress, float headYaw, float headPitch) {
        Module m = Modules.CAPE;
        if (!m.enabled || p.isInvisible()) return;
        if (p != MinecraftClient.getInstance().player && !m.b("others")) return;

        float len = 0.95f * m.i("length") / 100f;
        float waveK = m.i("wave") / 100f;
        boolean flow = m.b("flow");
        int base = m.color("color");
        float[] hsb = Color.RGBtoHSB((base >> 16) & 255, (base >> 8) & 255, base & 255, null);
        float t = p.age + tickDelta;

        // скорость вперёд относительно корпуса
        Vec3d vel = p.getVelocity();
        float yaw = MathHelper.lerpAngleDegrees(tickDelta, p.prevBodyYaw, p.bodyYaw) * 0.017453292f;
        float fwd = (float) (vel.x * -MathHelper.sin(yaw) + vel.z * MathHelper.cos(yaw));
        float spd = (float) Math.min(0.4, vel.horizontalLength());
        float ang = (float) Math.toRadians(Math.min(55f, Math.max(0f, fwd * 130f)) + (p.isSneaking() ? 12f : 4f)
                + 3f * (float) Math.sin(t * 0.07));

        ms.push();
        getContextModel().body.rotate(ms);
        Matrix4f mat = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());

        float[][] px = new float[COLS + 1][ROWS + 1], py = new float[COLS + 1][ROWS + 1], pz = new float[COLS + 1][ROWS + 1];
        int[][] pc = new int[COLS + 1][ROWS + 1];
        int[][] pa = new int[COLS + 1][ROWS + 1];
        float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
        for (int i = 0; i <= COLS; i++) {
            float u = i / (float) COLS;
            for (int j = 0; j <= ROWS; j++) {
                float v = j / (float) ROWS;
                float amp = waveK * (0.35f + spd * 4f) * v;
                float wave = (float) Math.sin(t * 0.22 + v * 4.5 + u * 3.0) * 0.05f * amp;
                float sway = (float) Math.sin(t * 0.11 + v * 3.0) * 0.03f * v * (0.4f + waveK);
                px[i][j] = (u - 0.5f) * 0.62f + sway;
                py[i][j] = v * len * ca;
                pz[i][j] = 0.17f + v * len * sa + wave;
                float hue = flow ? (hsb[0] + v * 0.16f + t * 0.004f + u * 0.03f) % 1f : hsb[0];
                float br = (0.55f + 0.45f * (1f - v * 0.4f)) * hsb[2];
                pc[i][j] = Gfx.hsb(hue, Math.max(0.35f, hsb[1]), Math.min(1f, br));
                pa[i][j] = (int) (240 - 110 * v);
            }
        }
        for (int i = 0; i < COLS; i++) {
            for (int j = 0; j < ROWS; j++) {
                CosmeticUtil.quad(vc, mat,
                        px[i][j], py[i][j], pz[i][j], pc[i][j], pa[i][j],
                        px[i + 1][j], py[i + 1][j], pz[i + 1][j], pc[i + 1][j], pa[i + 1][j],
                        px[i + 1][j + 1], py[i + 1][j + 1], pz[i + 1][j + 1], pc[i + 1][j + 1], pa[i + 1][j + 1],
                        px[i][j + 1], py[i][j + 1], pz[i][j + 1], pc[i][j + 1], pa[i][j + 1]);
            }
        }
        ms.pop();
    }
}
