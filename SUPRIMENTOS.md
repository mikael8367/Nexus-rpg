# Suprimentos de campo (experimentais)

Três consumíveis fabricáveis na grade 2×2, sem formato específico e disponíveis na aba Nexus RPG. Clique direito com o item na mão. Cada uso eficaz tem espera de 2 segundos para aquele item, por jogador. Em criativo não consomem; espectadores e jogadores falsos não usam.

* Bandagem de campo: papel + linha. Recupera até 2 corações; não é gasta com vida cheia ou se a cura for recusada.
* Antídoto de campo: frasco de vidro + açúcar + dente-de-leão. Remove veneno e Wither, sem apagar outros efeitos; não é gasto se não remover nenhum. O frasco é ingrediente, não é devolvido.
* Pó de fuga: pólvora + pena + açúcar. Dá Velocidade I por 10 segundos; não é gasto nem substitui qualquer efeito de velocidade já ativo.

Os itens não criam dados de progressão e não modificam as jornadas ou caçadas. NBT nas pilhas é preservado quando a quantidade diminui. Os efeitos são executados no servidor; o cliente apenas prevê o resultado da interação. A espera é temporária, não é um progresso salvo.

`tests/supplies/SupplyTest.java` cobre a lógica de consumo com alvo simulado e APIs Minecraft/Forge reais, e verifica progressão/NBT/clone em conjunto com dados das missões. A inicialização isolada emite aviso sobre receitas vanilla indisponíveis no classpath de teste; não comprova carregamento de receitas, efeitos reais, cliente/servidor, compatibilidade com outros mods, empacotamento ou comportamento em Minecraft. Esses pontos precisam de teste real antes de distribuição.
