package com.nexusrpg.mobs.client;
import com.nexusrpg.mobs.*;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = "nexusrpg", value = Side.CLIENT)
public final class MobClientRegistration {
    private MobClientRegistration() { }
    @SubscribeEvent public static void register(RegistryEvent.Register<EntityEntry> event) {
        RenderingRegistry.registerEntityRenderingHandler(EntityBramble.class, m -> new MobRenderer<EntityBramble>(m, new ModelBramble(), "bramble_hound"));
        RenderingRegistry.registerEntityRenderingHandler(EntityCavern.class, m -> new MobRenderer<EntityCavern>(m, new ModelCavern(), "cavern_crawler"));
        RenderingRegistry.registerEntityRenderingHandler(EntityBasalt.class, m -> new MobRenderer<EntityBasalt>(m, new ModelBasalt(), "basalt_guardian"));
    }
}
