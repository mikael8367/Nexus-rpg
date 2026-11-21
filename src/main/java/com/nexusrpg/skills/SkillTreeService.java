package com.nexusrpg.skills;

import com.nexusrpg.character.CharacterProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

/** API somente no servidor, sempre na thread principal do Minecraft. */
public final class SkillTreeService {
    private static final String TREE = "tree_v1";
    private SkillTreeService() {}

    private static boolean server(EntityPlayerMP p) {
        return p!=null && p.world!=null && !p.world.isRemote && p.getServer()!=null
            && p.getServer().isCallingFromMinecraftThread();
    }
    private static boolean profileValid(EntityPlayerMP p) {
        return CharacterProfile.hasValidSelectedProfile(p);
    }
    static boolean valid(NBTTagCompound data) {
        if(data==null || (data.hasKey(TREE) && !data.hasKey(TREE,10))) return false;
        NBTTagCompound tree=data.getCompoundTag(TREE);
        for(SkillTree.Node node:SkillTree.allNodes()) {
            boolean legacy="training".equals(node.kind);
            NBTTagCompound owner=legacy?data:tree;
            String key=legacy?"rank_"+node.characterClass:node.id;
            if(owner.hasKey(key) && (!owner.hasKey(key,3) || owner.getInteger(key)<0 || owner.getInteger(key)>node.maxRank)) return false;
        }
        // Purchases cannot produce a child without its ancestors. Keep the bytes for recovery.
        for (SkillTree.Node node : SkillTree.allNodes()) {
            if (rank(data, node) == 0) continue;
            for (SkillTree.Requirement requirement : node.requirements)
                if (rank(data, SkillTree.node(requirement.nodeId)) < requirement.rank) return false;
        }
        return true;
    }
    // Fail closed on malformed runtime fields without coercing or overwriting them.
    static boolean runtimeValid(NBTTagCompound data) {
        if (!valid(data)) return false;
        for (String c : new String[]{"warrior", "mage", "archer", "assassin", "paladin", "necromancer"}) {
            for (String prefix : new String[]{"cast_", "ultimate_", "combo_start_", "combo_until_"})
                if (!nonnegativeLong(data, prefix + c)) return false;
            String target = "combo_target_" + c;
            if (data.hasKey(target) && !data.hasKey(target, 8)) return false;
        }
        return nonnegativeLong(data, "assassin_dodge_ready_v1") && nonnegativeLong(data, "leech_ready") && nonnegativeLong(data, "pal_combo_start")
            && nonnegativeLong(data, "pal_combo_until");
    }
    private static boolean nonnegativeLong(NBTTagCompound data, String key) {
        return !data.hasKey(key) || (data.hasKey(key, 4) && data.getLong(key) >= 0);
    }
    static long deadline(long tick, long duration) {
        return tick > Long.MAX_VALUE - duration ? Long.MAX_VALUE : tick + duration;
    }
    static long secondsRemaining(long ready, long tick) {
        long remaining = ready - tick; // both nonnegative and ready > tick
        return remaining / 20 + (remaining % 20 == 0 ? 0 : 1);
    }
    static int rank(NBTTagCompound data, SkillTree.Node node) {
        if(data==null || node==null) return 0;
        if("training".equals(node.kind)) return Math.max(0,Math.min(node.maxRank,data.getInteger("rank_"+node.characterClass)));
        return Math.max(0,Math.min(node.maxRank,data.getCompoundTag(TREE).getInteger(node.id)));
    }
    static int spent(NBTTagCompound data) {
        int total=0;
        for(SkillTree.Node n:SkillTree.allNodes()) total+=rank(data,n)*n.cost;
        return total;
    }
    static int available(NBTTagCompound data,int level) {
        return Math.max(0,(Math.max(1,level)-1)/3-spent(data));
    }
    /** Snapshot NBT independente: ok, reason, class, level, points e ranks (id -> int). */
    public static NBTTagCompound snapshot(EntityPlayerMP p) {
        NBTTagCompound out=new NBTTagCompound();
        if(!server(p) || !profileValid(p)) { out.setBoolean("ok",false); out.setString("reason","Perfil indisponível ou chamada fora do servidor."); return out; }
        NBTTagCompound data=SkillEvents.data(p);
        if(!valid(data)) { out.setBoolean("ok",false); out.setString("reason","Dados de habilidades incompatíveis; nenhum dado foi alterado."); return out; }
        String c=CharacterProfile.characterClass(p); int level=CharacterProfile.level(p);
        out.setBoolean("ok",true); out.setString("class",c); out.setInteger("level",level); out.setInteger("points",available(data,level));
        NBTTagCompound ranks=new NBTTagCompound();
        for(SkillTree.Node n:SkillTree.nodes(c)) ranks.setInteger(n.id,rank(data,n));
        out.setTag("ranks",ranks); return out;
    }
    /** Solicita a ativa (false) ou suprema (true); retorno e falhas são informados ao jogador no chat. */
    public static void activate(EntityPlayerMP p, boolean ultimate) {
        if (!server(p) || !profileValid(p) || !p.isEntityAlive() || p.isSpectator()) return;
        SkillEvents.activateServer(p,ultimate,false);
    }
    /** Ataque de fogo do mago, com a mesma mana e recarga da ativa normal. */
    public static void activateFire(EntityPlayerMP p) {
        if (!server(p) || !profileValid(p) || !p.isEntityAlive() || p.isSpectator()) return;
        SkillEvents.activateServer(p,false,true);
    }
    /** Retorna texto em português; a string vazia significa sucesso. Nunca aceita rank, custo ou classe do cliente. */
    public static String allocate(EntityPlayerMP p,String nodeId) {
        if(!server(p) || !profileValid(p) || !p.isEntityAlive() || p.isSpectator()) return "Perfil ou jogador indisponível.";
        return allocateChecked(SkillEvents.data(p), CharacterProfile.characterClass(p), CharacterProfile.level(p), nodeId);
    }
    // Regra única testável, invocada apenas após verificação do jogador na API pública.
    static String allocateChecked(NBTTagCompound data,String characterClass,int level,String nodeId) {
        if(nodeId==null || nodeId.length()>100) return "Habilidade desconhecida.";
        SkillTree.Node n=SkillTree.node(nodeId);
        if(n==null || !n.characterClass.equals(characterClass)) return "Habilidade desconhecida para sua classe.";
        if(!valid(data)) return "Dados de habilidades incompatíveis; nenhum dado foi alterado.";
        int current=rank(data,n);
        if(current>=n.maxRank) return "Habilidade já está no máximo.";
        if(level<n.minLevel) return "Exige nível " + n.minLevel + ".";
        for(SkillTree.Requirement r:n.requirements) if(rank(data,SkillTree.node(r.nodeId))<r.rank) return "Exige " + SkillTree.node(r.nodeId).name + " grau " + r.rank + ".";
        if(available(data,level)<n.cost) return "Pontos de habilidade insuficientes.";
        if("training".equals(n.kind)) data.setInteger("rank_"+n.characterClass,current+1);
        else {
            if(!data.hasKey(TREE)) data.setTag(TREE,new NBTTagCompound());
            data.getCompoundTag(TREE).setInteger(n.id,current+1);
        }
        return "";
    }
}
