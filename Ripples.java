package com.troizy.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Круги-лужи под ногами и подсветка игроков кольцом. Рисуется в мире, с проверкой глубины (сквозь стены не видно). */
public final class Ripples {
    private static final class Ring {
        final double x, y, z;
        int age;
        final int life;
        final float max;
        Ring(double x, double y, double z, int life, float max) { this.x = x; this.y = y; this.z = z; this.life = life; this.max = max; }
    }

    private static final List<Ring> RINGS = new ArrayList<>();
    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    private static final int SEG = 40;

    private Ripples() {}

    public static void tick(MinecraftClient mc) {
        for (Ring r : RINGS) r.age++;
        RINGS.removeIf(r -> r.age >= r.life);
        if (mc.world == null || mc.player == null) { RINGS.clear(); COOLDOWN.clear(); return; }
        Module m = Modules.RINGS;
        if (!m.enabled) return;
        int interval = m.i("interval"), life = m.i("life");
        float max = m.i("size") / 10f;
        boolean others = m.b("others");
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !others) continue;
            int cd = COOLDOWN.getOrDefault(p.getUuid(), 0) - 1;
            double dx = p.getX() - p.prevX, dz = p.getZ() - p.prevZ;
            boolean moving = dx * dx + dz * dz > 0.0004;
            if (moving && p.isOnGround() && cd <= 0) {
                RINGS.add(new Ring(p.getX(), p.getY() + 0.02, p.getZ(), life, max));
                cd = interval;
            }
            COOLDOWN.put(p.getUuid(), cd);
        }
    }

    private static void v(BufferBuilder bb, Matrix4f m, float x, float z, int rgb, int a) {
        bb.vertex(m, x, 0f, z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Math.max(0, Math.min(255, a))).next();
    }

    /** Кольцо с мягким внутренним краем: от r0 (прозрачно) до r1 (alpha a1) + тонкая чёткая кайма. */
    private static void ring(BufferBuilder bb, Matrix4f m, float r0, float r1, float edge, int rgb, int a1) {
        for (int i = 0; i < SEG; i++) {
            double t0 = Math.PI * 2 * i / SEG, t1 = Math.PI * 2 * (i + 1) / SEG;
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0), c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            // мягкая часть
            v(bb, m, c0 * r0, s0 * r0, rgb, 0);
            v(bb, m, c0 * r1, s0 * r1, rgb, (int) (a1 * 0.55f));
            v(bb, m, c1 * r1, s1 * r1, rgb, (int) (a1 * 0.55f));
            v(bb, m, c1 * r0, s1 * r0, rgb, 0);
            // чёткая кайма
            float r2 = r1 + edge;
            v(bb, m, c0 * r1, s0 * r1, rgb, a1);
            v(bb, m, c0 * r2, s0 * r2, rgb, a1);
            v(bb, m, c1 * r2, s1 * r2, rgb, a1);
            v(bb, m, c1 * r1, s1 * r1, rgb, a1);
        }
    }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        boolean hl = Modules.HIGHLIGHT.enabled;
        if (RINGS.isEmpty() && !hl) return;

        MatrixStack ms = ctx.matrixStack();
        if (ms == null) return;
        Vec3d cam = ctx.camera().getPos();
        float td = ctx.tickDelta();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1f, -10f);

        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        // невидимый квад, чтобы буфер никогда не был пустым
        for (int k = 0; k < 4; k++) bb.vertex(0, 0, 0).color(0, 0, 0, 0).next();

        int ringCol = Modules.RINGS.color("color");
        for (Ring r : RINGS) {
            float p = (r.age + td) / r.life;
            float rad = r.max * Gfx.easeOutCubic(p);
            int a = (int) (190 * (1f - p) * (1f - p) + 20 * (1f - p));
            ms.push();
            ms.translate(r.x - cam.x, r.y - cam.y, r.z - cam.z);
            ring(bb, ms.peek().getPositionMatrix(), rad * 0.45f, rad, 0.05f + 0.05f * (1f - p), ringCol, a);
            ms.pop();
        }

        if (hl) {
            Module m = Modules.HIGHLIGHT;
            float rad = m.i("radius") / 10f;
            double range = m.i("range");
            int col = m.color("color");
            float pulse = m.b("pulse") ? 0.75f + 0.25f * (float) Math.sin((mc.world.getTime() + td) * 0.18) : 1f;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player || p.isInvisible() || p.isSpectator()) continue;
                if (p.squaredDistanceTo(mc.player) > range * range) continue;
                Vec3d pos = p.getLerpedPos(td);
                ms.push();
                ms.translate(pos.x - cam.x, pos.y + 0.03 - cam.y, pos.z - cam.z);
                ring(bb, ms.peek().getPositionMatrix(), rad * 0.4f, rad * pulse, 0.06f, col, 210);
                ms.pop();
                }
        }

        BufferRenderer.drawWithGlobalProgram(bb.end());

        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
