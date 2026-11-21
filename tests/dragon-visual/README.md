# Visual de sopro dos dragões

O helper `DragonBreathVisual` é independente e ainda não está ligado ao combate. Não cria projéteis: envia partículas vanilla a jogadores próximos via `WorldServer.spawnParticle`, apenas no servidor. Não consulta blocos ou chunks, nem altera dano ou terreno. Limita a 12 pontos, dois envios de duas partículas por ponto e distância de 16 blocos entre as extremidades. As configurações de partículas e distância do cliente podem afetar a visualização.

## Integração sugerida

Em `EntityElementalDragon.elementalStrike`, imediatamente depois de atribuir `attackCooldown`, antes de chamar `attackEntityFrom`, acrescentar:

```java
if (!melee) DragonBreathVisual.show(this, target);
```

Ambas as classes ficam no mesmo pacote, sem import. Isso representa a tentativa de ataque à distância inclusive se o dano for cancelado; melee não é elemental na implementação original. Manter a validação prévia de alvo, alcance, chunks e visibilidade, e chamar só no thread do servidor. Outra alteração concorrente pode mudar a localização apropriada ou a lógica do voo: integrar e revisar no código final, não copiar cegamente.

## Testes

```sh
JAVA_HOME=/home/daytona/workspace/nexus-test/jdk python3 tests/dragon-visual/run.py
```

Compila a classe real com fakes e checa tipos e quantidade de partículas, coordenadas, espalhamento, metadata da poeira, 500 geometrias aleatórias e entradas inválidas. Os fakes não substituem teste real em clientes, rede ou servidor dedicado; o teste não verifica a chamada de integração. A compilação `compileJava` do projeto foi executada separadamente com Forge 1.12.2 e JDK 8.
