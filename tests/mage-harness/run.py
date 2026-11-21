#!/usr/bin/env python3
"""Exercise the real fireball against minimal stubs; does not launch Minecraft."""
from pathlib import Path
import os, subprocess, tempfile
root = Path(__file__).resolve().parents[2]
sources = {
'net/minecraft/world/World.java': '''package net.minecraft.world; public class World { public boolean isRemote; }''',
'net/minecraft/world/WorldServer.java': '''package net.minecraft.world; import net.minecraft.util.EnumParticleTypes; import net.minecraft.entity.*; import java.util.*; public class WorldServer extends World { public final Map<UUID,Entity> entities=new HashMap<>(); public Entity getEntityFromUuid(UUID id){return entities.get(id);} public void spawnParticle(EnumParticleTypes p,double x,double y,double z,int n,double a,double b,double c,double d,int... args){} }''',
'net/minecraft/util/EnumParticleTypes.java': '''package net.minecraft.util; public enum EnumParticleTypes {FLAME}''',
'net/minecraft/util/DamageSource.java': '''package net.minecraft.util; import net.minecraft.entity.*; public class DamageSource { public Entity source; public EntityLivingBase caster; public boolean projectile,fire; public static DamageSource causeIndirectMagicDamage(Entity e,EntityLivingBase c){DamageSource d=new DamageSource();d.source=e;d.caster=c;return d;} public DamageSource setProjectile(){projectile=true;return this;} public DamageSource setFireDamage(){fire=true;return this;} }''',
'net/minecraft/entity/Entity.java': '''package net.minecraft.entity; import net.minecraft.world.World; import net.minecraft.util.DamageSource; public class Entity { public World world; public boolean isDead; public double posX,posY,posZ; public float damage; public int fire; public DamageSource source; public boolean accepts=true; public Entity(World w){world=w;} public void setDead(){isDead=true;} public boolean attackEntityFrom(DamageSource d,float n){source=d;damage+=accepts?n:0;return accepts;} public void setFire(int s){fire=s;} }''',
'net/minecraft/entity/EntityLivingBase.java': '''package net.minecraft.entity; import net.minecraft.world.World; import java.util.UUID; public class EntityLivingBase extends Entity {private final UUID id=UUID.randomUUID(); public EntityLivingBase(World w){super(w);} public UUID getUniqueID(){return id;} }''',
'net/minecraft/nbt/NBTTagCompound.java': '''package net.minecraft.nbt; import java.util.*; public class NBTTagCompound { private final Map<String,Object> tags=new HashMap<>(); public void setString(String k,String v){tags.put(k,v);} public String getString(String k){return tags.containsKey(k)?(String)tags.get(k):"";} public void setInteger(String k,int v){tags.put(k,v);} public int getInteger(String k){return tags.containsKey(k)?(Integer)tags.get(k):0;} }''',
'net/minecraft/util/math/RayTraceResult.java': '''package net.minecraft.util.math; import net.minecraft.entity.Entity; public class RayTraceResult {public Entity entityHit; public RayTraceResult(Entity e){entityHit=e;} }''',
'net/minecraft/entity/projectile/EntityThrowable.java': '''package net.minecraft.entity.projectile; import net.minecraft.entity.*; import net.minecraft.world.*; import java.util.UUID; import net.minecraft.nbt.NBTTagCompound; import net.minecraft.util.math.RayTraceResult; public abstract class EntityThrowable extends Entity {protected EntityLivingBase thrower; private String owner; public int superUpdates; public RayTraceResult simulatedHit; public EntityThrowable(World w){super(w);} public EntityThrowable(World w,EntityLivingBase e){super(w);thrower=e;} protected float getGravityVelocity(){return 0.03F;} public void onUpdate(){superUpdates++; if(simulatedHit!=null) onImpact(simulatedHit);} public void writeEntityToNBT(NBTTagCompound tag){tag.setString("ownerName",owner==null?"":owner);} public void readEntityFromNBT(NBTTagCompound tag){owner=tag.getString("ownerName"); thrower=null; getThrower();} public EntityLivingBase getThrower(){if(thrower==null && owner!=null && !owner.isEmpty() && world instanceof WorldServer) {try {Entity e=((WorldServer)world).getEntityFromUuid(UUID.fromString(owner)); if(e instanceof EntityLivingBase) thrower=(EntityLivingBase)e;} catch(IllegalArgumentException ignored){}} return thrower;} protected abstract void onImpact(RayTraceResult hit); }''',
'com/nexusrpg/mage/FireballHarness.java': '''package com.nexusrpg.mage;
import net.minecraft.world.*; import net.minecraft.entity.*; import net.minecraft.util.math.RayTraceResult; import net.minecraft.nbt.NBTTagCompound;
public class FireballHarness {
 static void check(boolean v,String m){if(!v) throw new AssertionError(m);}
 public static void main(String[] a){
  WorldServer w=new WorldServer(); EntityLivingBase caster=new EntityLivingBase(w), target=new EntityLivingBase(w);
  EntityMageFireball f=new EntityMageFireball(w,caster); check(f.getGravityVelocity()==0,"gravity"); f.onImpact(new RayTraceResult(target));
  check(f.isDead && target.damage==7 && target.fire==3,"hit and fire"); check(target.source.caster==caster && target.source.source==f && target.source.fire && target.source.projectile,"credit");
  EntityLivingBase immune=new EntityLivingBase(w); immune.accepts=false; f=new EntityMageFireball(w,caster); f.onImpact(new RayTraceResult(immune)); check(immune.fire==0,"immune target must not ignite");
  f=new EntityMageFireball(w,caster); f.onImpact(new RayTraceResult(null)); check(f.isDead,"block impact");
  f=new EntityMageFireball(w,caster); f.onImpact(new RayTraceResult(caster)); check(caster.damage==0 && caster.fire==0,"self hit");
  World client=new World(); client.isRemote=true; EntityLivingBase c=new EntityLivingBase(client), t=new EntityLivingBase(client); f=new EntityMageFireball(client,c); f.onImpact(new RayTraceResult(t)); check(t.damage==0 && !f.isDead,"client does not apply impact");
  f=new EntityMageFireball(w,caster); for(int i=0;i<79;i++)f.onUpdate(); check(!f.isDead,"expires early"); NBTTagCompound n=new NBTTagCompound(); f.writeEntityToNBT(n); check(n.getString("ownerName").equals(caster.getUniqueID().toString()),"mob owner persistence"); check(n.getInteger("NexusMageFlightTicks")==79,"lifetime persisted"); EntityMageFireball loaded=new EntityMageFireball(w); loaded.readEntityFromNBT(n); loaded.onUpdate(); check(loaded.isDead,"restored lifetime");
  EntityLivingBase victim=new EntityLivingBase(w); f=new EntityMageFireball(w,caster); n=new NBTTagCompound(); f.writeEntityToNBT(n);
  loaded=new EntityMageFireball(w); loaded.readEntityFromNBT(n); check(loaded.getThrower()==null,"unloaded caster unresolved");
  NBTTagCompound n2=new NBTTagCompound(); loaded.writeEntityToNBT(n2); check(n2.getString("ownerName").equals(caster.getUniqueID().toString()),"unresolved owner kept on resave");
  w.entities.put(caster.getUniqueID(),caster); check(loaded.getThrower()==caster,"late loaded owner resolves"); loaded.onImpact(new RayTraceResult(victim)); check(victim.damage==7 && victim.source.caster==caster,"loaded caster credited"); loaded.onImpact(new RayTraceResult(victim)); check(victim.damage==7,"duplicate hit ignored");
  n2.setInteger("NexusMageFlightTicks",80); loaded=new EntityMageFireball(w); loaded.readEntityFromNBT(n2); loaded.simulatedHit=new RayTraceResult(victim); loaded.onUpdate(); check(loaded.isDead && loaded.superUpdates==0 && victim.damage==7,"expired projectile does not move or collide");
  EntityMageFireball noOwner=new EntityMageFireball(w); noOwner.onImpact(new RayTraceResult(victim)); check(victim.damage==7 && noOwner.isDead,"unknown owner fails closed");
  System.out.println("PASS: damage, immunity, block and self hit, duplicate impact, client, owner roundtrip, delayed resolution, fail-closed owner, lifetime");
 }
}'''
}
with tempfile.TemporaryDirectory(prefix='mage-test-') as td:
    td=Path(td); src=td/'src'; classes=td/'classes'; classes.mkdir()
    files=[]
    for name, text in sources.items():
        p=src/name; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text); files.append(str(p))
    files.append(str(root/'src/main/java/com/nexusrpg/mage/EntityMageFireball.java'))
    home=os.environ.get('JAVA_HOME'); javac=str(Path(home)/'bin/javac') if home else 'javac'; java=str(Path(home)/'bin/java') if home else 'java'
    subprocess.run([javac,'-source','8','-target','8','-d',str(classes)]+files,check=True)
    subprocess.run([java,'-cp',str(classes),'com.nexusrpg.mage.FireballHarness'],check=True)
