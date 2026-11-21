package com.nexusrpg.character;

import net.minecraft.nbt.NBTTagCompound;

/** Tests NBT stat reads against real Minecraft NBT, not a running game. */
public final class CharacterStatSafetyTest {
    private static void check(boolean value,String label) { if(!value) throw new AssertionError(label); }
    private static NBTTagCompound profile(String name) {
        NBTTagCompound n=new NBTTagCompound();
        n.setString("class",name); n.setInteger("level",1); n.setInteger("xp",0);
        n.setInteger("points",0); n.setInteger("mana",0); return n;
    }
    private static void noBonuses(NBTTagCompound n,String label) {
        NBTTagCompound before=n.copy();
        for(String stat:CharacterProfile.STATS)
            check(CharacterProfile.statData(n,stat)==0,label+" "+stat);
        check(n.equals(before),label+" modified NBT");
    }
    public static void main(String[] args) {
        int[][] bonuses={{3,0,4,2,0,0,0},{0,5,0,0,0,4,0},{0,1,2,0,3,0,2},
                         {0,0,3,0,4,0,2},{4,2,1,4,0,1,0},{1,4,0,1,0,3,1}};
        for(int c=0;c<CharacterProfile.CLASSES.length;c++) {
            NBTTagCompound n=profile(CharacterProfile.CLASSES[c]);
            for(int i=0;i<CharacterProfile.STATS.length;i++) {
                String stat=CharacterProfile.STATS[i];
                check(CharacterProfile.statData(n,stat)==bonuses[c][i],"class bonus "+c+" "+stat);
                n.setInteger("stat_"+stat,123);
                check(CharacterProfile.statData(n,stat)==bonuses[c][i]+123,"allocation "+c+" "+stat);
            }
            check(CharacterProfile.statData(n,"unknown")==0,"unknown stat");
            check(CharacterProfile.statData(n,null)==0,"null stat");
        }
        NBTTagCompound n=profile("mage"); n.removeTag("mana");
        noBonuses(n,"incomplete profile");
        n.setInteger("mana",0);
        check(100+CharacterProfile.statData(n,"mana")*10+CharacterProfile.statData(n,"intelligence")*5==170,
              "complete mage profile retains starting mana bonuses");
        n=profile("warrior"); n.setInteger("stat_strength",10000); n.setInteger("points",-1); noBonuses(n,"negative points");
        n=profile("warrior"); n.setInteger("stat_defense",10001); noBonuses(n,"out-of-range stat");
        n=profile("warrior"); n.setString("stat_strength","future data"); noBonuses(n,"wrong stat type");
        n=profile("warrior"); n.setString("level","future data"); noBonuses(n,"wrong required type");
        n=profile("future_class"); noBonuses(n,"unknown class");
        noBonuses(new NBTTagCompound(),"empty profile");
        check(CharacterProfile.statData(null,"strength")==0,"null profile");
        n=profile("warrior"); n.setString("other_mod_data","preserve"); NBTTagCompound before=n.copy();
        check(CharacterProfile.statData(n,"strength")==4 && n.equals(before),"unknown tags preserved");
        System.out.println("CharacterStatSafetyTest: passed");
    }
}
