package net.minecraft.nbt;
import java.util.*;
public class NBTTagCompound {
 private final Map<String,Object> values = new HashMap<>();
 public Set<String> getKeySet(){return values.keySet();}
 public boolean hasKey(String s,int type){Object v=values.get(s);return (type==10&&v instanceof NBTTagCompound)||(type==11&&v instanceof int[]);}
 public NBTTagCompound getCompoundTag(String s){return (NBTTagCompound)values.get(s);}
 public boolean getBoolean(String s){return true;}
 public int[] getIntArray(String s){return (int[])values.get(s);}
 public void put(String s,Object o){values.put(s,o);}
}
