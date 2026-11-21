package com.nexusrpg.character;

import java.util.UUID;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/** Authoritative player profile. Mutators must be called on the logical server thread. */
public final class CharacterProfile {
    static final String KEY="NexusRpgCharacter";
    static final String[] CLASSES={"warrior","mage","archer","assassin","paladin","necromancer"};
    static final String[] STATS={"health","mana","strength","defense","speed","intelligence","luck"};
    // Starting points are class bonuses, not spendable allocations.
    private static final int[][] BONUS={{3,0,4,2,0,0,0},{0,5,0,0,0,4,0},{0,1,2,0,3,0,2},{0,0,3,0,4,0,2},{4,2,1,4,0,1,0},{1,4,0,1,0,3,1}};
    private static final UUID HEALTH=UUID.fromString("10709e2a-e6ce-4507-9da9-c88812139001"), SPEED=UUID.fromString("10709e2a-e6ce-4507-9da9-c88812139002"), LUCK=UUID.fromString("10709e2a-e6ce-4507-9da9-c88812139003");
    private CharacterProfile() {}
    // An unexpected NBT type may be recoverable data: never overwrite it.
    private static boolean storageWritable(EntityPlayer p) {
        if (p==null) return false;
        NBTTagCompound data=p.getEntityData();
        if (data.hasKey(EntityPlayer.PERSISTED_NBT_TAG) && !data.hasKey(EntityPlayer.PERSISTED_NBT_TAG,Constants.NBT.TAG_COMPOUND)) return false;
        NBTTagCompound persisted=data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        return !persisted.hasKey(KEY) || persisted.hasKey(KEY,Constants.NBT.TAG_COMPOUND);
    }
    static NBTTagCompound root(EntityPlayer p) {
        if (!storageWritable(p)) return new NBTTagCompound();
        NBTTagCompound data=p.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)) data.setTag(EntityPlayer.PERSISTED_NBT_TAG,new NBTTagCompound());
        NBTTagCompound persisted=data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey(KEY,Constants.NBT.TAG_COMPOUND)) persisted.setTag(KEY,new NBTTagCompound());
        return persisted.getCompoundTag(KEY);
    }
    static void replace(EntityPlayer p,NBTTagCompound tag) {
        if (tag==null || !storageWritable(p)) return;
        root(p); // ensure PlayerPersisted exists before installing the copy
        p.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).setTag(KEY,tag.copy());
    }
    private static boolean server(EntityPlayer p) {
        if (p==null || p.world==null || p.world.isRemote || !storageWritable(p)) return false;
        NBTTagCompound n=root(p);
        if (n.hasKey("class") && !n.hasKey("class",Constants.NBT.TAG_STRING)) return false;
        String[] fields={"level","xp","points","mana","stat_health","stat_mana","stat_strength","stat_defense","stat_speed","stat_intelligence","stat_luck"};
        for (String field:fields) {
            if (!n.hasKey(field)) continue;
            if (!n.hasKey(field,Constants.NBT.TAG_INT)) return false;
            int value=n.getInteger(field);
            if (value<0 || (field.equals("level") && (value<1 || value>1000000)) || (field.startsWith("stat_") && value>10000)) return false;
        }
        return true;
    }
    private static int index(EntityPlayer p) { String c=root(p).getString("class"); for(int i=0;i<CLASSES.length;i++) if(CLASSES[i].equals(c)) return i; return -1; }
    public static String characterClass(EntityPlayer p) { if(p==null) return ""; int i=index(p); return i<0?"":CLASSES[i]; }
    public static int level(EntityPlayer p) { return p==null?1:Math.min(1000000,Math.max(1,root(p).getInteger("level"))); }
    /** Total stat, including the class bonus; returns 0 for unknown stat names. */
    public static int stat(EntityPlayer p,String name) {
        if(p==null) return 0; int c=index(p); if(c<0) return 0;
        for(int i=0;i<STATS.length;i++) if(STATS[i].equals(name)) return Math.max(0,Math.min(10000,root(p).getInteger("stat_"+name)))+BONUS[c][i];
        return 0;
    }
    public static int intelligence(EntityPlayer p) { return stat(p,"intelligence"); }
    public static int points(EntityPlayer p) { return p==null?0:Math.max(0,root(p).getInteger("points")); }
    public static int xp(EntityPlayer p) { return p==null?0:Math.max(0,root(p).getInteger("xp")); }
    public static int xpForNextLevel(EntityPlayer p) { return (int)Math.min(Integer.MAX_VALUE,100L+30L*(level(p)-1)); }
    public static int maxMana(EntityPlayer p) { return characterClass(p).isEmpty()?0:100+stat(p,"mana")*10+intelligence(p)*5; }
    public static int mana(EntityPlayer p) { return p==null?0:Math.max(0,Math.min(maxMana(p),root(p).getInteger("mana"))); }
    /** Consume mana atomically on server thread; false on client, invalid cost or insufficient mana. */
    public static boolean consumeMana(EntityPlayer p,int amount) {
        if(!server(p)||amount<0||characterClass(p).isEmpty()||mana(p)<amount) return false;
        root(p).setInteger("mana",mana(p)-amount); sync(p); return true;
    }
    static boolean select(EntityPlayer p,int choice) {
        // A nonempty unknown/partial profile must not be reset by class selection.
        if(!server(p)||choice<0||choice>=CLASSES.length||!root(p).getKeySet().isEmpty()) return false;
        NBTTagCompound n=root(p); n.setString("class",CLASSES[choice]); n.setInteger("level",1); n.setInteger("xp",0); n.setInteger("points",0); n.setInteger("mana",maxMana(p));
        apply(p); sync(p); return true;
    }
    static boolean allocate(EntityPlayer p,int which) {
        if(!server(p)||characterClass(p).isEmpty()||which<0||which>=STATS.length||points(p)<=0) return false;
        NBTTagCompound n=root(p); String key="stat_"+STATS[which]; int current=n.getInteger(key); if(current<0||current>=10000) return false;
        n.setInteger(key,current+1); n.setInteger("points",points(p)-1); apply(p); sync(p); return true;
    }
    /** Adds combat XP; does nothing on client, unselected class or nonpositive amount. */
    public static void addXp(EntityPlayer p,int amount) {
        if(!server(p)||amount<=0||characterClass(p).isEmpty()) return;
        NBTTagCompound n=root(p); long total=(long)xp(p)+amount; int l=level(p); int pts=points(p);
        while(l<1000000) { long need=100L+30L*(l-1); if(total<need) break; total-=need; l++; pts=(int)Math.min(Integer.MAX_VALUE,(long)pts+3); }
        n.setInteger("level",l); n.setInteger("points",pts); n.setInteger("xp",(int)Math.min(Integer.MAX_VALUE,total)); sync(p);
    }
    static void regenerate(EntityPlayer p) {
        if(!server(p)||characterClass(p).isEmpty()||p.isDead) return;
        int old=mana(p); int next=Math.min(maxMana(p),old+1+intelligence(p)/10); if(next!=old) {root(p).setInteger("mana",next);sync(p);}
        if(p.ticksExisted%200==0 && p.getHealth()>0 && p.getHealth()<p.getMaxHealth()) p.heal(0.5F+Math.min(4,stat(p,"health")/20F));
    }
    private static void modifier(IAttributeInstance attribute,UUID id,String name,double amount) {
        if(attribute==null) return; AttributeModifier existing=attribute.getModifier(id); if(existing!=null) attribute.removeModifier(existing);
        if(amount!=0) attribute.applyModifier(new AttributeModifier(id,name,amount,0).setSaved(false));
    }
    /** Idempotently refreshes our modifiers without touching those of other mods. */
    static void apply(EntityPlayer p) {
        if(!server(p)) return;
        modifier(p.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH),HEALTH,"Nexus health",stat(p,"health")*2.0);
        modifier(p.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED),SPEED,"Nexus speed",Math.min(0.2,stat(p,"speed")*0.002));
        modifier(p.getEntityAttribute(SharedMonsterAttributes.LUCK),LUCK,"Nexus luck",Math.min(100,stat(p,"luck")*0.1));
        if(p.getHealth()>p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
    static void sync(EntityPlayer p) { if(p instanceof EntityPlayerMP) CharacterNetwork.sync((EntityPlayerMP)p); }
}
