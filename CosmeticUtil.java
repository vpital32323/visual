package com.troizy.visuals;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

final class CosmeticUtil {
    private CosmeticUtil() {}

    static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, int rgb, int a) {
        vc.vertex(m, x, y, z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Math.max(0, Math.min(255, a))).next();
    }

    static void quad(VertexConsumer vc, Matrix4f m,
                     float x1, float y1, float z1, int c1, int a1,
                     float x2, float y2, float z2, int c2, int a2,
                     float x3, float y3, float z3, int c3, int a3,
                     float x4, float y4, float z4, int c4, int a4) {
        v(vc, m, x1, y1, z1, c1, a1);
        v(vc, m, x2, y2, z2, c2, a2);
        v(vc, m, x3, y3, z3, c3, a3);
        v(vc, m, x4, y4, z4, c4, a4);
    }
}
