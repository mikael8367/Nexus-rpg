# Auxílios de expedição

Três itens de uso único, criáveis sem novas estruturas ou geração de mundo. Use com o botão direito (em qualquer mão); pilhas de até 16. O efeito é aplicado apenas no servidor a jogadores reais vivos que não estejam em modo espectador. Em criativo não consomem; após uso bem-sucedido há espera de 40 ticks para aquele item. Um uso sem resultado não consome nem inicia espera.

* **Unguento antichamas:** papel + bola de neve + bola de slime. Apaga o fogo atual; não impede que uma fonte de fogo ou lava volte a queimar o jogador.
* **Reserva do mergulhador:** papel + açúcar + fragmento de prismarine. Debaixo d'água, concede respiração aquática por 600 ticks (30 segundos em 20 TPS). Não substitui um efeito de respiração aquática já ativo.
* **Pó de lanterna:** papel + cenoura + pó de pedra luminosa. Concede visão noturna por 600 ticks. Não substitui visão noturna já ativa.

As receitas são sem formato e cada combinação produz um item. As texturas seguem o padrão PNG codificado do projeto. Testes em `tests/expedition` compilam todo o código com Forge mapeado, verificam a lógica de consumo e a correspondência de receitas com objetos reais de crafting; a checagem de colisões cobre receitas do mod com ingredientes explícitos por ID, não outros mods nem receitas com alternativas. Não substituem testes dentro do jogo.
