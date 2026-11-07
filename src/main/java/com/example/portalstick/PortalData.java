package com.example.portalstick;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

public class PortalData extends WorldSavedData {
    public static final String NAME = PortalStickMod.ID + "_portals";
    public final List<Pair> pairs = new ArrayList<>();
    public int nextId = 1;

    public PortalData() { super(NAME); }

    public static PortalData get(MinecraftServer s) {
        return s.getWorld(World.OVERWORLD).getSavedData().getOrCreate(PortalData::new, NAME);
    }

    public static RegistryKey<World> key(String s) {
        return RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(s));
    }

    @Override
    public void read(CompoundNBT n) {
        pairs.clear();
        nextId = n.getInt("Next");
        ListNBT l = n.getList("Pairs", 10);
        for (int i = 0; i < l.size(); i++) {
            CompoundNBT c = l.getCompound(i);
            Pair p = new Pair();
            p.id = c.getInt("Id");
            p.a = Portal.load(c.getCompound("A"));
            p.b = Portal.load(c.getCompound("B"));
            pairs.add(p);
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT n) {
        n.putInt("Next", nextId);
        ListNBT l = new ListNBT();
        for (Pair p : pairs) {
            CompoundNBT c = new CompoundNBT();
            c.putInt("Id", p.id);
            c.put("A", p.a.save());
            c.put("B", p.b.save());
            l.add(c);
        }
        n.put("Pairs", l);
        return n;
    }

    public static class Pair {
        public int id;
        public Portal a, b;
    }

    public static class Portal {
        public static final int MAX = 64; // batas ukuran (blok)
        public RegistryKey<World> dim;
        public BlockPos min, max;

        /** a,b = dua sudut bawah (Y sama), top = sudut atas. Null kalau tidak valid. */
        public static Portal make(RegistryKey<World> dim, BlockPos a, BlockPos b, BlockPos top) {
            if (a.getY() != b.getY()) return null;
            if (a.getX() != b.getX() && a.getZ() != b.getZ()) return null;
            Portal p = new Portal();
            p.dim = dim;
            p.min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), top.getY()), Math.min(a.getZ(), b.getZ()));
            p.max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), top.getY()), Math.max(a.getZ(), b.getZ()));
            if (p.width() > MAX || p.height() > MAX) return null;
            return p;
        }

        /** true = bidang XY (tepi memanjang di X, tebal di Z). false = bidang ZY. */
        public boolean alongX() { return min.getZ() == max.getZ(); }
        public int width() { return alongX() ? max.getX() - min.getX() + 1 : max.getZ() - min.getZ() + 1; }
        public int height() { return max.getY() - min.getY() + 1; }

        public AxisAlignedBB box() {
            return new AxisAlignedBB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        }

        public CompoundNBT save() {
            CompoundNBT c = new CompoundNBT();
            c.putString("Dim", dim.getLocation().toString());
            c.putLong("Min", min.toLong());
            c.putLong("Max", max.toLong());
            return c;
        }

        public static Portal load(CompoundNBT c) {
            Portal p = new Portal();
            p.dim = key(c.getString("Dim"));
            p.min = BlockPos.fromLong(c.getLong("Min"));
            p.max = BlockPos.fromLong(c.getLong("Max"));
            return p;
        }
    }
}
