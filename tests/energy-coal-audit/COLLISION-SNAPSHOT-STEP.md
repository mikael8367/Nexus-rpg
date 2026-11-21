# Etapa de pesquisa — NÃO USAR EM MUNDOS

Este relatório atualiza apenas os pontos abaixo do CRITICAL-RESULTS.md; aquele arquivo histórico foi preservado. Não houve publicação, merge, commit, edição de mundo ou teste do Minecraft real.

## Colisões na serialização

Em `TileCoalGenerator`, se uma raiz `ForgeData`/`ForgeCaps` malformada colidir com dados ativos, os dados ativos ficam na chave Forge; a raiz antiga é guardada, com nome e tipo, em envelope opaco `NexusRpgForgeCollision[_n]`. Entradas conflitantes pré-existentes no objeto de saída são guardadas como `NexusRpgCallerCollision[_n]`. Esses envelopes não são usados para restaurar capability, dados ou inventário. Raízes desconhecidas existentes são reservadas antes de escolher nomes para os novos envelopes. A mudança elimina as `IllegalStateException` explícitas desses caminhos de colisão; NÃO garante que serializadores/desserializadores de outros mods, erros de registro do tile, memória ou outras falhas não lancem exceções. A gravação normal de dados ativos por si só não implica que uma cópia opaca de outra origem seja segura para recuperação; não há recuperação automática.

Teste `CoalForgeCollisionRoundTripTest`: dois conflitos simultâneos, prefixo ocupado, dados ativos distintos, cinco recargas/gravações, verificações de unicidade dos envelopes, entrada não mutada, colisões em campos de saída e campos iguais. Os testes antigos de exceção foram atualizados para exigir preservação sem a exceção específica.

## Snapshots e quebra: situação ainda insegura

Inspeção do bytecode Forge 1.12.2-14.23.5.2860: `World.setBlockState` cria `BlockSnapshot` antes de `Chunk.setBlockState` durante captura; `Chunk.setBlockState` chama `breakBlock` e remove o tile. `BlockSnapshot` usa `TileEntity.writeToNBT`, e `restore(true)` recoloca o bloco e chama `readFromNBT` no tile presente. Não confundir esse snapshot de estado pré-alteração com um sinal confiável de commit. O código atual suprime drops durante captura/restauração para não duplicar no rollback.

Teste `CoalSnapshotProtocolTest` utiliza o verdadeiro `BlockSnapshot` Forge com mundo/célula simulado, não `World`/`Chunk` reais: rollback recria tile e restaura NBT sem drops; commit simulado, descartando snapshot após substituição forçada, comprova perda dos dados (tile apagado e nenhum drop). Passar nesse teste significa detectar o bug, não consertá-lo. Outras sequências de restauração e callbacks não foram verificadas.

- Player comum/criativo: a proteção atual impede remoção em captura e quando o tile contém dados opacos, mas não cobre remoção genérica. Estado normal de energia e combustão não é preservado no drop comum. Criativo não tem garantia de conservação especial.
- Explosão: proteção para dados opacos/snapshot bloqueia a destruição pelo método próprio e suprime drops; para estado normal, chance de drop, energia e combustão seguem sem garantia.
- `doTileDrops=false`: extração normal é feita antes de tentar `spawnAsEntity`; regra pode impedir spawn e perder o combustível. A remoção forçada com dados opacos também pode perder o carrier. Sem sucesso confirmado de spawn não há garantia.
- `setBlockState`, `destroyBlock`, outros mods, rollback e chamadas diretas a `breakBlock` podem contornar `removedByPlayer`/explosão; não há gancho aqui que possa vetar genericamente a alteração depois que ela já ocorreu. Carriers podem ser cancelados ou destruídos. Nunca gerar itens a partir dos snapshots: rollback restauraria o estado e duplicaria.

## Próxima implementação proposta (não implementada)

1. Não liberar a funcionalidade; em caminhos de remoção controláveis, recusar de forma consistente toda remoção contendo estado até existir transação confiável. Isso não protege `setBlockState` arbitrário.
2. Projetar um protocolo com participante transacional explícito no chamador que possui captura: registrar antes da alteração um identificador único e payload completo em journal persistente, sem extrair nem gerar item; rollback descarta/cancela o registro e restaura exclusivamente o snapshot; commit confirmado e apenas após a alteração ser realmente definitiva resolve o payload uma única vez, respeitando regra de drops e política criativo. Sem callback confiável de commit/rollback, assumir resultado desconhecido e não gerar item automaticamente. Sem restauração automática de cópias opacas junto a inventário ativo.
3. Crash/ordem de escrita: journal, chunk, inventário de jogador e entidade/item não são atomicamente persistidos pelo Forge; identificação/deduplicação e reconciliação auditável são necessárias, mas não garantem exactly-once em todos os caminhos sem intervenção adicional. Uma solução universal exigiria interceptação suportada e testada de todos os caminhos, ou restringir o contrato e oferecer recuperação manual controlada. Mod adversarial ou que ignore o protocolo permanece fora da garantia.
4. Em isolamento, testar Forge real e servidor cliente: capturas aninhadas, falha/cancelamento, commit e rollback, jogador/criativo, explosão, `doTileDrops` ligado/desligado, `setBlockState`/`destroyBlock`, desabilitar chunks, restart/crash em cada etapa, mod de capability que recusa tipos, duplicatas e preservação de dados antigos e mundos reais COPIADOS. Fazer backup previamente. A solução continua não utilizável até testes e revisão.

## Execução

`JAVA_HOME=/home/daytona/workspace/nexus-test/jdk GRADLE_USER_HOME=/home/daytona/workspace/nexus-test/gradle sh tests/energy-coal-audit/run-safety.sh` passou 10 classes headless, compilando o código modificado contra Forge 1.12.2 e usando classes base pré-compiladas de outra árvore, somente leitura. Log: `step-snapshot-test.log`. Avisos Unsafe e erros headless de `.mcassetsroot`/receitas apareceram. Não é build completo nem jogo/servidor real. Espaço livre ~106 MiB; nenhuma limpeza foi realizada.
