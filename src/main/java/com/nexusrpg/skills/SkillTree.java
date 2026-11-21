package com.nexusrpg.skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Definições imutáveis compartilhadas entre servidor e interface. IDs são persistentes. */
public final class SkillTree {
    public static final class Requirement {
        public final String nodeId;
        public final int rank;
        private Requirement(String nodeId, int rank) { this.nodeId = nodeId; this.rank = rank; }
    }
    public static final class Node {
        public final String id, characterClass, name, description, kind;
        public final int x, y, maxRank, cost, minLevel;
        public final List<Requirement> requirements;
        private Node(String id, String c, String name, String description, String kind,
                     int x, int y, int maxRank, int cost, int minLevel, Requirement... requirements) {
            this.id=id; this.characterClass=c; this.name=name; this.description=description; this.kind=kind;
            this.x=x; this.y=y; this.maxRank=maxRank; this.cost=cost; this.minLevel=minLevel;
            List<Requirement> copy=new ArrayList<>(); Collections.addAll(copy,requirements);
            this.requirements=Collections.unmodifiableList(copy);
        }
    }
    private static final Map<String,Node> NODES;
    static {
        Map<String,Node> n=new LinkedHashMap<>();
        addClass(n,"warrior","Guerreiro","Ímpeto de guerra","+3% de dano por grau na passiva corpo a corpo.","Golpe reforçado","+1 de dano por grau na ativa.","Bastião de guerra","+2 de dano e 2 segundos de resistência na suprema por grau.");
        addClass(n,"mage","Mago","Estudo arcano","+3% de dano por grau quando a passiva de inteligência se aplica.","Concentração arcana","+1 de dano por grau na ativa. !skill fire causa dano de fogo sem incendiar.","Ascensão arcana","+2 de dano e 2 segundos de regeneração na suprema por grau.");
        addClass(n,"archer","Arqueiro","Olho certeiro","+3% de dano por grau em projéteis contra hostis.","Flecha veloz","+1 de dano por grau na ativa.","Tempestade de flechas","+2 de dano e 2 segundos de velocidade na suprema por grau.");
        addClass(n,"assassin","Assassino","Oportunismo","+3% de dano por grau contra hostis com pouca vida. Esquiva inata: 15% contra ataques comuns hostis; recarga de 400 ticks após sucesso. Não bloqueia fogo, magia, explosões ou dano ambiental.","Lâmina precisa","+1 de dano por grau na ativa.","Véu mortal","+2 de dano e 2 segundos de invisibilidade na suprema por grau.");
        addClass(n,"paladin","Paladino","Luz punitiva","+3% de dano por grau na passiva contra mortos-vivos hostis.","Bênção vital","+1 de cura por grau na ativa.","Santuário","+2 de cura e 2 segundos de resistência na suprema por grau.");
        addClass(n,"necromancer","Necromante","Roubo de vida","+0,5 de cura por grau na passiva de roubo de vida.","Dreno sombrio","+1 de dano e 0,5 de cura por grau na ativa.","Colheita de almas","+2 de dano por grau e 1 de cura por grau na suprema.");
        NODES=Collections.unmodifiableMap(n);
    }
    private static void addClass(Map<String,Node> m,String c,String label,String passive,String passiveDesc,String active,String activeDesc,String ultimate,String ultimateDesc) {
        String root=c+".training", p=c+".passive", a=c+".active", u=c+".ultimate";
        add(m,new Node(root,c,"Aperfeiçoamento: "+label,"Aperfeiçoamento original: melhora ativa, suprema e passiva conforme a classe.","training",0,0,5,1,1));
        add(m,new Node(p,c,passive,passiveDesc,"passive",-1,1,3,1,4,new Requirement(root,1)));
        add(m,new Node(a,c,active,activeDesc,"active",1,1,3,1,4,new Requirement(root,1)));
        add(m,new Node(u,c,ultimate,ultimateDesc,"ultimate",0,2,2,2,15,new Requirement(p,1),new Requirement(a,2)));
    }
    private static void add(Map<String,Node> m,Node n) { if(m.put(n.id,n)!=null) throw new IllegalStateException(n.id); }
    public static Node node(String id) { return NODES.get(id); }
    public static List<Node> nodes(String characterClass) {
        List<Node> result=new ArrayList<>();
        for(Node n:NODES.values()) if(n.characterClass.equals(characterClass)) result.add(n);
        return Collections.unmodifiableList(result);
    }
    public static List<Node> allNodes() { return Collections.unmodifiableList(new ArrayList<>(NODES.values())); }
    private SkillTree() {}
}
