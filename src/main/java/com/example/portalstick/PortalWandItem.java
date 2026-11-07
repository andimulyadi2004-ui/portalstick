package com.example.portalstick;

import java.util.Arrays;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

/**
 * Alur klik kanan pada blok:
 *  1) sudut bawah  2) sudut bawah lain (Y sama)  3) sudut atas  -> Portal A
 *  4..6) sama lagi -> Portal B, lalu keduanya otomatis terhubung.
 * Sneak + klik kanan (di udara) = reset pilihan.
 */
public class PortalWandItem extends Item {
    public PortalWandItem(Properties p) { super(p); }

    private static void msg(PlayerEntity p, String s) {
        p.sendStatusMessage(new StringTextComponent(s), true);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext c) {
        World w = c.getWorld();
        PlayerEntity pl = c.getPlayer();
        if (pl == null) return ActionResultType.PASS;
        if (w.isRemote) return ActionResultType.SUCCESS;

        ItemStack st = c.getItem();
        CompoundNBT t = st.getOrCreateTag();
        long[] old = t.getLongArray("Pts");
        long[] pts = Arrays.copyOf(old, old.length + 1);
        pts[old.length] = c.getPos().toLong();
        int n = pts.length;
        t.putLongArray("Pts", pts);
        if (n == 1) t.putString("DimA", w.getDimensionKey().getLocation().toString());
        if (n == 4) t.putString("DimB", w.getDimensionKey().getLocation().toString());

        switch (n) {
            case 1: msg(pl, "Portal A: sudut bawah 1 dipilih. Klik sudut bawah lain (Y sama)."); break;
            case 2: msg(pl, "Portal A: sekarang klik sudut ATAS."); break;
            case 4: msg(pl, "Portal B: sudut bawah 1 dipilih. Klik sudut bawah lain (Y sama)."); break;
            case 5: msg(pl, "Portal B: sekarang klik sudut ATAS."); break;
            case 3: {
                PortalData.Portal a = build(t, "DimA", pts, 0);
                if (a == null) { st.setTag(null); msg(pl, "Portal A tidak valid (Y harus sama, lurus di X atau Z, maks " + PortalData.Portal.MAX + "). Reset."); }
                else msg(pl, "Portal A siap (" + a.width() + "x" + a.height() + "). Sekarang pilih Portal B.");
                break;
            }
            case 6: {
                PortalData.Portal a = build(t, "DimA", pts, 0);
                PortalData.Portal b = build(t, "DimB", pts, 3);
                if (a == null || b == null) {
                    t.putLongArray("Pts", Arrays.copyOf(pts, 3));
                    msg(pl, "Portal B tidak valid. Ulangi Portal B.");
                } else {
                    PortalData d = PortalData.get(w.getServer());
                    PortalData.Pair pr = new PortalData.Pair();
                    pr.id = d.nextId++;
                    pr.a = a; pr.b = b;
                    d.pairs.add(pr);
                    d.markDirty();
                    Net.syncAll(w.getServer());
                    st.setTag(null);
                    msg(pl, "Portal #" + pr.id + " terhubung! A " + a.width() + "x" + a.height() + " <-> B " + b.width() + "x" + b.height());
                }
                break;
            }
        }
        return ActionResultType.SUCCESS;
    }

    private static PortalData.Portal build(CompoundNBT t, String dimKey, long[] p, int o) {
        return PortalData.Portal.make(PortalData.key(t.getString(dimKey)),
                BlockPos.fromLong(p[o]), BlockPos.fromLong(p[o + 1]), BlockPos.fromLong(p[o + 2]));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World w, PlayerEntity p, Hand h) {
        ItemStack s = p.getHeldItem(h);
        if (p.isSecondaryUseActive()) {
            if (!w.isRemote) { s.setTag(null); msg(p, "Pilihan direset."); }
            return ActionResult.resultSuccess(s);
        }
        return ActionResult.resultPass(s);
    }
}
