package com.example.portalstick;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PortalEvents {

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.world.isRemote || !(e.world instanceof ServerWorld)) return;
        ServerWorld w = (ServerWorld) e.world;
        PortalData d = PortalData.get(w.getServer());
        if (d.pairs.isEmpty()) return;
        for (PortalData.Pair p : d.pairs) {
            check(w, p.a, p.b);
            check(w, p.b, p.a);
        }
    }

    private void check(ServerWorld w, PortalData.Portal f, PortalData.Portal t) {
        if (w.getDimensionKey() != f.dim) return;
        ServerWorld tw = w.getServer().getWorld(t.dim);
        if (tw == null) return;
        for (Entity e : w.getEntitiesWithinAABB(Entity.class, f.box(),
                x -> x.isAlive() && !x.isSpectator() && !x.isPassenger())) {
            if (w.getGameTime() < e.getPersistentData().getLong("ps_cd")) continue;
            boolean player = e instanceof ServerPlayerEntity;
            if (!player && tw != w) continue; // non-player hanya satu dimensi

            boolean fx = f.alongX(), tx = t.alongX();
            // posisi relatif di sepanjang lebar portal (0..1) -> dipetakan ke lebar portal tujuan
            double u = MathHelper.clamp(((fx ? e.getPosX() - f.min.getX() : e.getPosZ() - f.min.getZ())) / f.width(), 0.02, 0.98);
            double fNormal = (fx ? f.min.getZ() : f.min.getX()) + 0.5;
            int side = (fx ? e.getPosZ() : e.getPosX()) >= fNormal ? 1 : -1; // sisi masuk
            int out = -side; // keluar di sisi seberang (lanjut lurus)

            double along = (tx ? t.min.getX() : t.min.getZ()) + u * t.width();
            double normal = (tx ? t.min.getZ() : t.min.getX()) + 0.5 + out * 0.9;
            double dy = MathHelper.clamp(e.getPosY() - f.min.getY(), 0, Math.max(0, t.height() - e.getHeight()));
            double nx = tx ? along : normal, nz = tx ? normal : along, ny = t.min.getY() + dy;
            float yaw = tx ? (out > 0 ? 0f : 180f) : (out > 0 ? -90f : 90f);

            double speed = Math.max(0.1, e.getMotion().length());
            Vector3d mot = new Vector3d(tx ? 0 : out, 0, tx ? out : 0).scale(speed);
            e.getPersistentData().putLong("ps_cd", w.getGameTime() + 40);
            if (player) {
                ((ServerPlayerEntity) e).teleport(tw, nx, ny, nz, yaw, e.rotationPitch);
            } else {
                e.setLocationAndAngles(nx, ny, nz, yaw, e.rotationPitch);
            }
            e.setMotion(mot.x, e.getMotion().y, mot.z);
            e.velocityChanged = true;
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getPlayer() instanceof ServerPlayerEntity) Net.syncTo((ServerPlayerEntity) e.getPlayer());
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("portalstick").requires(s -> s.hasPermissionLevel(2))
                .then(Commands.literal("list").executes(c -> {
                    PortalData d = PortalData.get(c.getSource().getServer());
                    for (PortalData.Pair p : d.pairs)
                        c.getSource().sendFeedback(new StringTextComponent("#" + p.id + " A " + p.a.width() + "x" + p.a.height()
                                + " @ " + p.a.min.getX() + "," + p.a.min.getY() + "," + p.a.min.getZ()
                                + " | B " + p.b.width() + "x" + p.b.height()
                                + " @ " + p.b.min.getX() + "," + p.b.min.getY() + "," + p.b.min.getZ()), false);
                    return d.pairs.size();
                }))
                .then(Commands.literal("clear").executes(c -> {
                    PortalData d = PortalData.get(c.getSource().getServer());
                    d.pairs.clear(); d.markDirty();
                    Net.syncAll(c.getSource().getServer());
                    return 1;
                }))
                .then(Commands.literal("remove").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> {
                    int id = IntegerArgumentType.getInteger(c, "id");
                    PortalData d = PortalData.get(c.getSource().getServer());
                    boolean r = d.pairs.removeIf(p -> p.id == id);
                    d.markDirty();
                    Net.syncAll(c.getSource().getServer());
                    return r ? 1 : 0;
                }))));
    }
}
