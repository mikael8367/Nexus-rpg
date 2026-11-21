# Exploração (Forge 1.12.2)

Esta expansão adiciona três construções pequenas de superfície: uma masmorra com três seções, divisória central e passagens variáveis, uma ruína de castelo com muralha parcialmente danificada e uma torre iluminada decorativa. Cada uma contém um baú com a tabela vanilla `minecraft:chests/simple_dungeon`; na masmorra, o baú fica na seção mais distante da entrada. São plantas limitadas e não são castelos, masmorras subterrâneas ou torres completas de múltiplos andares. A seleção, as passagens e os danos variam deterministicamente por semente e chunk.

## Onde aparecem

- Apenas no Overworld do servidor, quando a opção de gerar estruturas do mundo está ativa, durante a população de chunks novos. Chunks que chegaram pelo evento de carregamento do disco são descartados (inclusive chunks antigos com população incompleta). Não há retrogen nem substituição de biomas ou alteração das outras estruturas.
- Chance inicial 1/180 por chunk; validações estritas reduzem muito a frequência real. A construção ocupa 10×10 blocos, com área de inspeção de 12×12 dentro do mesmo chunk, no máximo 9 blocos acima do solo. Solo precisa ser plano, de uma lista restrita de blocos naturais; todo o espaço acima deve ser ar. Árvores, flores, neve e terreno irregular impedem a geração.
- Todas as colunas da área são inspecionadas até o fundo do mundo e blocos desconhecidos ou entidades de bloco rejeitam o local. Minérios vanilla e os três minérios naturais do Nexus são permitidos abaixo do solo e não são modificados; não é possível distingui-los de blocos iguais colocados por jogadores. Os limites conhecidos das estruturas vanilla salvos (vila, mina, templo, fortaleza, monumento e mansão) também são verificados. Entidades na área, borda do mundo e altura são verificadas.
- Somente ar recebe blocos novos. A planta e cada escrita são validadas; falhas tentam reverter apenas blocos exatamente iguais aos colocados pelo gerador, sem remover entidades de bloco ou baús com inventário. Uma falha após criar um baú com entidade de bloco pode deixar uma construção parcial para preservar dados.
- A inspeção é conservadora, não uma garantia absoluta: não é possível identificar construções feitas apenas de blocos naturais ou caixas de estruturas de outros mods que não exponham dados compatíveis. Outros mods também podem alterar o mundo depois da geração; callbacks de terceiros podem interferir na reversão. Faça backup e teste em mundo separado antes de usar um mundo importante.

## Não implementado

Vilas medievais funcionais, cavernas especiais, dimensão nova, portais funcionais e eventos aleatórios não estão implementados. Não existe ID de dimensão nem portal inativo fingindo ser funcional. Baús secretos com mecanismo de segredo também não foram implementados; o baú da masmorra é apenas parcialmente oculto pela parede. Uma dimensão exige identificação persistente e compatibilidade entre instalações; escavação subterrânea não consegue distinguir com segurança terreno natural de construções de jogadores. Esses recursos não foram arriscados sem testes reais adequados.

## Testes

`compileJava` com JDK 8 e Forge 1.12.2; execute `sh tests/exploration-harness/run.sh` para testar plantas, acesso ao baú, registro simulado, colisões e rollback. O Forge 1.12.2 inscreve automaticamente a classe anotada antes de publicar o evento de registro de blocos; esse evento registra um gerador de peso 2. Os testes usam um mundo e um registro falsos e **não** substituem teste real no Minecraft, em mundo novo e em cópias de mundos existentes. O projeto também contém um teste separado das 50 estruturas: `sh tests/structures-harness/run.sh`.
