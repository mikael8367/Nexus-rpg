package net.minecraft.nbt;
import java.util.*;
public class NBTTagCompound {
 public final Map<String,Object> entries=new HashMap<String,Object>();
 public Set<String> getKeySet(){return entries.keySet();}
 public boolean hasKey(String s,int i){Object o=entries.get(s);return i==10 ? o instanceof NBTTagCompound : i==11 && o instanceof int[];}
 public NBTTagCompound getCompoundTag(String s){Object o=entries.get(s);return o instanceof NBTTagCompound ? (NBTTagCompound)o : new NBTTagCompound();}
 public boolean getBoolean(String s){return true;}
 public int[] getIntArray(String s){Object o=entries.get(s);return o instanceof int[] ? (int[])o : new int[0];}
}
