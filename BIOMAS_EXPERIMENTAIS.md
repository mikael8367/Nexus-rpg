# Seis biomas experimentais — NÃO habilitados

**Estado: protótipos parciais, não conteúdo jogável.** Há seis definições de bioma registráveis e projetos puros para árvores e estruturas; não há distribuição natural, implementação de colocação no mundo ou gerador de recursos. Os biomas não aparecem espontaneamente nem em mundo novo comum. Não anunciar os desenhos como árvores/estruturas já geradas. Não há mobs novos.

| Registro | Clima e terreno | Projeto de árvore | Projeto de estrutura |
|---|---|---|---|
| `nexusrpg:amber_steppe` | estepe quente | acácia larga | monólitos de arenito |
| `nexusrpg:mistwood` | bosque úmido | bétula estreita | arco de pedra musgosa |
| `nexusrpg:crystal_peaks` | picos frios com neve | pinheiro em camadas | espira de gelo |
| `nexusrpg:red_oasis` | seco, areia/arenito | frondes de selva | pilares rubros |
| `nexusrpg:twilight_fen` | brejo quente | carvalho escuro em guarda-chuva | altar de prismarina |
| `nexusrpg:ash_plateau` | planalto seco, cascalho/pedra | carvalho esparso | chaminé de tijolo do Nether |

São apenas plantas abstratas de blocos; folhas e troncos ainda precisam de estados e propriedades apropriados. Existe diferenciação de clima, relevo, cores e, em dois casos, superfície. A decoração do bioma está vazia e a classe de planos não é usada no jogo. Nenhum dos seis é adicionado ao `BiomeManager`/listas climáticas. Um gerador externo ou outro mod pode selecionar um bioma registrado por conta própria: a proteção não controla código de terceiros. O registro ocupa seis IDs e pode falhar em modpacks sem espaço de registro; mesmo sem distribuição, não é mudança isenta de riscos.

## Revisão e motivo do bloqueio

O bytecode mapeado do Forge 14.23.5.2860 mostra que `ChunkGeneratorOverworld.populate` chama `Biome.decorate`, e que a decoração vanilla usa posições `nextInt(16)+8`, eventos `TerrainGen.decorate` e geradores que podem alcançar chunks vizinhos. Acrescentar os biomas à distribuição, mesmo sem retrogeração direta, alteraria chunks novos na borda de mundos antigos e exporia decoração e geradores de outros mods. Não foi possível provar a proteção completa, inclusive contra chunks já salvos parcialmente populados, estruturas de mods, posição dos jogadores e cancelamento durante escrita. Por isso nenhuma distribuição ou escrita foi conectada. As alterações não modificam nem ativam a torre gigante; os demais geradores preexistentes continuam como estavam e **não foram auditados neste trabalho**.

`FeatureBlueprint.plan` é puro: limita cada plano a um único chunk com margem, não acessa mundo. `preflight` é apenas um ensaio contra uma interface hipotética: exige chunk carregado, fronteira, bioma e ar sem TileEntity e aceita cancelamento. Isso NÃO detecta propriedade de jogadores ou todas as estruturas, não valida apoio/fundação, não oferece atomicidade, não escuta eventos Forge reais e não tem rollback; jamais usar o resultado como autorização para escrever blocos. A mera substituibilidade não prova segurança. Não confundir essa simulação com proteção testada no Minecraft.

## Testes realizados

`tests/newbiomes/run.sh` compila em Java 8 sem Gradle: 420.645 verificações de plano/preflight em 7 sementes fixas (inclusive extremos), 81 pares de chunks positivos/negativos/limites por estilo, além de 256 sementes pseudoaleatórias por estilo em alturas extremas; determinismo, cardinalidade, posição/margem, variantes, entradas inválidas, ar ocupado, TileEntity, fronteira, chunk indisponível, bioma diferente e cancelamento em cada célula. O teste de TileEntity usa um fake, não instâncias reais. Teste adicional usa stubs para simular registro de seis IDs, clima e decoração vazia; uma varredura estática verifica ausência de integração. Compilação isolada do registro contra o jar Forge 1.12.2 mapeado real passou. Não houve inicialização real do Forge, teste dentro do Minecraft, teste de salvamento/carregamento, comparação de chunks ou teste de outros mods. Testes de cancelamento são de preflight, não de eventos Forge nem escrita parcial.

## Para preservar mundos e avançar

Não instalar protótipos em mundo importante; manter o mod anterior, cópia integral fechada do mundo e cópias da lista de mods/configurações. Para desenvolver, testar primeiro em cópia isolada descartável, sem remover registro já usado em mundos experimentais. Antes de habilitar é necessário projetar e testar um adaptador de estados e escrita transacional com callbacks reais, chunks novos verificáveis (inclusive reinício e população incompleta), proteção de estruturas e jogadores, revalidação antes de cada escrita, cancelamento, falhas/rollback, interações de vizinhos/TileEntities, limites do mundo, compatibilidade de outros geradores, integração de biomas novos e teste real em mundos novos e cópias com fronteiras e estruturas de jogadores. Não alegar garantia de preservação apenas com os testes atuais.
