package com.nexusrpg.mage;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

import javax.annotation.Nullable;

/** Forge discovers the registration automatically; do not register a second time in preInit. */
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class MageRegistration {
    private MageRegistration() { }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.<EntityTowerMage>create()
                .entity(EntityTowerMage.class)
                .id(new ResourceLocation("nexusrpg", "tower_mage"), 0)
                .name("tower_mage")
                .tracker(64, 3, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.<EntityMageFireball>create()
                .entity(EntityMageFireball.class)
                .id(new ResourceLocation("nexusrpg", "mage_fireball"), 2)
                .name("mage_fireball")
                .tracker(64, 1, true)
                .build());
    }

    /**
     * Spawn once on the server, AFTER the tower and chunk exist, at the empty block above
     * a solid floor. Returns null on invalid space, client side, or a cancelled spawn.
     * The caller must persist its own per-tower spawn/death marker; this method does not
     * deduplicate calls or respawn a boss killed earlier. Invoke on the server thread.
     */
    @Nullable
    public static EntityTowerMage spawnTowerMage(World world, BlockPos feet) {
        if (world == null || world.isRemote || feet == null || feet.getY() < 1 || feet.getY() > 253
                || !world.isBlockLoaded(feet) || !world.isBlockLoaded(feet.up(2))
                || !world.isBlockLoaded(feet.down())
                || !world.getBlockState(feet.down()).isSideSolid(world, feet.down(), EnumFacing.UP)) return null;
        EntityTowerMage mage = new EntityTowerMage(world);
        mage.setLocationAndAngles(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, 0.0F, 0.0F);
        if (!world.getCollisionBoxes(mage, mage.getEntityBoundingBox()).isEmpty()
                || world.containsAnyLiquid(mage.getEntityBoundingBox())
                || !world.checkNoEntityCollision(mage.getEntityBoundingBox(), mage)) return null;
        mage.setHomePosAndDistance(feet, 16);
        return world.spawnEntity(mage) ? mage : null;
    }
}
