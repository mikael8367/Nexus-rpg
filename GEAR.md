# Equipamentos Nexus RPG

Recursos para Minecraft Java 1.12.2 / Forge, com três conjuntos: mithril, adamantita e draconita. Cada conjunto tem minério, barra, espada, picareta, machado, pá, enxada, capacete, peitoral, calças e botas. A progressão temática pretendida segue essa ordem; os atributos, níveis de mineração, locais e frequências de geração dependem da implementação Java.

## Obtenção

Os itens, blocos e sua geração precisam estar registrados pelo mod. Depois de obter as barras de um material, use-as com gravetos nas receitas abaixo. O Minecraft 1.12.2 não registra fundição pelo formato JSON de receitas de bancada: a conversão de minério em barra depende de uma receita de fundição registrada em Java (ou de outro método de obtenção implementado pelo mod). Não há garantia de geração natural nem de fundição apenas com estes arquivos de recursos.

## Receitas de bancada

`I` = barra do material do conjunto; `S` = graveto; `.` = vazio. Cada receita produz um item. Todas usam barras do mesmo conjunto.

```text
Espada       Picareta     Machado      Pá           Enxada
.I.          III          II           I            II
.I.          .S.          IS           S            .S
.S.          .S.          .S           S            .S

Capacete     Peitoral     Calças       Botas
III          I.I          III          I.I
I.I          III          I.I          I.I
             III          I.I
```

Os formatos são espelháveis conforme as regras normais de receitas moldadas do Minecraft. Use uma bancada 3×3 quando necessário.

## Texturas e integração

As texturas são PNGs codificados em Base64 com sufixo `.png.b64`, para manter apenas texto no repositório. `processResources` decodifica esses arquivos para os caminhos `.png` no diretório de saída e os inclui no JAR, sem incluir os `.b64`. `mcmod.info` continua recebendo as variáveis de versão. Se houver um `.png` e um `.png.b64` para o mesmo caminho de origem, o processamento falha em vez de escolher um silenciosamente. As texturas de armadura usam o layout vanilla 64×32: camada 1 para capacete, peitoral e botas; camada 2 para calças. O código de armadura precisa apontar para `nexusrpg:textures/models/armor/<material>_layer_1.png` e `_layer_2.png` conforme apropriado, e os modelos precisam usar UVs compatíveis.

Os modelos, traduções e receitas dependem dos IDs `nexusrpg:<material>_<tipo>` estarem registrados no Java. Minérios também precisam de ItemBlock e registro de modelo no cliente; itens e ferramentas precisam de modelos registrados. Traduções usam as chaves `tile.nexusrpg.<id>.name` e `item.nexusrpg.<id>.name`.
