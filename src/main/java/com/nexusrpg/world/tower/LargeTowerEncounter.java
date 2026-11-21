package com.nexusrpg.world.tower;

import com.nexusrpg.world.structures.LargeWizardTower;

import com.nexusrpg.init.ModItems;
import com.nexusrpg.mage.EntityTowerMage;
import com.nexusrpg.mage.MageRegistration;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Optional post-commit encounter; never call this during planning or rollback. */
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class LargeTowerEncounter {
    private static final String STYLE = "NexusLargeTowerKeyStyle";
    private static final String DROPPED = "NexusLargeTowerKeyDropped";
    private LargeTowerEncounter() { }

    /** Invoke once per committed tower, on the server thread, while its chunk is loaded.
     * No retries or persistent spawn markers are provided. Return true iff boss spawned.
     * False (including peaceful difficulty or a cancelled spawn) means no boss/key.
     */
    public static boolean spawn(World world, int x, int y, int z, int style) {
        if (world == null || world.isRemote || !LargeWizardTower.supports(style) ||
                world.getDifficulty() == EnumDifficulty.PEACEFUL) return false;
        BlockPos bossFeet = LargeWizardTower.bossFeet(x, y, z);
        EntityTowerMage boss = MageRegistration.spawnTowerMage(world, bossFeet);
        if (boss == null) return false;
        // getEntityData is persisted by Forge across normal chunk unloads.
        boss.getEntityData().setInteger(STYLE, style + 1);
        for (int floor = 0; floor < 5; floor++) {
            BlockPos feet = LargeWizardTower.guardFeet(x, y, z, floor);
            if (!world.isBlockLoaded(feet) || !world.isBlockLoaded(feet.up()) || !world.isBlockLoaded(feet.down())) continue;
            if (!world.getBlockState(feet.down()).isSideSolid(world, feet.down(), net.minecraft.util.EnumFacing.UP)) continue;
            EntityLiving guard = floor % 2 == 0 ? new EntityZombie(world) : new EntitySkeleton(world);
            guard.setLocationAndAngles(feet.getX() + .5D, feet.getY(), feet.getZ() + .5D, 0.0F, 0.0F);
            if (!world.getCollisionBoxes(guard, guard.getEntityBoundingBox()).isEmpty() ||
                world.containsAnyLiquid(guard.getEntityBoundingBox()) ||
                !world.checkNoEntityCollision(guard.getEntityBoundingBox(), guard)) continue;
            guard.onInitialSpawn(world.getDifficultyForLocation(feet), null);
            guard.enablePersistence();
            world.spawnEntity(guard); // Spawn can be cancelled independently.
        }
        return true;
    }

    /** Reward only bosses marked by this encounter. Preserve the marker across reloads. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onBossDrops(LivingDropsEvent event) {
        if (!(event.getEntityLiving() instanceof EntityTowerMage) || event.getEntityLiving().world.isRemote) return;
        EntityTowerMage boss = (EntityTowerMage) event.getEntityLiving();
        NBTTagCompound data = boss.getEntityData();
        int style = data.getInteger(STYLE) - 1;
        if (!LargeWizardTower.supports(style) || data.getBoolean(DROPPED)) return;
        Item[] keys = {ModItems.KEY_GREEN, ModItems.KEY_CYAN, ModItems.KEY_YELLOW, ModItems.KEY_PURPLE,
                       ModItems.KEY_RED, ModItems.KEY_PINK, ModItems.KEY_BLACK, ModItems.KEY_ORANGE};
        EntityItem dropped = new EntityItem(boss.world, boss.posX, boss.posY, boss.posZ, new ItemStack(keys[style]));
        event.getDrops().add(dropped);
        data.setBoolean(DROPPED, true);
    }
}
