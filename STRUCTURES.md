# Estruturas de superfície (Forge 1.12.2)

Cada um dos oito biomas mantém sua construção original e recebe os projetos abaixo. O número é um identificador interno, não um comando. Os nomes descrevem a forma visual; não são novas estruturas subterrâneas ou sistemas de jogo.

## Enchanted Grove (estilo 0)
0. Arco quebrado
1. Torre de vigia em ruínas
2. Santuário em ruínas
3. Colunata caída
4. Obelisco de beira de caminho
5. Pátio em ruínas
6. Ponte quebrada

## Frost Crown (estilo 1)
7. Marco funerário de pedras
8. Cabana abandonada
9. Refúgio abandonado
10. Ruínas de portaria
11. Pedras erguidas
12. Abside em ruínas
13. Pavilhão

## Sunken Empire (estilo 2)
14. Zigurate quebrado
15. Cripta de superfície
16. Bastião de superfície
17. Colunata desmoronada
18. Torre de vigia quebrada
19. Anfiteatro em ruínas

## Witching Mire (estilo 3)
20. Portal em arco fraturado
21. Santuário circular aberto
22. Biblioteca caída
23. Cabana abandonada de telhado inclinado
24. Oficina abandonada de telhado inclinado
25. Cripta de superfície

## Cinder Heart (estilo 4)
26. Labirinto de superfície
27. Pirâmide escalonada em ruínas
28. Aqueduto quebrado
29. Pátio em ruínas
30. Base de moinho em ruínas
31. Observatório estrelado

## Rosa Sanctum (estilo 5)
32. Abrigo de eremita
33. Torres gêmeas em ruínas
34. Arco quebrado
35. Torre de vigia em ruínas
36. Claustro em ruínas
37. Aqueduto caído

## Umbral Reach (estilo 6)
38. Santuário em ruínas
39. Canto de fortaleza em ruínas
40. Cabana abandonada de telhado inclinado
41. Casa abandonada de telhado inclinado
42. Cripta aberta de superfície
43. Câmara abobadada de superfície

## Titan Canopy (estilo 7)
44. Círculo de pedras
45. Pátio do obelisco
46. Anfiteatro em ruínas
47. Forja sem telhado
48. Portal em ruínas
49. Pagode escalonado

## Geração e limites

- Somente Overworld, nos oito biomas correspondentes. A tentativa inicial foi aumentada de 1 em 80 para **1 em 8 por chunk candidato** (12,5%), antes de verificar bioma, terreno e proteção. Isso não é a chance final de construção: o resultado depende da semente e pode ser bem menor. A seleção uniforme entre o projeto original, os projetos daquele bioma e, quando disponível, a torre de mago ocorre depois das verificações de terreno (9 opções no estilo 0; 8 nos estilos 1, 3 e 6; 7 nos demais). Uma falha de validação não tenta outro projeto.
- O gerador está registrado no preInit do mod. Não há configuração própria de frequência ou dimensão; o gerador destas estruturas não consulta diretamente a opção vanilla de gerar estruturas. Outros geradores, mods e tipos de mundo podem mudar o resultado.
- Usa o mesmo piso, tabela de tesouro e validação de proteção dos projetos originais. Todas as construções são compactas e de superfície, com um baú central. Criptas e labirinto são formas decorativas, não dungeons subterrâneas com salas, armadilhas ou monstros próprios.
- Decorações novas ficam no volume de 7×7, entre 1 e 10 blocos acima do piso. Uma lista permitida exclui blocos perigosos, líquidos, blocos que caem e blocos com entidades. O centro do baú e a posição acima dele permanecem livres. O caminho de dois blocos de altura deve ligar uma borda acessível a um lado do baú antes da construção; a verificação não altera o terreno para abrir passagem. Flores, grama alta, arbustos secos e cogumelos vanilla são considerados atravessáveis, em vez de bloquearem o caminho como se fossem paredes; neve espessa não recebe essa permissão.
- A área de inspeção é de 9×9 e não atravessa a borda do chunk. Exige-se superfície natural, cinco camadas naturais sólidas sob a base, bioma uniforme e diferença de altura de até dois blocos sob o piso, além de distância segura dos limites de altura e da borda do mundo. As colunas são inspecionadas por toda a altura; estruturas e blocos desconhecidos bloqueiam a geração. Dados de limites salvos de estruturas vanilla também são examinados. Vegetação, telhados, bordas íngremes ou outros mods podem impedir a geração.
- Não força carregamento de chunks. Chunks carregados do disco são recusados, mesmo quando sua população não foi concluída; apenas chunks recém-gerados são elegíveis. A proteção é conservadora, mas blocos naturais colocados por jogadores podem ser indistinguíveis de terreno natural; não é garantia absoluta para todas as construções ou mods.
- A construção é planejada e validada antes de escrever. A criação do baú e atribuição do saque também integram a operação protegida. Se falhar sem deixar um baú com entidade, tenta-se reverter os blocos que ainda têm exatamente o estado escrito pelo gerador. Se restar entidade ou outro bloco no lugar do baú, preservam-se o piso e a estrutura para não destruir dados nem retirar o apoio. Assim, pode restar uma estrutura parcial ou um baú sem tesouro. A reversão é de melhor esforço: callbacks, descarregamento, exceções e alterações simultâneas podem impedir a restauração; estados idênticos colocados por outro mod são indistinguíveis. Exceções não são ocultadas e ainda podem interromper a geração.
- A consulta às caixas vanilla depende dos dados já registrados: estruturas ainda não registradas e estruturas de outros mods não são todas detectáveis. Não é proteção absoluta de mundos, especialmente com outros mods; faça backup antes de testar.
- Na simulação em terreno plano, as decorações variaram entre 16 e 176 blocos; piso e baú acrescentam 50 escritas em terreno plano, podendo haver apoios adicionais em declives. A inspeção completa custa até 81 × altura do mundo leituras por tentativa que alcance essa etapa, mais a consulta às caixas vanilla. Não há geração forçada de vizinhos.

