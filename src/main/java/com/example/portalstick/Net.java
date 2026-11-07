package com.example.portalstick;

import java.util.function.Supplier;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class Net {
    private static final String V = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PortalStickMod.ID, "main"), () -> V, V::equals, V::equals);

    public static void init() {
        CH.registerMessage(0, Sync.class, Sync::enc, Sync::dec, Sync::handle);
    }

    public static void syncAll(MinecraftServer s) {
        CH.send(PacketDistributor.ALL.noArg(), new Sync(PortalData.get(s).write(new CompoundNBT())));
    }

    public static void syncTo(ServerPlayerEntity p) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), new Sync(PortalData.get(p.getServer()).write(new CompoundNBT())));
    }

    public static class Sync {
        final CompoundNBT tag;
        Sync(CompoundNBT t) { tag = t; }
        static void enc(Sync m, PacketBuffer b) { b.writeCompoundTag(m.tag); }
        static Sync dec(PacketBuffer b) { return new Sync(b.readCompoundTag()); }
        static void handle(Sync m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRender.set(m.tag)));
            c.get().setPacketHandled(true);
        }
    }
}
