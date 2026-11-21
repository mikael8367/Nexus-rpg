#!/usr/bin/env python3
"""Compile the actual RestEvents against small Forge-shaped fakes and exercise its gates.
This is not an in-game or multiplayer integration test.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
JAVA = Path(os.environ.get('JAVA_HOME', '/home/daytona/workspace/nexus-test/jdk')) / 'bin'
STUBS = {
 'net/minecraft/util/math/BlockPos.java': '''package net.minecraft.util.math; public class BlockPos {}''',
 'net/minecraft/potion/Potion.java': '''package net.minecraft.potion; public class Potion {}''',
 'net/minecraft/init/MobEffects.java': '''package net.minecraft.init; import net.minecraft.potion.Potion; public class MobEffects { public static final Potion REGENERATION = new Potion(); }''',
 'net/minecraft/potion/PotionEffect.java': '''package net.minecraft.potion; public class PotionEffect { public final Potion potion; public final int duration, amplifier; public final boolean ambient, particles; public PotionEffect(Potion p,int d,int a,boolean amb,boolean part) {potion=p;duration=d;amplifier=a;ambient=amb;particles=part;} }''',
 'net/minecraft/util/text/TextComponentString.java': '''package net.minecraft.util.text; public class TextComponentString { public final String text; public TextComponentString(String s) {text=s;} }''',
 'net/minecraft/block/Block.java': '''package net.minecraft.block; import net.minecraft.block.state.IBlockState; import net.minecraft.world.World; import net.minecraft.util.math.BlockPos; import net.minecraft.entity.player.EntityPlayer; public class Block { public boolean bed=true; public boolean isBed(IBlockState s,World w,BlockPos p,EntityPlayer e) {return bed;} }''',
 'net/minecraft/block/state/IBlockState.java': '''package net.minecraft.block.state; import net.minecraft.block.Block; public class IBlockState { public Block block=new Block(); public Block getBlock(){return block;} }''',
 'net/minecraft/world/World.java': '''package net.minecraft.world; import net.minecraft.util.math.BlockPos; import net.minecraft.block.state.IBlockState; public class World { public boolean isRemote=false, loaded=true; public int reads=0; public IBlockState state=new IBlockState(); public boolean isBlockLoaded(BlockPos p){return loaded;} public IBlockState getBlockState(BlockPos p){reads++; if(!loaded) throw new AssertionError("forced chunk"); return state;} }''',
 'net/minecraft/entity/player/EntityPlayer.java': '''package net.minecraft.entity.player; import net.minecraft.world.World; import net.minecraft.util.math.BlockPos; import net.minecraft.potion.*; import net.minecraft.util.text.TextComponentString; public class EntityPlayer { public World world=new World(); public BlockPos bedLocation=new BlockPos(); public boolean alive=true, asleep=true, immune=false; public PotionEffect effect; public int adds=0, messages=0; public boolean isEntityAlive(){return alive;} public boolean isPlayerFullyAsleep(){return asleep;} public boolean isPotionActive(Potion p){return effect!=null && effect.potion==p;} public void addPotionEffect(PotionEffect e){adds++; if(!immune) effect=e;} public void sendStatusMessage(TextComponentString t,boolean b){messages++;} }''',
 'net/minecraftforge/event/entity/player/PlayerWakeUpEvent.java': '''package net.minecraftforge.event.entity.player; import net.minecraft.entity.player.EntityPlayer; public class PlayerWakeUpEvent { public EntityPlayer p; public boolean spawn=true, immediate=false; public PlayerWakeUpEvent(EntityPlayer p){this.p=p;} public EntityPlayer getEntityPlayer(){return p;} public boolean shouldSetSpawn(){return spawn;} public boolean wakeImmediately(){return immediate;} }''',
 'net/minecraftforge/fml/common/eventhandler/SubscribeEvent.java': '''package net.minecraftforge.fml.common.eventhandler; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface SubscribeEvent {}''',
 'RestEventsTest.java': r'''import com.nexusrpg.rest.RestEvents;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;

public final class RestEventsTest {
  static RestEvents handler = new RestEvents();
  static void check(boolean ok,String what) { if (!ok) throw new AssertionError(what); }
  static EntityPlayer player(){return new EntityPlayer();}
  static PlayerWakeUpEvent event(EntityPlayer p){return new PlayerWakeUpEvent(p);}
  static void reject(EntityPlayer p,PlayerWakeUpEvent e,String name) {
    handler.onWakeUp(e); check(p.adds==0 && p.messages==0,name);
  }
  public static void main(String[] args) {
    EntityPlayer p=player(); PlayerWakeUpEvent e=event(p);
    handler.onWakeUp(e);
    check(p.adds==1 && p.messages==1 && p.effect != null,"success");
    check(p.effect.potion==MobEffects.REGENERATION && p.effect.duration==200 && p.effect.amplifier==0 && !p.effect.ambient && p.effect.particles,"vanilla potion parameters");
    handler.onWakeUp(e); check(p.adds==1 && p.messages==1,"duplicate event");
    EntityPlayer other=player(); handler.onWakeUp(event(other)); check(other.adds==1 && p.adds==1,"per player isolation");
    p=player(); p.world.isRemote=true; reject(p,event(p),"client");
    p=player(); p.alive=false; reject(p,event(p),"dead");
    p=player(); p.asleep=false; reject(p,event(p),"not fully asleep");
    p=player(); e=event(p); e.spawn=false; reject(p,e,"not successful sleep");
    p=player(); e=event(p); e.immediate=true; reject(p,e,"interrupted wake");
    p=player(); p.bedLocation=null; reject(p,event(p),"no bed"); check(p.world.reads==0,"no null bed lookup");
    p=player(); p.world.loaded=false; reject(p,event(p),"unloaded bed"); check(p.world.reads==0,"no chunk load");
    p=player(); p.world.state.block.bed=false; reject(p,event(p),"bed removed");
    p=player(); PotionEffect existing=new PotionEffect(MobEffects.REGENERATION,5000,2,false,true); p.effect=existing; reject(p,event(p),"existing regeneration"); check(p.effect==existing,"existing effect untouched");
    p=player(); p.immune=true; handler.onWakeUp(event(p)); check(p.adds==1 && p.effect==null && p.messages==0,"potion refusal");
    System.out.println("RestEventsTest: 14 checks passed (stubbed Forge, not in-game)");
  }
}'''
}
with tempfile.TemporaryDirectory(prefix='nexusrpg-rest-test-') as temp:
    base = Path(temp)
    for name, text in STUBS.items():
        path = base / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf-8')
    (base / 'classes').mkdir()
    sources = [str(p) for p in base.rglob('*.java')]
    sources.append(str(ROOT / 'src/main/java/com/nexusrpg/rest/RestEvents.java'))
    subprocess.run([str(JAVA/'javac'), '-encoding','UTF-8','-source','8','-target','8','-d',str(base/'classes')] + sources, check=True)
    subprocess.run([str(JAVA/'java'), '-cp',str(base/'classes'), 'RestEventsTest'], check=True)
