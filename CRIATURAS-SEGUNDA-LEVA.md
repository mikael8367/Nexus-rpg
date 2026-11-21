# Segunda leva experimental de criaturas

Base: `aba088117bc36fe9c1fe8e37e69e5e1c9a0a1e53` da branch remota `auditoria-seguranca-rpg` (obtida antes da implementação). Etapa experimental, sem validação em jogo. A meta de 50 criaturas não está completa.

* **Pernalta dos Juncos**, `nexusrpg:reed_stilt`, ID local 10: ave terrestre de pernas e pescoço longos, duas asas e cauda. Vida 30, velocidade 0,28, dano 4; loot de 1–2 penas e 25% de chance de uma semente de trigo.
* **Tartaruga Prismática**, `nexusrpg:prism_tortoise`, ID local 11: casco em camadas com três cristais, quatro patas e cauda. Vida 36, velocidade 0,19, dano 6; loot de 1–2 quartzos e 25% de chance de uma pepita de ouro.

Ambas possuem modelos e texturas 64×64 diferentes, animações, ovos criativos e traduções PT/EN. São monstros terrestres com a mesma IA vanilla de melee, perseguição, revide, nado e passeio, mas atributos diferentes. Não aplicam efeitos adicionais após ataques, nem afetam blocos. Sem spawn natural: testar pelos ovos criativos ou `/summon nexusrpg:reed_stilt` e `/summon nexusrpg:prism_tortoise`. Sem sons próprios. IDs devem ser revisados novamente se outra branch for mesclada.

## Verificação sem Gradle

Requer Python com Pillow, Java 8 e cache mapeado Forge 1.12.2-14.23.5.2860 snapshot 20171003:

```
python3 tests/mobs/check.py
JAVA_HOME=/home/daytona/workspace/nexus-test/jdk python3 tests/mobs/behavior.py /caminho/novo-para-teste
JAVA_HOME=/home/daytona/workspace/nexus-test/jdk GRADLE_USER_HOME=/home/daytona/workspace/nexus-test/gradle python3 tests/mobs/compile_mapped.py /outro/caminho/novo
```

Os diretórios de saída precisam ser novos; nada é apagado. O último comando compila todas as fontes com JDK 8, instancia modelos reais e carrega loot real via API mapeada isolada. O bootstrap deste teste registra avisos/erros por recursos vanilla ausentes, não representa um jogo completo. O teste de comportamento usa substitutos simulados. Não há teste real de Minecraft, servidor dedicado ou distribuição; checar colisões, animações, aparência, nome/ovo, registro e loot no jogo antes de publicar. O script `create_stilt_tortoise.py` se recusa a sobrescrever qualquer arquivo existente. O `processResources` do projeto transforma `.png.b64` em PNG durante uma build; essa etapa não foi executada aqui.
