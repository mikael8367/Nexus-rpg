package com.nexusrpg.supplies;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class SupplyTest {
    static void check(boolean result, String name) { if (!result) throw new AssertionError(name); }
    static final class Target implements ItemFieldSupply.Target {
        boolean cooling, heal, cleanse, speed;
        int healCalls, cleanseCalls, speedCalls, cooldownCalls;
        public boolean cooldown(Item item) { return cooling; }
        public void cooldown(Item item, int ticks) { check(ticks == 40, "cooldown ticks"); cooling = true; cooldownCalls++; }
        public boolean heal() { healCalls++; return heal; }
        public boolean cleanse() { cleanseCalls++; return cleanse; }
        public boolean speed() { speedCalls++; return speed; }
    }
    public static void main(String[] args) {
        net.minecraft.init.Bootstrap.register();
        for (ItemFieldSupply.Kind kind : ItemFieldSupply.Kind.values()) {
            ItemFieldSupply supply = new ItemFieldSupply(kind);
            ItemStack stack = new ItemStack(supply, 3);
            NBTTagCompound unknown = new NBTTagCompound(); unknown.setString("future", "untouched");
            NBTTagCompound nested = new NBTTagCompound(); nested.setLong("other", 789); unknown.setTag("nested", nested);
            stack.setTagCompound(unknown); NBTTagCompound before = unknown.copy();
            Target t = new Target();
            check(supply.getItemStackLimit() == 16, "limit");
            check(!supply.use(stack,t,false) && stack.getCount()==3 && t.cooldownCalls==0, "no benefit has no cost " + kind);
            t.heal = t.cleanse = t.speed = true;
            check(supply.use(stack,t,false) && stack.getCount()==2 && before.equals(stack.getTagCompound()), "survival and NBT " + kind);
            int calls = t.healCalls + t.cleanseCalls + t.speedCalls;
            check(!supply.use(stack,t,false) && stack.getCount()==2 && calls==t.healCalls+t.cleanseCalls+t.speedCalls, "replay cooldown " + kind);
            t.cooling=false;
            check(supply.use(stack,t,true) && stack.getCount()==2 && before.equals(stack.getTagCompound()), "creative " + kind);
            t.cooling=false;
            check(!supply.use(ItemStack.EMPTY,t,false) && !supply.use(stack,null,false), "invalid");
            check(!supply.use(new ItemStack(new Item()),t,false), "wrong item");
            ItemStack copied=stack.copy();
            check(before.equals(copied.getTagCompound()) && copied.getCount()==2, "copy " + kind);
            copied.getTagCompound().setString("future","copy");
            check(before.equals(stack.getTagCompound()), "copy does not alias original");
            check(kind==ItemFieldSupply.Kind.BANDAGE ? t.cleanseCalls==0 && t.speedCalls==0 :
                  kind==ItemFieldSupply.Kind.ANTIDOTE ? t.healCalls==0 && t.speedCalls==0 : t.healCalls==0 && t.cleanseCalls==0, "exclusive effect");
        }
        // Exercise item use alongside existing, independently persisted progression.
        net.minecraft.nbt.NBTTagCompound root = new net.minecraft.nbt.NBTTagCompound();
        root.setString("unknown_mod_data", "preserved");
        check(com.nexusrpg.quests.JourneyData.record(root, com.nexusrpg.quests.JourneyData.Journey.EXPLORER, 1), "journey start");
        check(com.nexusrpg.quests.HuntData.start(root), "zombie start");
        java.util.UUID zombie = java.util.UUID.randomUUID();
        check(com.nexusrpg.quests.HuntData.record(root, zombie), "zombie kill");
        check(com.nexusrpg.quests.TrailData.start(root, com.nexusrpg.quests.TrailData.Trail.SPIDER), "spider start");
        java.util.UUID spider = java.util.UUID.randomUUID();
        check(com.nexusrpg.quests.TrailData.record(root, com.nexusrpg.quests.TrailData.Trail.SPIDER, spider), "spider kill");
        net.minecraft.nbt.NBTTagCompound saved = root.copy();
        ItemFieldSupply item = new ItemFieldSupply(ItemFieldSupply.Kind.ESCAPE);
        Target t = new Target(); t.speed=true;
        check(item.use(new ItemStack(item, 2),t,false) && root.equals(saved), "use leaves quest data untouched");
        check(!com.nexusrpg.quests.HuntData.record(root, zombie) && !com.nexusrpg.quests.TrailData.record(root, com.nexusrpg.quests.TrailData.Trail.SPIDER, spider), "kill dedup after use");
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.CompressedStreamTools.writeCompressed(root,out);
            root = net.minecraft.nbt.CompressedStreamTools.readCompressed(new java.io.ByteArrayInputStream(out.toByteArray()));
        } catch (java.io.IOException ex) { throw new AssertionError(ex); }
        check(root.equals(saved), "quest persistence including unknown data");
        net.minecraft.nbt.NBTTagCompound clone = new net.minecraft.nbt.NBTTagCompound();
        com.nexusrpg.quests.JourneyData.cloneProgress(root,clone,com.nexusrpg.quests.JourneyData.Journey.EXPLORER);
        com.nexusrpg.quests.HuntData.cloneProgress(root,clone);
        com.nexusrpg.quests.TrailData.cloneProgress(root,clone,com.nexusrpg.quests.TrailData.Trail.SPIDER);
        check(com.nexusrpg.quests.JourneyData.mask(clone,com.nexusrpg.quests.JourneyData.Journey.EXPLORER)==1 &&
            com.nexusrpg.quests.HuntData.count(clone)==1 && com.nexusrpg.quests.TrailData.count(clone,com.nexusrpg.quests.TrailData.Trail.SPIDER)==1, "clone preserves quests");
        System.out.println("SupplyTest passed");
    }
}
