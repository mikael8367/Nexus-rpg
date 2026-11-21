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
        RenderingRegistry.registerEntityRenderingHandler(EntityStilt.class, m -> new MobRenderer<EntityStilt>(m, new ModelStilt(), "reed_stilt"));
        RenderingRegistry.registerEntityRenderingHandler(EntityPrism.class, m -> new MobRenderer<EntityPrism>(m, new ModelPrism(), "prism_tortoise"));
        RenderingRegistry.registerEntityRenderingHandler(EntityLumenMoth.class, m -> new MobRenderer<EntityLumenMoth>(m, new ModelLumenMoth(), "lumen_moth"));
        RenderingRegistry.registerEntityRenderingHandler(EntityDuneMole.class, m -> new MobRenderer<EntityDuneMole>(m, new ModelDuneMole(), "dune_mole"));
        RenderingRegistry.registerEntityRenderingHandler(EntityBellback.class, m -> new MobRenderer<EntityBellback>(m, new ModelBellback(), "bellback"));
        RenderingRegistry.registerEntityRenderingHandler(EntityBurrower.class, m -> new MobRenderer<EntityBurrower>(m, new ModelBurrower(), "dune_burrower"));
        RenderingRegistry.registerEntityRenderingHandler(EntityRootwarden.class, m -> new MobRenderer<EntityRootwarden>(m, new ModelRootwarden(), "root_warden"));
    }
}
