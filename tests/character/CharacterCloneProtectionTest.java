package com.nexusrpg.character;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/** Checks clone conflict detection against real Minecraft NBT, not a game integration test. */
public final class CharacterCloneProtectionTest {
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
    public static void main(String[] args) {
        NBTTagCompound dst = new NBTTagCompound();
        check(!CharacterEvents.hasProtectedProfile(dst), "absent persisted data");
        dst.setString(EntityPlayer.PERSISTED_NBT_TAG, "unexpected");
        check(CharacterEvents.hasProtectedProfile(dst), "malformed persisted data");
        NBTTagCompound persisted = new NBTTagCompound();
        dst.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        persisted.setString("other_mod", "keep");
        check(!CharacterEvents.hasProtectedProfile(dst), "other mod data alone");
        persisted.setInteger(CharacterProfile.KEY, 123);
        check(CharacterEvents.hasProtectedProfile(dst), "malformed profile");
        NBTTagCompound profile = new NBTTagCompound();
        persisted.setTag(CharacterProfile.KEY, profile);
        check(!CharacterEvents.hasProtectedProfile(dst), "empty profile");
        profile.setString("class", "mage");
        check(CharacterEvents.hasProtectedProfile(dst), "existing profile");
        profile.setString("class", "future_class");
        check(CharacterEvents.hasProtectedProfile(dst), "unknown profile");
        check("keep".equals(persisted.getString("other_mod")), "other data unchanged");
        System.out.println("CharacterCloneProtectionTest: passed");
    }
}
