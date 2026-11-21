package com.nexusrpg.skills;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;

/** Verifica regras reais de compra, migração sem escrita e preservação de NBT. */
public final class SkillTreeTest {
    private static void check(boolean ok,String label) { if(!ok) throw new AssertionError(label); }
    private static void buy(NBTTagCompound d,String c,int level,String id) {
        String result=SkillTreeService.allocateChecked(d,c,level,id);
        check(result.isEmpty(),id+": "+result);
    }
    private static void reject(NBTTagCompound d,String c,int level,String id) {
        NBTTagCompound before=d.copy();
        check(!SkillTreeService.allocateChecked(d,c,level,id).isEmpty(),"rejeitar "+id);
        check(before.equals(d),"rejeição não pode alterar dados: "+id);
    }
    public static void main(String[] args) {
        String[] classes={"warrior","mage","archer","assassin","paladin","necromancer"};
        Set<String> ids=new HashSet<>();
        check(SkillTree.allNodes().size()==24,"24 nós");
        for(String c:classes) {
            check(SkillTree.nodes(c).size()==4,"4 nós em "+c);
            for(SkillTree.Node n:SkillTree.nodes(c)) {
                check(ids.add(n.id),"id único");
                check(n.cost>0 && n.maxRank>0 && n.name.length()>0 && n.description.length()>0,"metadados");
                for(SkillTree.Requirement r:n.requirements)
                    check(SkillTree.node(r.nodeId)!=null && SkillTree.node(r.nodeId).characterClass.equals(c) && r.rank<=SkillTree.node(r.nodeId).maxRank,"ligação válida");
            }
            NBTTagCompound d=new NBTTagCompound();
            d.setLong("cast_"+c,123); d.setString("outra_chave","preservada");
            NBTTagCompound initial=d.copy();
            check(SkillTreeService.valid(d),"formato legado");
            check(SkillTreeService.available(d,16)==5 && initial.equals(d),"leitura não migra nem altera");
            reject(d,c,4,c+".active"); reject(d,c,4,c+".passive");
            reject(d,c,1,c+".training"); reject(d,c,100,"outro.active"); reject(d,c,100,"mage.invalid");
            buy(d,c,4,c+".training");
            check(SkillTreeService.available(d,4)==0,"sem gasto duplo");
            reject(d,c,4,c+".training");
            buy(d,c,7,c+".passive"); buy(d,c,10,c+".active"); buy(d,c,13,c+".active");
            reject(d,c,14,c+".ultimate"); reject(d,c,15,c+".ultimate");
            buy(d,c,19,c+".ultimate");
            check(SkillTreeService.rank(d,SkillTree.node(c+".ultimate"))==1,"compra da suprema");
            check(SkillTreeService.available(d,19)==0,"custo 2 descontado");
            buy(d,c,25,c+".ultimate"); reject(d,c,100,c+".ultimate");
            buy(d,c,100,c+".active"); reject(d,c,100,c+".active");
            check(d.getLong("cast_"+c)==123 && d.getString("outra_chave").equals("preservada"),"recarga e dados preservados");
            NBTTagCompound save=d.copy();
            check(SkillTreeService.spent(save)==SkillTreeService.spent(d),"persistência por cópia NBT");
        }
        NBTTagCompound old=new NBTTagCompound(); old.setInteger("rank_warrior",3); old.setInteger("rank_mage",1); old.setLong("ultimate_warrior",99);
        NBTTagCompound backup=old.copy();
        check(SkillTreeService.available(old,16)==1 && SkillTreeService.rank(old,SkillTree.node("warrior.training"))==3,"pontos e grau legado");
        check(old.equals(backup),"migração virtual idempotente");
        check(SkillTreeService.available(old,16)==1 && old.equals(backup),"migração repetida");
        buy(old,"warrior",16,"warrior.passive");
        check(old.getInteger("rank_warrior")==3 && old.getInteger("rank_mage")==1 && old.getLong("ultimate_warrior")==99,"sem reset");
        reject(old,"warrior",16,"warrior.active");
        NBTTagCompound malformed=new NBTTagCompound(); malformed.setString("tree_v1","não substitua"); reject(malformed,"warrior",100,"warrior.training");
        malformed=new NBTTagCompound(); malformed.setString("rank_mage","não substitua"); reject(malformed,"warrior",100,"warrior.training");
        malformed=new NBTTagCompound(); NBTTagCompound tree=new NBTTagCompound(); tree.setInteger("warrior.active",999); malformed.setTag("tree_v1",tree); reject(malformed,"warrior",100,"warrior.training");
        malformed=new NBTTagCompound(); malformed.setInteger("rank_mage",-1); reject(malformed,"warrior",100,"warrior.training");
        malformed=new NBTTagCompound(); tree=new NBTTagCompound(); tree.setInteger("warrior.ultimate",1);
        malformed.setTag("tree_v1",tree); reject(malformed,"warrior",100,"warrior.training");
        check(!SkillTreeService.runtimeValid(malformed),"ramo sem pré-requisitos bloqueia ativação");
        // Runtime tags are not allowed to bypass cooldowns or be overwritten by casting.
        String[] longs={"cast_warrior","ultimate_mage","combo_start_archer","combo_until_assassin",
            "pal_combo_start","pal_combo_until","leech_ready","assassin_dodge_ready_v1"};
        for (String key:longs) {
            NBTTagCompound bad=new NBTTagCompound(); bad.setString(key,"preserve"); NBTTagCompound before=bad.copy();
            check(!SkillTreeService.runtimeValid(bad) && before.equals(bad),"tipo inválido preservado: "+key);
            bad=new NBTTagCompound(); bad.setLong(key,-1); before=bad.copy();
            check(!SkillTreeService.runtimeValid(bad) && before.equals(bad),"valor inválido preservado: "+key);
        }
        NBTTagCompound timers=new NBTTagCompound(); timers.setInteger("combo_target_warrior", 3);
        check(!SkillTreeService.runtimeValid(timers),"tipo de alvo inválido");
        timers=new NBTTagCompound(); timers.setInteger("rank_warrior", 2); timers.setLong("cast_warrior", 20);
        timers.setString("combo_target_warrior", "alvo");
        check(SkillTreeService.runtimeValid(timers),"dados legados válidos");
        check(SkillTreeService.secondsRemaining(21, 1)==1 && SkillTreeService.secondsRemaining(22, 1)==2,
            "arredondamento da recarga");
        check(SkillTreeService.secondsRemaining(Long.MAX_VALUE, 0)==461168601842738791L,"recarga sem overflow");
        check(SkillTreeService.deadline(Long.MAX_VALUE-2, 200)==Long.MAX_VALUE,"prazo saturado");
        check(!SkillTreeService.allocate(null,"warrior.training").isEmpty(),"não aceita jogador ausente");
        System.out.println("SkillTreeTest: sucesso (6 classes, migração, pré-requisitos, gastos, limites e NBT)");
    }
}
