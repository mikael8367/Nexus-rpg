# Integração isolada: IDs 15 e 16

Base: auditoria remota c5cdc74. Estas classes agora foram registradas e possuem ovos, mas ainda não foram testadas no jogo. Não alteram geração, blocos, NBT ou torre. Os mobs são hostis terrestres com perseguição e ataque corpo a corpo padrão; não aplicam efeitos extras. Não há spawn natural. Ao integrar, verificar colisões de IDs na versão final, especialmente com trabalhos paralelos.

* **Escavador das Dunas / Dune Burrower**: besouro hexápode com mandíbulas; 32 de vida, velocidade 0,23, dano 5; saque 1–2 areia e 25% chance de sílex.
* **Guardião das Raízes / Root Warden**: guardião de madeira bípede com braços e galhos; 40 de vida, velocidade 0,20, dano 6; saque 1–2 gravetos e 25% chance de sementes de trigo.

## Registro do servidor e ovos (integrado)
Adicionar dentro de `MobRegistration.register`, após confirmar IDs livres (imports existentes incluem as classes por estarem no mesmo pacote):
```java
event.getRegistry().register(EntityEntryBuilder.<EntityBurrower>create().entity(EntityBurrower.class).id(new ResourceLocation("nexusrpg", "dune_burrower"), 15).name("dune_burrower").tracker(64, 3, true).egg(0x875B2E, 0x28DCE1).build());
event.getRegistry().register(EntityEntryBuilder.<EntityRootwarden>create().entity(EntityRootwarden.class).id(new ResourceLocation("nexusrpg", "root_warden"), 16).name("root_warden").tracker(64, 3, true).egg(0x4E3A24, 0x4C6F38).build());
```

## Registro cliente (integrado)
Adicionar dentro de `MobClientRegistration.register` (imports wildcard e modelos no mesmo pacote já existentes):
```java
RenderingRegistry.registerEntityRenderingHandler(EntityBurrower.class, m -> new MobRenderer<EntityBurrower>(m, new ModelBurrower(), "dune_burrower"));
RenderingRegistry.registerEntityRenderingHandler(EntityRootwarden.class, m -> new MobRenderer<EntityRootwarden>(m, new ModelRootwarden(), "root_warden"));
```

## Traduções (integradas)
Adicionar em `src/main/resources/assets/nexusrpg/lang/pt_br.lang`:
```properties
entity.dune_burrower.name=Escavador das Dunas
entity.root_warden.name=Guardião das Raízes
```
Adicionar em `src/main/resources/assets/nexusrpg/lang/en_us.lang`:
```properties
entity.dune_burrower.name=Dune Burrower
entity.root_warden.name=Root Warden
```
Texturas `.png.b64` são decodificadas pelo `processResources` do Gradle. A integração ainda precisa de build do mod e teste no Minecraft, com ovos, modelos, animações, saque e IA.

## Testes próprios
`tests/creatures1516/check.py` exige Java 8, cache Forge mapeado e Pillow; compila as classes novas e testa modelos, UV e carregamento das tabelas com classes reais. `tests/creatures1516/behavior.py` testa atributos, tarefas, IDs e delegação de ataque usando dublês. Ambos exigem um caminho de saída novo e não apagam diretórios ou caches; não substituem teste em jogo. O bootstrap isolado pode avisar da ausência de recursos vanilla no classpath; os pools das duas criaturas são verificados individualmente.
