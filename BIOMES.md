# Biomas originais do Nexus RPG

Implementação inicial para Minecraft Java 1.12.2 / Forge 14.23.5.2860.

| Bioma | ID interno | Chave |
|---|---|---|
| Bosque das Runas | `enchanted_grove` | Verde |
| Coroa Boreal | `frost_crown` | Ciano |
| Império Soterrado | `sunken_empire` | Amarela |
| Charco das Bruxas | `witching_mire` | Roxa |
| Coração de Cinzas | `cinder_heart` | Vermelha |
| Santuário Rosado | `rosa_sanctum` | Rosa |
| Ermos do Umbral | `umbral_reach` | Preta |
| Dossel dos Titãs | `titan_canopy` | Laranja |

Os biomas são registrados como biomas próprios do mod, com parâmetros de clima, relevo e cor de água definidos pelo Nexus RPG. As chaves também são itens próprios e aparecem na aba Materiais do criativo.

Os oito biomas são adicionados às listas climáticas do Forge para geração natural no Overworld, com peso 4 por bioma, sem remover os biomas existentes. Recebem também tipos do Biome Dictionary para compatibilidade. A disponibilidade depende do gerador de mundo: tipos personalizados, mundos de bioma único e a opção vanilla antiga podem ignorar parte dessas listas. A distribuição afeta novos chunks; chunks já gerados não são recriados automaticamente.

O peso 4 é relativo às entradas do mesmo clima, não uma chance de 4% por chunk. No Forge 1.12.2, o sorteio é feito pelas camadas do gerador e passa por outras transformações, como rios e bordas; a distância até um bioma varia com a semente. Adicionar entradas ao BiomeManager marca as listas como modificadas, portanto o Forge considera pesos que não são múltiplos de 10. O modo de mundo antigo (`DEFAULT_1_1`) substitui a lista desértica do Forge; os dois biomas Nexus desse clima podem não aparecer nele.

A aparência usa relevo e cores, neve na Coroa Boreal, areia no Império Soterrado, terreno pedregoso nos ermos e decoração com blocos e árvores vanilla. Os biomas não têm blocos ou vegetação exclusivos por enquanto.

## Como procurar e diagnosticar

Use um mundo novo de tipo **Padrão**, com o Forge e o mod instalados. Em um mundo anterior, explore território ainda não gerado; os chunks antigos mantêm seus biomas. Na edição Java 1.12.2 não existe o comando vanilla `/locatebiome`. Uma opção é instalar [Nature's Compass para Forge 1.12.2](https://www.curseforge.com/minecraft/mc-mods/natures-compass/files/2893527), escolher o bioma Nexus e pesquisar as coordenadas. Uma busca sem resultado não prova que o bioma não existe: considere o alcance configurado, o tipo do mundo e a semente.

Ao carregar o Overworld no servidor, o Nexus registra no log o provedor de biomas e, para cada bioma, sua presença no registro, clima, quantidade de entradas e peso atual no BiomeManager, além do ID e tipos quando disponíveis. Avisos apontam registro, entrada, peso ou tipos ausentes. Isso não verifica chunks e não garante que um gerador personalizado respeite as listas. Os dados refletem o momento do carregamento; outro mod pode alterá-los. Não executa busca de chunks nem os modifica. Os logs ajudam a diagnosticar, mas não substituem um teste no jogo.

## Estruturas e saques

Cada bioma pode gerar uma pequena estrutura diferente, com um baú de suprimentos e tesouros adequados ao local:

| Bioma | Estrutura |
|---|---|
| Bosque das Runas | Anel rúnico musgoso |
| Coroa Boreal | Obeliscos de gelo |
| Império Soterrado | Ruínas de arenito |
| Charco das Bruxas | Cabana aberta |
| Coração de Cinzas | Forja em ruínas |
| Santuário Rosado | Santuário de quartzo |
| Ermos do Umbral | Portal de obsidiana |
| Dossel dos Titãs | Plataforma com postes |

Os baús podem conter itens vanilla, lingotes do mod e, com chance de 25%, a chave da cor do bioma. As chaves ainda não abrem masmorras. As estruturas são tentadas raramente em chunks novos do Overworld, em locais planos e desocupados; árvores, outros blocos ou estruturas próximas podem impedir a geração. O gerador não carrega chunks vizinhos. Mundos e geradores personalizados podem alterar a distribuição. Faça backup antes de experimentar num mundo existente: a integração ainda não foi testada dentro do jogo.
