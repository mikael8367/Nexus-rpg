# Criaturas — primeira leva experimental

Implementadas **3 de 50 pretendidas**, não 50. São inimigos terrestres para enfrentar, não raças selecionáveis pelo jogador. Forge 1.12.2; registro com ovos criativos e IDs locais 7–9, seguindo os IDs 0–6 já ocupados na base 6a9f9d0.

| Criatura | Registro / ID | Anatomia | Vida / dano base |
| --- | --- | --- | --- |
| Cão Espinheiro | `nexusrpg:bramble_hound` / 7 | Quadrúpede com focinho, orelhas, cauda e espinhos | 28 / 5 |
| Rastejador das Cavernas | `nexusrpg:cavern_crawler` / 8 | Corpo baixo, carapaça, mandíbulas e seis pernas | 20 / 4 |
| Guardião de Basalto | `nexusrpg:basalt_guardian` / 9 | Bípede pétreo de ombros largos e um olho | 42 / 7 |

Cada um tem ModelBase próprio, textura 64×64 própria, ovo e animação de locomoção. São hostis e usam IA vanilla de nado, busca de jogadores, combate corpo a corpo, revide, caminhada e observação; não há habilidades especiais. Podem ser colocados pelos ovos do criativo ou `/summon` com o identificador acima. A classe EntityMob faz os monstros desaparecerem no modo pacífico. Não há spawn natural, loot, sons próprios, progressão ou integração com biomas, missões e estruturas. O renderer e os modelos ficam em pacote exclusivo do cliente; o registro comum não os referencia.

## Testes e limites

* `tests/mobs/check.py`: checagens estáticas de IDs, ovos, isolamento cliente, traduções e PNGs (Python e Pillow).
* `tests/mobs/ModelChecks.java`: instancia os modelos reais da API mapeada, verifica UVs nos limites e animação normal sem contexto OpenGL.
* Fontes compiladas com Java 8 contra Forge 1.12.2 mapeado 14.23.5.2860 e Guava 21; não é um build Gradle nem um JAR distribuível.
* Ainda não foram executados testes reais no jogo ou num servidor dedicado: registro/ovos, comportamento de IA, colisão, visuais, iluminação e empacotamento precisam de validação no ambiente Forge real. Testes estáticos não provam o carregamento de servidor.
* `create_mobs_local.py` regenera as três texturas usando Pillow e **sobrescreve** esses três arquivos; não executá-lo sobre texturas editadas sem antes preservá-las.
