# Magia (Forge 1.12.2)

Quatro cajados aparecem na aba Nexus RPG. Segure o cajado e clique com o botão direito. Cada uso bem-sucedido consome **1 pó de redstone do inventário principal** (não procura na mão secundária); no criativo não consome. Os cajados não se gastam. Cada tipo tem seu próprio intervalo por jogador, compartilhado entre exemplares iguais.

- **Arcano:** raio visual instantâneo de até 16 blocos, 6 pontos (3 corações) de dano base, intervalo 1 segundo. Consome redstone mesmo se errar ou um obstáculo bloquear. Para no primeiro bloco sólido ou entidade com colisão. Só causa dano a entidades vivas `IMob` que não implementem `IEntityOwnable` e não estejam na equipe do usuário. Não causa dano a jogadores nem a animais normais; não é PvP. Criaturas neutras marcadas como `IMob` também podem ser atingidas. Aliados de outros mods sem equipe ou interface de dono não são identificáveis com certeza. O raio não é um projétil físico e não atravessa entidades nem blocos.
- **Cura:** recupera até 6 pontos (3 corações) do usuário, intervalo 10 segundos. Não consome se estiver com vida cheia.
- **Proteção:** Resistência I no usuário por 10 segundos, intervalo 15 segundos.
- **Agilidade:** Velocidade I no usuário por 15 segundos, intervalo 10 segundos.

Cura e efeitos beneficiam apenas o próprio usuário, sem cura em área. Proteção e agilidade não substituem efeito mais forte e não consomem pó se o mesmo efeito já durar pelo menos tanto quanto o novo. Se outro mod cancelar a cura ou impedir o efeito, o pó reservado é devolvido e não começa a recarga; se o inventário tiver sido preenchido durante o evento, o pó é largado no chão. Todas as decisões, dano, efeitos, custo e intervalo são aplicados pelo servidor; o cliente só envia a interação normal. Recipientes, água e danos ambientais não são alterados. Interações de mods de terceiros e experiências em servidor dedicado precisam ser testadas no jogo.

## Receitas

Na bancada, use a mesma forma para cada cajado, com espaços vazios onde indicado:

```
 _ G _
 _ R S
 S _ _
```

`S`: graveto, `R`: pó de redstone. `G`: diamante para Arcano, maçã dourada para Cura, barra de ferro para Proteção, pena para Agilidade. O pó da receita é independente do consumido ao lançar.
