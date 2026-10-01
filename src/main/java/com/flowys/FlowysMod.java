package com.flowys;

import com.flowys.registry.ModEntities;
import com.flowys.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FlowysMod.MODID)
public class FlowysMod {
    public static final String MODID = "flowys";

    public FlowysMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
    }
}
