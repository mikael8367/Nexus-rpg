package com.nexusrpg.dragons;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/** Unique mod-local IDs 3-6; 0, 1 and 2 belong to mage, ent and fireball. */
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class DragonRegistration {
    private DragonRegistration() { }
    @SubscribeEvent public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.<EntityFireDragon>create()
                .entity(EntityFireDragon.class).id(new ResourceLocation("nexusrpg", "fire_dragon"), 3)
                .name("fire_dragon").tracker(96, 3, true).egg(0x9B2815, 0xFFAA22).build());
        event.getRegistry().register(EntityEntryBuilder.<EntityWaterDragon>create()
                .entity(EntityWaterDragon.class).id(new ResourceLocation("nexusrpg", "water_dragon"), 4)
                .name("water_dragon").tracker(96, 3, true).egg(0x164D8B, 0x55DDEE).build());
        event.getRegistry().register(EntityEntryBuilder.<EntityWindDragon>create()
                .entity(EntityWindDragon.class).id(new ResourceLocation("nexusrpg", "wind_dragon"), 5)
                .name("wind_dragon").tracker(96, 3, true).egg(0xBDD7DA, 0xF8FAF4).build());
        event.getRegistry().register(EntityEntryBuilder.<EntityEarthDragon>create()
                .entity(EntityEarthDragon.class).id(new ResourceLocation("nexusrpg", "earth_dragon"), 6)
                .name("earth_dragon").tracker(96, 3, true).egg(0x654A2F, 0x82A64A).build());
    }
}
