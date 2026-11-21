# Jornadas automáticas experimentais

Duas jornadas independentes ficam automaticamente disponíveis, sem comandos, em sobrevivência/aventura. O chat informa os objetivos e o progresso ao entrar e quando um objetivo é atingido.

* Explorador: no Mundo normal, estar em Y de 0 a 24, em Y de 110 a 255 e dentro de água em bioma classificado OCEAN. A verificação de posição ocorre uma vez por segundo; não importa se as estruturas ou líquidos foram colocados por jogadores. Completar os três concede permanentemente +2% velocidade.
* Artífice: retirar o resultado da fabricação de uma picareta de ferro, um escudo e uma bússola. Receitas alternativas de mods que resultem no mesmo item também contam; resultados de máquinas que não emitam o evento de fabricação de jogador não contam. Completar os três concede permanentemente +1 armadura.

Os bônus são modificadores não salvos e idempotentes, reconstruídos do progresso salvo, e acumulam com outros bônus do mod. As jornadas não retroagem para atividades anteriores à instalação e não contam criativo, espectador, jogador morto ou FakePlayer. Progresso separado em PlayerPersisted, com versão e bits validados; dados desconhecidos são preservados e não progridem. Ao clonar, conflitos geram backups existentes do personagem. Nenhuma caça existente é modificada.

Teste com `JAVA_HOME`, `FORGE_MAPPED_JAR`, `DEPS_CP` e `bash tests/journeys/run.sh`. Usa Java 8 e API Forge/Minecraft mapeada real, e preserva arquivos temporários. Exercita NBT comprimido, clonagem, progresso, limites de localização, bônus idempotente, evento de fabricação não cancelável e regressões de caçadas/conquistas. Não substitui testes em Minecraft: despacho de eventos, fabricação real, multiplayer, dimensões, outros mods, empacotamento e reobfuscação permanecem pendentes. O evento de crafting é lançado ao retirar o resultado, antes de processar ingredientes restantes; modificações ou interrupções externas posteriores não podem ser garantidas como transações atômicas.
