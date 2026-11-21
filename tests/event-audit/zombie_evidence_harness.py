#!/usr/bin/env python3
"""Runs production evidence handler with deterministic stubs, not Minecraft/Forge runtime.
Requires JAVA_HOME and ZOMBIE_TEST_DIR (a new, non-existent directory). Deletes nothing.
"""
import ast, os, pathlib, subprocess
root = pathlib.Path(__file__).resolve().parents[2]
old = ast.parse((root/'tests/achievements/combat_death_harness.py').read_text())
sources = next(ast.literal_eval(n.value) for n in old.body if isinstance(n, ast.Assign) and any(isinstance(t, ast.Name) and t.id == 'sources' for t in n.targets))
sources.pop('CombatDeathTest.java')
sources['ZombieEvidenceTest.java'] = r'''
import com.nexusrpg.achievements.ZombieDeathEvidence;
import net.minecraft.entity.monster.*; import net.minecraft.entity.player.*; import net.minecraft.world.*; import net.minecraft.util.*; import net.minecraftforge.event.entity.living.*; import net.minecraftforge.fml.common.eventhandler.*; import net.minecraftforge.fml.common.gameevent.TickEvent;
public class ZombieEvidenceTest {
 static void ck(boolean x,String m){if(!x)throw new AssertionError(m);} static void tick(){ZombieDeathEvidence.serverTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));} static void twice(){tick();tick();}
 static LivingDeathEvent death(EntityZombie z,DamageSource s){LivingDeathEvent e=new LivingDeathEvent(z,s);ZombieDeathEvidence.death(e);return e;}
 static void drops(EntityZombie z,DamageSource s,boolean canceled){LivingDropsEvent e=new LivingDropsEvent(z,s);e.canceled=canceled;ZombieDeathEvidence.drops(e);ck(e.canceled==canceled && e.drops.size()==0,"drops unchanged");}
 static class SubZombie extends EntityZombie { SubZombie(World w){super(w);} }
 public static void main(String[] a)throws Exception {
 ck(ZombieDeathEvidence.class.getMethod("death",LivingDeathEvent.class).getAnnotation(SubscribeEvent.class).receiveCanceled(),"accept previously cancelled events");
 ck(ZombieDeathEvidence.class.getMethod("drops",LivingDropsEvent.class).getAnnotation(SubscribeEvent.class).receiveCanceled(),"accept canceled drops");
 World w=new World(); EntityPlayerMP p=new EntityPlayerMP(w); DamageSource s=new DamageSource(p);
 EntityZombie z=new EntityZombie(w); LivingDeathEvent ev=death(z,s); ck(p.kills==0,"no immediate side effect"); drops(z,s,false); tick();ck(p.kills==0,"deferred");tick();ck(p.kills==1,"confirmed");death(z,s);drops(z,s,false);twice();ck(p.kills==1,"duplicate UUID");
 EntityZombie canceled=new EntityZombie(w);ev=death(canceled,s);ev.setCanceled(true);twice();ck(p.kills==1,"late cancellation with no drops");
 EntityZombie later=new EntityZombie(w);ev=death(later,s);drops(later,s,false);ev.setCanceled(true);twice();ck(p.kills==1,"late cancellation even after drops");
 EntityZombie already=new EntityZombie(w);ev=new LivingDeathEvent(already,s);ev.setCanceled(true);ZombieDeathEvidence.death(ev);drops(already,s,false);twice();ck(p.kills==1,"already canceled");
 EntityZombie restored=new EntityZombie(w);ev=new LivingDeathEvent(restored,s);ev.setCanceled(true);ZombieDeathEvidence.death(ev);ev.setCanceled(false);drops(restored,s,true);twice();ck(p.kills==2,"uncancel then canceled empty drops still count");
 EntityZombie mismatch=new EntityZombie(w);death(mismatch,s);drops(mismatch,new DamageSource(p),false);twice();ck(p.kills==2,"different source identity");
 EntityZombie revived=new EntityZombie(w);death(revived,s);drops(revived,s,false);revived.health=1;twice();ck(p.kills==2,"revived");
 EntityZombie nan=new EntityZombie(w);death(nan,s);drops(nan,s,false);nan.health=Float.NaN;twice();ck(p.kills==2,"NaN health fails closed");
 SubZombie sub=new SubZombie(w);death(sub,s);drops(sub,s,false);twice();ck(p.kills==2,"zombie subclasses excluded");
 p.hunt=false;EntityZombie inactive=new EntityZombie(w);death(inactive,s);p.hunt=true;drops(inactive,s,false);twice();ck(p.kills==2,"must be active at event");
 EntityZombie inactiveLater=new EntityZombie(w);death(inactiveLater,s);drops(inactiveLater,s,false);p.hunt=false;twice();p.hunt=true;ck(p.kills==2,"must be active at recording");
 EntityZombie off=new EntityZombie(w);death(off,s);drops(off,s,false);p.server.player=null;twice();p.server.player=p;ck(p.kills==2,"offline");
 EntityZombie dimension=new EntityZombie(w);death(dimension,s);drops(dimension,s,false);p.world=new World();twice();p.world=w;ck(p.kills==2,"changed world");
 World client=new World();client.isRemote=true;p.world=client;EntityZombie remote=new EntityZombie(client);death(remote,s);drops(remote,s,false);twice();p.world=w;ck(p.kills==2,"client");
 ZombieDeathEvidence.serverTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
 EntityZombie first=new EntityZombie(w);death(first,s);drops(first,s,false);LivingDeathEvent bad=death(first,new DamageSource(p));bad.setCanceled(true);twice();ck(p.kills==3,"confirmed candidate not overwritten");
 int before=p.kills;for(int i=0;i<513;i++){EntityZombie q=new EntityZombie(w);death(q,s);drops(q,s,false);}twice();ck(p.kills-before==512,"queue limit fails closed");
 System.out.println("PASS production zombie evidence handler, mocked collaborators and event ordering; NOT a Forge runtime test");
 }
}
'''
out = pathlib.Path(os.environ['ZOMBIE_TEST_DIR'])
out.mkdir(parents=True, exist_ok=False)
for name, body in sources.items():
    p=out/name; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(body)
jdk=pathlib.Path(os.environ['JAVA_HOME'])
prod=root/'src/main/java/com/nexusrpg/achievements'
subprocess.run([str(jdk/'bin/javac'),'-source','8','-target','8','-d',str(out)]+[str(out/name) for name in sources]+[str(prod/'PendingDeaths.java'),str(prod/'ZombieDeathEvidence.java')],check=True)
subprocess.run([str(jdk/'bin/java'),'-cp',str(out),'ZombieEvidenceTest'],check=True)
