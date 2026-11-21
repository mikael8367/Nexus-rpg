package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class MobRegistration {
    private MobRegistration() { }
    @SubscribeEvent public static void register(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.<EntityBramble>create().entity(EntityBramble.class).id(new ResourceLocation("nexusrpg", "bramble_hound"), 7).name("bramble_hound").tracker(64, 3, true).egg(0x344B27, 0xC7A049).build());
        event.getRegistry().register(EntityEntryBuilder.<EntityCavern>create().entity(EntityCavern.class).id(new ResourceLocation("nexusrpg", "cavern_crawler"), 8).name("cavern_crawler").tracker(64, 3, true).egg(0x343750, 0x62CBD2).build());
        event.getRegistry().register(EntityEntryBuilder.<EntityBasalt>create().entity(EntityBasalt.class).id(new ResourceLocation("nexusrpg", "basalt_guardian"), 9).name("basalt_guardian").tracker(64, 3, true).egg(0x383B40, 0xE88B39).build());
        event.getRegistry().register(EntityEntryBuilder.<EntityStilt>create().entity(EntityStilt.class).id(new ResourceLocation("nexusrpg", "reed_stilt"), 10).name("reed_stilt").tracker(64, 3, true).egg(0x51745B, 0xCEB968).build());
        event.getRegistry().register(EntityEntryBuilder.<EntityPrism>create().entity(EntityPrism.class).id(new ResourceLocation("nexusrpg", "prism_tortoise"), 11).name("prism_tortoise").tracker(64, 3, true).egg(0x4B4D64, 0x74CDCA).build());
    }
}
