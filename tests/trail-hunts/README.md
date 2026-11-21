# Caçadas de esqueleto e aranha

No servidor, `/nexuscacadas` ou `/nexuscacadas status` mostra as duas missões. Inicie cada uma com `/nexuscacadas iniciar esqueleto` ou `/nexuscacadas iniciar aranha`. Após iniciar, derrote quatro esqueletos comuns ou quatro aranhas comuns, respectivamente. O golpe final precisa ser atribuído diretamente ao jogador (projéteis atribuídos ao jogador também contam). Mortes com jogador morto, no criativo ou no espectador não contam; essa condição é verificada na captura e na confirmação; variantes e subclasses não contam. As duas missões podem estar ativas juntas.

Cada uma é concluída uma vez por jogador. A de esqueleto dá +2% à velocidade de movimento e a de aranha +1 ponto de armadura, como modificadores temporários reaplicados a partir do progresso salvo. Elas não geram itens nem terreno. Não há verificação de origem natural: mobs de spawner ou criados por outros mecanismos também podem contar em sobrevivência.

A seção de cada missão é separada e versionada. Se os dados estiverem corrompidos, tiverem campos desconhecidos ou forem de outra versão, a missão afetada para de progredir; ela não tenta reparar os dados. Em conflitos no clone, snapshots de recuperação são guardados pelo mecanismo de backup existente. A pessoa que administra o servidor deve fazer backup do mundo antes de qualquer recuperação manual.

## Teste

`bash tests/trail-hunts/run.sh` exige `JAVA_HOME` de JDK 8, `FORGE_MAPPED_JAR` de Forge 1.12.2 mapeado e `DEPS_CP` com dependências. Compila o mod e testa NBT, clonagem e modificadores com classes Forge/Minecraft reais. Não é um teste dentro do jogo: eventos reais de combate, multiplayer, interação com outros mods e integração com a distribuição ainda precisam de teste no Minecraft.
