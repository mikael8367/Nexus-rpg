package com.nexusrpg.magic;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class MagicRegistry {
    public static final Item ARCANE_STAFF = staff("arcane_staff", ItemMagicStaff.Spell.ARCANE, 20);
    public static final Item HEALING_STAFF = staff("healing_staff", ItemMagicStaff.Spell.HEAL, 200);
    public static final Item WARD_STAFF = staff("ward_staff", ItemMagicStaff.Spell.WARD, 300);
    public static final Item SWIFT_STAFF = staff("swift_staff", ItemMagicStaff.Spell.SWIFT, 200);

    private MagicRegistry() {}
    private static Item staff(String name, ItemMagicStaff.Spell spell, int cooldown) {
        return new ItemMagicStaff(spell, cooldown).setRegistryName("nexusrpg", name)
            .setUnlocalizedName("nexusrpg." + name).setCreativeTab(CreativeTabs.TOOLS);
    }
    @SubscribeEvent
    public static void register(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(ARCANE_STAFF, HEALING_STAFF, WARD_STAFF, SWIFT_STAFF);
    }
}
