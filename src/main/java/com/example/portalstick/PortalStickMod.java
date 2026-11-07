package com.example.portalstick;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(PortalStickMod.ID)
public class PortalStickMod {
    public static final String ID = "portalstick";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final RegistryObject<Item> WAND = ITEMS.register("portal_wand",
            () -> new PortalWandItem(new Item.Properties().group(ItemGroup.TOOLS).maxStackSize(1)));

    public PortalStickMod() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        Net.init();
        MinecraftForge.EVENT_BUS.register(new PortalEvents());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(new ClientRender()));
    }
}
