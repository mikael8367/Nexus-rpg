package com.nexusrpg.character;

import net.minecraft.nbt.NBTTagCompound;

/** Regression: damaged selected profiles must be rejected without altering their NBT. */
public final class CharacterProfileValidationTest {
    private static void check(boolean b,String message) { if(!b) throw new AssertionError(message); }
    private static NBTTagCompound valid() {
        NBTTagCompound n=new NBTTagCompound(); n.setString("class","mage");
        n.setInteger("level",4); n.setInteger("xp",2); n.setInteger("points",3); n.setInteger("mana",10);
        return n;
    }
    private static void rejected(NBTTagCompound n,String label) {
        NBTTagCompound before=n.copy();
        check(!CharacterProfile.validData(n),label); check(n.equals(before),label+" changed");
    }
    public static void main(String[] args) {
        check(CharacterProfile.validData(new NBTTagCompound()),"unselected empty profile");
        check(CharacterProfile.validData(valid()),"selected complete profile");
        for(String key:new String[]{"level","xp","points","mana"}) {
            NBTTagCompound n=valid(); n.removeTag(key); rejected(n,"missing "+key);
            n=valid(); n.setString(key,"recover me"); rejected(n,"wrong type "+key);
        }
        NBTTagCompound n=valid(); n.setInteger("xp",-1); rejected(n,"negative xp");
        n=valid(); n.setInteger("level",0); rejected(n,"level zero");
        n=valid(); n.setInteger("stat_speed",10001); rejected(n,"stat overflow");
        n=valid(); n.setString("class","future_class"); rejected(n,"unknown class preserved");
        n=valid(); n.setString("future_data","preserve"); NBTTagCompound before=n.copy();
        check(CharacterProfile.validData(n) && n.equals(before),"unknown extra tags allowed and preserved");
        check(!CharacterProfile.hasValidSelectedProfile(null),"missing player rejected");
        System.out.println("CharacterProfileValidationTest: success");
    }
}
