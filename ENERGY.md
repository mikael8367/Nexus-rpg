# Rede de energia

A rede usa Forge Energy (FE). O código compila, mas ainda precisa de testes no Minecraft e em servidor dedicado.

- `nexusrpg:solar_panel_large`: painel 3×3 construído ao colocar um único item. Os nove espaços precisam estar vazios e no mesmo chunk. O bloco central fornece até 40 FE/t durante o dia, sem chuva e com céu livre. Conecte o cabo ao bloco central.
- `nexusrpg:coal_generator`: aceita carvão ou carvão vegetal com clique direito ou automação e gera até 40 FE/t. Quebrar a máquina devolve o combustível guardado.
- `nexusrpg:energy_cable`: transmite FE para máquinas vizinhas e outros cabos, com braços visuais nas conexões. A rede transfere energia enviada ativamente e não carrega chunks; não atende máquinas que apenas puxam energia.
- `nexusrpg:digital_miner`: no Overworld, examina uma área 16×128×16, centrada horizontalmente na máquina, de Y=0 a Y=127. Cada minério extraído custa 200 FE. Clique com o botão direito para retirar a primeira pilha, ou use funis/tubos para extrair os itens. A mineradora pausa se o inventário estiver cheio e não carrega chunks. Reconhece minérios vanilla, do Nexus RPG e registrados no Ore Dictionary. Usa eventos Forge de proteção; compatibilidade com todo mod de claims não é garantida.

## Receitas

Receitas de bancada 3×3; linhas são mostradas de cima para baixo:

| Resultado | Padrão | Ingredientes |
| --- | --- | --- |
| Painel solar grande | `GGG / RRR / IMI` | G vidro, R redstone, I barra de ferro, M barra de mithril |
| Gerador a carvão | `ICI / IFI / IRI` | I barra de ferro, C carvão, F fornalha, R redstone |
| 8 cabos de energia | `III / RRR / III` | I barra de ferro, R redstone |
| Mineradora digital | `IMI / RPR / IEI` | I barra de ferro, M barra de mithril, R redstone, P picareta de diamante, E pérola do End |

A barra de mithril é obtida pela fundição do minério registrado pelo mod; nenhuma receita depende de outra máquina desta rede. A disponibilidade no mundo e a integração dos registros devem ser verificadas na versão final.

## Recursos visuais

As texturas pixel-art são mantidas como `.png.b64` em `assets/nexusrpg/textures/blocks`; a etapa `processResources` as decodifica para PNG. Modelos de blocos, itens, blockstates e receitas ficam em `assets/nexusrpg` e requerem os IDs correspondentes no registro Java. Máquinas usam modelos de cubo com faces distintas; o cabo usa modelos separados para centro e seis braços, além de um modelo de inventário.
