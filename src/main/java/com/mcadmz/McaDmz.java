package com.mcadmz;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(McaDmz.MODID)
public class McaDmz {
    public static final String MODID = "mcadmz";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> SCOUTER = ITEMS.register("scouter",
            () -> new ScouterItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> EXTRA_RING = ITEMS.register("extra_ring",
            () -> new ExtraRingItem(new Item.Properties().stacksTo(16)));

    public McaDmz() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(this::tabs);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, McadmzConfig.SPEC);
    }

    private void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(SCOUTER);
            e.accept(EXTRA_RING);
        }
    }
}