## Torre do mago

- Pode ser selecionada em Enchanted Grove, Witching Mire e Umbral Reach. Tem escada, galeria e baú com a tabela de tesouro do bioma.
- Após a conclusão da torre, tenta criar um mago hostil persistente, com ataque mágico e barra de chefe. A criação pode falhar por colisão, chunks indisponíveis ou cancelamento por outro mod, sem nova tentativa. No modo Pacífico, como outros monstros hostis, o mago é removido; ele não reaparece automaticamente. Não foi testado no Minecraft real.

## Como procurar no jogo

1. Depois de instalar uma versão do mod que contenha estas alterações, crie um mundo novo do tipo **Padrão**, em Criativo, no Overworld. Em um mundo existente faça backup e vá para áreas ainda não geradas; nenhuma estrutura é adicionada aos chunks antigos.
2. Use a bússola de biomas do Nexus para procurar um dos oito biomas e voe pelo terreno aberto. Ela procura biomas, não estruturas, e a busca tem alcance limitado; o resultado para área ainda não gerada é uma previsão, não uma confirmação. O projeto não fornece comando para localizar diretamente estas construções; `/locate` do Minecraft 1.12.2 não as encontra.
3. Procure pequenas construções com baú na superfície. Se não achar, continue explorando outros chunks novos do bioma ou crie outro mundo/semente: a chance inicial não garante resultado e o terreno pode ser rejeitado. Anote semente, coordenadas e versão do mod ao relatar o teste. Nenhum teste dentro do Minecraft foi executado aqui.

## Verificação independente

- `sh tests/structures-harness/run.sh` (JDK 8+) compila o gerador com simuladores mínimos, sem iniciar Minecraft. Testa seleção forçada de todos os IDs 0–49 por bioma, preservação das oito versões originais, geometria, caminhos, segunda chamada, falhas do baú e a regressão de flores em volta do terreno. Exercita o sorteio, dimensões, bioma, chunk salvo ou ausente, borda, blocos e baú preexistentes, e caixas vanilla sobrepostas ou malformadas. São 58 conjuntos de coordenadas distintos; isso não prova que todas as construções sejam visualmente muito diferentes.
- O gerador e os três arquivos de desenhos também compilaram com Java 8 contra Forge 1.12.2, mapeamentos snapshot 20171003. Os testes simulados não comprovam comportamento em Minecraft real, interação com outros mods, chunks salvos, biomas reais, inventário do baú nem geração em servidores. É necessário testar em uma cópia de mundo no jogo antes de considerar a integração validada.
