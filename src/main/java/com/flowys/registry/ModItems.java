package com.flowys.registry;

import com.flowys.FlowysMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FlowysMod.MODID);

    public static final RegistryObject<Item> FLOWY_SPAWN_EGG = ITEMS.register("flowy_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FLOWY, 0xFFD83A, 0xC8761A, new Item.Properties()));
}
