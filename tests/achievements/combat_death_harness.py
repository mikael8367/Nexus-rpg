#!/usr/bin/env python3
"""Execute production death dispatcher with minimal deterministic Forge-shaped stubs."""
import os, pathlib, subprocess, tempfile
root = pathlib.Path(__file__).resolve().parents[2]
sources = {
'net/minecraft/world/World.java': 'package net.minecraft.world; public class World { public boolean isRemote; }',
'net/minecraft/entity/Entity.java': '''package net.minecraft.entity; import java.util.*; import net.minecraft.world.World; public class Entity { public World world; private UUID id=UUID.randomUUID(); public Entity(World w){world=w;} public UUID getUniqueID(){return id;} }''',
'net/minecraft/entity/EntityLivingBase.java': '''package net.minecraft.entity; import net.minecraft.world.World; public class EntityLivingBase extends Entity { public float health=0, max=20; public EntityLivingBase(World w){super(w);} public float getHealth(){return health;} public float getMaxHealth(){return max;} }''',
'net/minecraft/entity/item/EntityArmorStand.java': 'package net.minecraft.entity.item; import net.minecraft.entity.*; import net.minecraft.world.World; public class EntityArmorStand extends EntityLivingBase { public EntityArmorStand(World w){super(w);} }',
'net/minecraft/entity/monster/EntityZombie.java': 'package net.minecraft.entity.monster; import net.minecraft.entity.*; import net.minecraft.world.World; public class EntityZombie extends EntityLivingBase { public EntityZombie(World w){super(w);} }',
'net/minecraft/entity/monster/EntitySkeleton.java': 'package net.minecraft.entity.monster; import net.minecraft.entity.*; import net.minecraft.world.World; public class EntitySkeleton extends EntityLivingBase { public EntitySkeleton(World w){super(w);} }',
'net/minecraft/entity/monster/EntitySpider.java': 'package net.minecraft.entity.monster; import net.minecraft.entity.*; import net.minecraft.world.World; public class EntitySpider extends EntityLivingBase { public EntitySpider(World w){super(w);} }',
'net/minecraft/entity/player/EntityPlayer.java': 'package net.minecraft.entity.player; import net.minecraft.entity.*; import net.minecraft.world.World; public class EntityPlayer extends EntityLivingBase { public EntityPlayer(World w){super(w);} }',
'net/minecraft/entity/player/EntityPlayerMP.java': '''package net.minecraft.entity.player; import net.minecraft.world.World; import net.minecraft.server.MinecraftServer; public class EntityPlayerMP extends EntityPlayer { public String profile="warrior"; public int xp=0, kills=0; public boolean hunt=true, creative=false, spectator=false, alive=true, skeleton=true, spider=true; public int skeletonKills=0, spiderKills=0; public boolean isCreative(){return creative;} public boolean isSpectator(){return spectator;} public boolean isEntityAlive(){return alive;} public MinecraftServer server; public EntityPlayerMP(World w){super(w); server=new MinecraftServer(this);} public MinecraftServer getServer(){return server;} public Object getEntityData(){return this;} }''',
'net/minecraft/server/MinecraftServer.java': '''package net.minecraft.server; import java.util.UUID; import net.minecraft.entity.player.EntityPlayerMP; public class MinecraftServer { public EntityPlayerMP player; public MinecraftServer(EntityPlayerMP p){player=p;} public MinecraftServer getPlayerList(){return this;} public EntityPlayerMP getPlayerByUUID(UUID id){return player!=null && player.getUniqueID().equals(id)?player:null;} }''',
'net/minecraft/util/DamageSource.java': '''package net.minecraft.util; import net.minecraft.entity.Entity; public class DamageSource { private Entity source; public DamageSource(Entity e){source=e;} public Entity getTrueSource(){return source;} }''',
'net/minecraftforge/event/entity/living/LivingDeathEvent.java': '''package net.minecraftforge.event.entity.living; import net.minecraft.entity.EntityLivingBase; import net.minecraft.util.DamageSource; public class LivingDeathEvent { private EntityLivingBase e; private DamageSource s; private boolean canceled; public LivingDeathEvent(EntityLivingBase e,DamageSource s){this.e=e;this.s=s;} public EntityLivingBase getEntityLiving(){return e;} public DamageSource getSource(){return s;} public boolean isCanceled(){return canceled;} public void setCanceled(boolean b){canceled=b;} }''',
'net/minecraftforge/event/entity/living/LivingDropsEvent.java': '''package net.minecraftforge.event.entity.living; import net.minecraft.entity.EntityLivingBase; import net.minecraft.util.DamageSource; import java.util.*; public class LivingDropsEvent { private EntityLivingBase e; private DamageSource s; public boolean canceled; public List<Object> drops=new ArrayList<Object>(); public LivingDropsEvent(EntityLivingBase e,DamageSource s){this.e=e;this.s=s;} public EntityLivingBase getEntityLiving(){return e;} public DamageSource getSource(){return s;} }''',
'net/minecraftforge/fml/common/Mod.java': '''package net.minecraftforge.fml.common; import java.lang.annotation.*; public class Mod { @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface EventBusSubscriber {String modid();} }''',
'net/minecraftforge/fml/common/eventhandler/EventPriority.java': 'package net.minecraftforge.fml.common.eventhandler; public enum EventPriority { LOWEST }',
'net/minecraftforge/fml/common/eventhandler/SubscribeEvent.java': '''package net.minecraftforge.fml.common.eventhandler; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface SubscribeEvent { EventPriority priority() default EventPriority.LOWEST; boolean receiveCanceled() default false; }''',
'net/minecraftforge/fml/common/gameevent/TickEvent.java': '''package net.minecraftforge.fml.common.gameevent; public class TickEvent {public enum Phase {START,END} public static class ServerTickEvent {public Phase phase; public ServerTickEvent(Phase p){phase=p;} } }''',
'com/nexusrpg/character/CharacterProfile.java': '''package com.nexusrpg.character; import net.minecraft.entity.player.EntityPlayerMP; public class CharacterProfile { public static String characterClass(EntityPlayerMP p){return p.profile;} public static void addXp(EntityPlayerMP p,int n){p.xp+=n;} }''',
'com/nexusrpg/quests/HuntData.java': '''package com.nexusrpg.quests; import net.minecraft.entity.player.EntityPlayerMP; public class HuntData {public static int status(Object p){return ((EntityPlayerMP)p).hunt?1:0;} }''',
'com/nexusrpg/quests/ZombieHunt.java': '''package com.nexusrpg.quests; import java.util.*; import net.minecraft.entity.player.EntityPlayerMP; public class ZombieHunt {static Set<UUID> seen=new HashSet<UUID>(); public static void confirmedZombie(EntityPlayerMP p,UUID id){if(p.hunt && seen.add(id))p.kills++;} }''',
'com/nexusrpg/quests/TrailData.java': '''package com.nexusrpg.quests; import net.minecraft.entity.player.EntityPlayerMP; public class TrailData { public enum Trail {SKELETON,SPIDER} public static int status(Object o, Trail t){EntityPlayerMP p=(EntityPlayerMP)o;return (t==Trail.SKELETON?p.skeleton:p.spider)?1:0;} }''',
'com/nexusrpg/quests/TrailHunts.java': '''package com.nexusrpg.quests; import java.util.*; import net.minecraft.entity.player.EntityPlayerMP; public class TrailHunts {static Map<EntityPlayerMP,Set<UUID>> seen=new HashMap<EntityPlayerMP,Set<UUID>>(); public static void confirmedTrail(EntityPlayerMP p, TrailData.Trail t, UUID id){ if(p.creative||p.spectator||!p.alive||TrailData.status(p,t)!=1)return; Set<UUID> s=seen.get(p);if(s==null){s=new HashSet<UUID>();seen.put(p,s);}if(s.add(id)){if(t==TrailData.Trail.SKELETON)p.skeletonKills++;else p.spiderKills++;}} }''',
'CombatDeathTest.java': '''import com.nexusrpg.achievements.CombatDeathEvents; import net.minecraft.entity.monster.*; import net.minecraft.entity.player.*; import net.minecraft.world.*; import net.minecraft.util.*; import net.minecraftforge.event.entity.living.*; import net.minecraftforge.fml.common.eventhandler.*; import net.minecraftforge.fml.common.gameevent.TickEvent;
public class CombatDeathTest {
 static void ck(boolean v,String s){if(!v)throw new AssertionError(s);} static void tick(){CombatDeathEvents.serverTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));}
 static LivingDeathEvent death(net.minecraft.entity.EntityLivingBase v, DamageSource s){LivingDeathEvent e=new LivingDeathEvent(v,s);CombatDeathEvents.death(e);return e;} static void drops(net.minecraft.entity.EntityLivingBase v,DamageSource s){CombatDeathEvents.drops(new LivingDropsEvent(v,s));}
 static class SubSkeleton extends EntitySkeleton{SubSkeleton(World w){super(w);}}
 public static void main(String[] a)throws Exception{
 ck(CombatDeathEvents.class.getMethod("death",LivingDeathEvent.class).getAnnotation(SubscribeEvent.class).receiveCanceled(),"receive canceled for late uncancel");
 World w=new World();EntityPlayerMP p=new EntityPlayerMP(w);DamageSource s=new DamageSource(p);
 EntitySkeleton sk=new EntitySkeleton(w);death(sk,s);drops(sk,s);tick();ck(p.skeletonKills==0,"deferred");tick();ck(p.skeletonKills==1,"confirmed");death(sk,s);drops(sk,s);tick();tick();ck(p.skeletonKills==1,"dedupe");
 EntitySpider sp=new EntitySpider(w);LivingDeathEvent e=death(sp,s);e.setCanceled(true);drops(sp,s);tick();tick();ck(p.spiderKills==0,"late cancel");
 EntitySpider ok=new EntitySpider(w);death(ok,s);drops(ok,s);tick();tick();ck(p.spiderKills==1,"spider");
 EntitySkeleton wrong=new EntitySkeleton(w);death(wrong,s);drops(wrong,new DamageSource(p));tick();tick();ck(p.skeletonKills==1,"source mismatch");
 EntitySkeleton revived=new EntitySkeleton(w);death(revived,s);drops(revived,s);revived.health=1;tick();tick();ck(p.skeletonKills==1,"revived");
 SubSkeleton sub=new SubSkeleton(w);death(sub,s);drops(sub,s);tick();tick();ck(p.skeletonKills==1,"subclass");
 p.creative=true;EntitySkeleton creative=new EntitySkeleton(w);death(creative,s);drops(creative,s);p.creative=false;tick();tick();ck(p.skeletonKills==1,"creative ineligible");
 EntitySkeleton offline=new EntitySkeleton(w);death(offline,s);drops(offline,s);p.server.player=null;tick();tick();ck(p.skeletonKills==1,"offline excluded");
 ck(p.xp==0&&p.kills==0,"dispatcher leaves xp/zombie alone");System.out.println("PASS trail integration; existing handlers untouched");
 }
}'''
}
with tempfile.TemporaryDirectory(prefix='nexus-death-') as tmp:
    d=pathlib.Path(tmp)
    for name, content in sources.items():
        file=d/name; file.parent.mkdir(parents=True,exist_ok=True); file.write_text(content)
    javahome=pathlib.Path(os.environ['JAVA_HOME'])
    prod=root/'src/main/java/com/nexusrpg/achievements'
    subprocess.run([str(javahome/'bin/javac'),'-source','8','-target','8','-d',str(d)]+[str(d/name) for name in sources]+[str(prod/'PendingDeaths.java'),str(prod/'CombatDeathEvents.java')],check=True)
    subprocess.run([str(javahome/'bin/java'),'-cp',str(d),'CombatDeathTest'],check=True)
