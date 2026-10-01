package com.flowys.registry;

import com.flowys.FlowysMod;
import com.flowys.entity.FlowyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FlowysMod.MODID);

    public static final RegistryObject<EntityType<FlowyEntity>> FLOWY = ENTITIES.register("flowy",
            () -> EntityType.Builder.of(FlowyEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.6F)
                    .clientTrackingRange(8)
                    .build("flowy"));
}
