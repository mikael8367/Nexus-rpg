package com.nexusrpg.expedition;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class ExpeditionAidTest {
    static void check(boolean b, String message) { if (!b) throw new AssertionError(message); }
    static final class Target implements ItemExpeditionAid.Target {
        boolean cool, burning, underwater, dive, sight, reject;
        int applied, extinguished, cooldowns, ticks;
        public boolean cooling(Item item) { return cool; }
        public void cooldown(Item item, int t) { cool = true; cooldowns++; check(t == 40, "cooldown"); }
        public boolean burning() { return burning; }
        public void extinguish() { extinguished++; if (!reject) burning = false; }
        public boolean underwater() { return underwater; }
        public boolean active(ItemExpeditionAid.Kind k) { return k == ItemExpeditionAid.Kind.DIVE ? dive : sight; }
        public void apply(ItemExpeditionAid.Kind k, int t) { ticks=t; applied++; if (!reject) { if (k == ItemExpeditionAid.Kind.DIVE) dive=true; else sight=true; } }
    }
    static void ready(Target t, ItemExpeditionAid.Kind k) { t.burning=true; t.underwater=true; t.dive=false; t.sight=false; t.cool=false; }
    public static void main(String[] args) {
        Bootstrap.register();
        for (ItemExpeditionAid.Kind kind : ItemExpeditionAid.Kind.values()) {
            ItemExpeditionAid a = new ItemExpeditionAid(kind);
            check(a.getItemStackLimit() == 16, "stack size");
            ItemStack s = new ItemStack(a, 3);
            NBTTagCompound n = new NBTTagCompound(); n.setString("other_mod", "preserved"); s.setTagCompound(n); NBTTagCompound saved = n.copy();
            Target t = new Target();
            if (kind != ItemExpeditionAid.Kind.SIGHT) check(!a.use(s,t,false) && s.getCount()==3, "no fire/no water");
            if (kind != ItemExpeditionAid.Kind.QUENCH) {
                ready(t,kind); if (kind == ItemExpeditionAid.Kind.DIVE) t.dive=true; else t.sight=true;
                check(!a.use(s,t,false) && t.applied==0 && s.getCount()==3, "existing effect preserved");
            }
            ready(t,kind); t.reject=true;
            check(!a.use(s,t,false) && t.cooldowns==0 && s.getCount()==3, "rejected action has no cost");
            ready(t,kind); t.reject=false; int applications = t.applied, extinguishes=t.extinguished;
            check(a.use(s,t,false) && s.getCount()==2 && t.cooldowns==1 && saved.equals(s.getTagCompound()), "survival consumption and tags");
            if (kind == ItemExpeditionAid.Kind.QUENCH) check(t.extinguished==extinguishes+1 && t.applied==applications, "quench only");
            else check(t.applied==applications+1 && t.ticks==600 && t.extinguished==extinguishes && t.active(kind), "exact effect and duration");
            check(!a.use(s,t,false) && s.getCount()==2, "cooldown");
            ready(t,kind); check(a.use(s,t,true) && s.getCount()==2 && saved.equals(s.getTagCompound()), "creative");
            ready(t,kind); check(!a.use(ItemStack.EMPTY,t,false) && !a.use(null,t,false) && !a.use(s,null,false) && !a.use(new ItemStack(new Item()),t,false), "invalid input");
        }
        System.out.println("ExpeditionAidTest passed");
    }
}
