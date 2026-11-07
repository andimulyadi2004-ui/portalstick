package com.example.portalstick;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Render ringan: 1 bidang bertekstur per portal (tanpa render ulang dunia). */
public class ClientRender {
    static PortalData data = new PortalData();
    static final ResourceLocation TEX = new ResourceLocation(PortalStickMod.ID, "textures/portal/portal.png");

    public static void set(CompoundNBT t) {
        PortalData d = new PortalData();
        d.read(t);
        data = d;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.world == null || data.pairs.isEmpty()) return;
        MatrixStack ms = e.getMatrixStack();
        Vector3d cam = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
        Matrix4f m = ms.getLast().getMatrix();
        float a = 0.6f + 0.15f * (float) Math.sin((mc.world.getGameTime() + e.getPartialTicks()) * 0.1);

        mc.getTextureManager().bindTexture(TEX);
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(7, DefaultVertexFormats.POSITION_COLOR_TEX);
        boolean any = false;
        for (PortalData.Pair p : data.pairs) {
            for (PortalData.Portal q : new PortalData.Portal[]{p.a, p.b}) {
                if (q.dim != mc.world.getDimensionKey()) continue;
                any = true;
                double y0 = q.min.getY(), y1 = q.max.getY() + 1;
                if (q.alongX()) {
                    double x0 = q.min.getX(), x1 = q.max.getX() + 1, z = q.min.getZ() + 0.5;
                    vtx(b, m, cam, x0, y0, z, a, 0, 1);
                    vtx(b, m, cam, x1, y0, z, a, 1, 1);
                    vtx(b, m, cam, x1, y1, z, a, 1, 0);
                    vtx(b, m, cam, x0, y1, z, a, 0, 0);
                } else {
                    double z0 = q.min.getZ(), z1 = q.max.getZ() + 1, x = q.min.getX() + 0.5;
                    vtx(b, m, cam, x, y0, z0, a, 0, 1);
                    vtx(b, m, cam, x, y0, z1, a, 1, 1);
                    vtx(b, m, cam, x, y1, z1, a, 1, 0);
                    vtx(b, m, cam, x, y1, z0, a, 0, 0);
                }
            }
        }
        if (any) { b.finishDrawing(); net.minecraft.client.renderer.WorldVertexBufferUploader.draw(b); }
        else b.finishDrawing();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vtx(BufferBuilder b, Matrix4f m, Vector3d cam, double x, double y, double z, float a, float u, float v) {
        b.pos(m, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color(1f, 1f, 1f, a).tex(u, v).endVertex();
    }
}
