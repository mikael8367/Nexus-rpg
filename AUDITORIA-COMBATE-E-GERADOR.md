# Auditoria de combate e do gerador (21/11/2026)

## Gerador de carvão: correção bloqueada

`tests/energy-coal-audit/run-break.sh` ainda reproduz perda do backup NBT na quebra (`DATA LOSS: breakBlock removed opaque archive without a recovery carrier`). Não quebre geradores com dados importantes. Nenhuma mudança experimental do gerador foi integrada nesta etapa.

Uma pesquisa separada no experimento não publicado reproduziu mais cenários de perda: drops desativados ou spawn recusado, substituição forçada após captura de snapshot e um campo desconhecido aninhado em uma capability reconhecida que não sobrevive ao ciclo de leitura/gravação. Esses cenários foram simulados e não comprovam cada caminho em um mundo real. Um item portador não é solução completa: ainda faltam recuperação, prova de ausência de duplicação e uma estratégia verificável para rollback, interrupções e escrita em disco. Não usar o experimento em mundos.

## Morte, XP e missão de zumbis

A implementação publicada escuta `LivingDeathEvent` com prioridade LOWEST, mas um listener posterior na mesma prioridade ainda pode cancelar a morte. Nesse caso XP e missão de zumbis podem receber crédito indevido. Foi estudada uma alternativa baseada em `LivingDropsEvent` depois de observar a morte, mas ela pode perder crédito para entidades que não emitam drops e em outras situações ambíguas. Ela não foi integrada. As caçadas de esqueleto/aranha têm seu próprio mecanismo e também exigem teste com mods e reinício.

## Necromante

A cura passiva observa `LivingDamageEvent` antes de seu processamento terminar; outro listener pode cancelar o dano mais tarde. Um protótipo que verifica cancelamento e queda de vida no fim do tick foi testado isoladamente, mas não prova que a queda veio daquele ataque quando outros efeitos ocorrem. Ele não foi integrado. As habilidades ativas também precisam de validação com cancelamento e mods.

## Validação pendente

Testar com Minecraft/Forge real, cliente e servidor, mods que cancelam eventos antes e depois dos listeners, mobs com pipelines de morte diferentes, desconexão, clone, reinício, mundos copiados e cenários de crash. Harnesses isolados não substituem esses testes. A torre gigante permanece desativada.
