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

- Somente Overworld, nos oito biomas correspondentes, com tentativa inicial de 1 em 80 por chamada. A seleção uniforme entre o projeto original e os projetos daquele bioma ocorre depois das verificações de terreno (1 em 8 para os estilos 0–1; 1 em 7 para os demais). A probabilidade final é menor por causa das rejeições. Uma falha de validação não tenta outro projeto.
- Usa o mesmo piso, tabela de tesouro e validação de proteção dos projetos originais. Todas as construções são compactas e de superfície, com um baú central. Criptas e labirinto são formas decorativas, não dungeons subterrâneas com salas, armadilhas ou monstros próprios.
- Decorações novas ficam no volume de 7×7, entre 1 e 10 blocos acima do piso. Uma lista permitida exclui blocos perigosos, líquidos, blocos que caem e blocos com entidades. O centro do baú e a posição acima dele permanecem livres. O caminho de dois blocos de altura deve ligar uma borda acessível a um lado do baú antes da construção; a verificação não altera o terreno para abrir passagem.
- A área de inspeção é de 9×9 e não atravessa a borda do chunk. Exige-se superfície natural, bioma uniforme e diferença de altura de até dois blocos sob o piso, além de distância segura dos limites de altura e da borda do mundo. As colunas são inspecionadas por toda a altura; estruturas e blocos desconhecidos bloqueiam a geração. Dados de limites salvos de estruturas vanilla também são examinados. Vegetação, telhados, bordas íngremes ou outros mods podem impedir a geração.
- Não força carregamento de chunks. Chunks carregados do disco são recusados, mesmo quando sua população não foi concluída; apenas chunks recém-gerados são elegíveis. A proteção é conservadora, mas blocos naturais colocados por jogadores podem ser indistinguíveis de terreno natural; não é garantia absoluta para todas as construções ou mods.
- A construção é planejada e validada antes de escrever. A criação do baú e atribuição do saque também integram a operação protegida. Se falhar sem deixar um baú com entidade, tenta-se reverter os blocos que ainda têm exatamente o estado escrito pelo gerador. Se restar entidade ou outro bloco no lugar do baú, preservam-se o piso e a estrutura para não destruir dados nem retirar o apoio. Assim, pode restar uma estrutura parcial ou um baú sem tesouro. A reversão é de melhor esforço: callbacks, descarregamento, exceções e alterações simultâneas podem impedir a restauração; estados idênticos colocados por outro mod são indistinguíveis. Exceções não são ocultadas e ainda podem interromper a geração.
- A consulta às caixas vanilla depende dos dados já registrados: estruturas ainda não registradas e estruturas de outros mods não são todas detectáveis. Não é proteção absoluta de mundos, especialmente com outros mods; faça backup antes de testar.
- Na simulação em terreno plano, as decorações variaram entre 16 e 176 blocos; piso e baú acrescentam 50 escritas em terreno plano, podendo haver apoios adicionais em declives. A inspeção completa custa até 81 × altura do mundo leituras por tentativa que alcance essa etapa, mais a consulta às caixas vanilla. Não há geração forçada de vizinhos.

## Verificação independente

- `sh tests/structures-harness/run.sh` (JDK 8+) compila o gerador com simuladores mínimos, sem iniciar Minecraft. Testa seleção forçada de todos os IDs 0–49 por bioma, preservação das oito versões originais, geometria por conjunto de coordenadas, limites, caminho até o baú em terreno plano, segunda chamada no mesmo chunk e falhas simuladas na criação do baú. São 58 conjuntos de coordenadas distintos; isso não prova que todas as construções sejam visualmente muito diferentes.
- O gerador e os três arquivos de desenhos também compilaram com Java 8 contra Forge 1.12.2, mapeamentos snapshot 20171003. Os testes simulados não comprovam comportamento em Minecraft real, interação com outros mods, chunks salvos, biomas reais, inventário do baú nem geração em servidores. É necessário testar em uma cópia de mundo no jogo antes de considerar a integração validada.
