# Habilidades Nexus RPG (Forge 1.12.2)

## Uso pelo jogador

Escolha uma das classes no sistema de personagem. Abra o chat normal do Minecraft (não é um comando com barra) e envie:

- `!skill` ou `!skill info`: classe, nível, aperfeiçoamento e pontos de habilidade livres.
- `!skill tree`: mostra os nós, graus, custos e efeitos da classe atual.
- `!skill up`: gasta um ponto no aperfeiçoamento original da classe atual, até 5.
- `!skill up <id>`: aprimora um nó, por exemplo `!skill up warrior.active`.
- `!skill cast`: usa a habilidade ativa da classe. Nas classes ofensivas, mire num monstro hostil visível.
- `!skill ultimate`: usa a suprema da classe, disponível a partir do nível 15.

Não é necessário OP. O chat é apenas a entrada: a classe, o alvo, a vida, a mana, o nível e os tempos são verificados no servidor. Servidores que bloqueiam ou alteram mensagens de chat podem impedir esse controle. Não existe tecla nem interface dedicada nesta versão.

O personagem recebe 1 ponto de habilidade a cada 3 níveis, começando no nível 4. Esses pontos são separados dos pontos de atributos do personagem. O gasto é somado entre todas as classes, mesmo se um sistema futuro permitir trocar de classe. Aperfeiçoamento de 0 a 5 aumenta o dano e/ou a cura; sem pontos, a ativa continua disponível. A suprema custa 30 de mana e tem recarga de 60 segundos; a ativa custa 10 e tem recarga de 8 segundos. Os tempos usam ticks do Overworld (20 ticks por segundo de jogo), não tempo real: ficam pausados enquanto o servidor está parado e podem demorar mais em servidor com TPS baixo. As recargas são separadas por classe e habilidade e persistem ao morrer e reconectar. A passiva do necromante tem recarga própria de 200 ticks, também preservada. Alterações administrativas no relógio total do mundo ou restauração de backup podem afetar as recargas.

## Classes

Dano base da ativa ofensiva: 4 + aperfeiçoamento + até 5 por inteligência (0,05 por ponto, até 100), antes de outras modificações de dano. Cada suprema ofensiva acrescenta 4. Passivas e outros sistemas podem modificar os valores finais; imunidade, armadura e eventos Forge também se aplicam. As ativações sem alvo válido, sem nível, sem mana ou em recarga não consomem mana. Uma tentativa válida pode consumir mana e recarga mesmo que outro mod ou imunidade bloqueie o dano. Os efeitos pessoais e a marca de combo também podem ocorrer sem dano efetivo; a cura da suprema do necromante conta alvos elegíveis, não dano confirmado.

| Classe | Ativa | Suprema | Passiva contra hostis |
| --- | --- | --- | --- |
| Guerreiro | Golpe até 5 blocos e força por 3 s | Golpe em área de 7 blocos e resistência por 6 s | +10% dano a até 5 blocos |
| Mago | Golpe até 16 blocos e resistência por 3 s | Golpe em área de 7 blocos e regeneração por 5 s | até +15% dano conforme inteligência |
| Arqueiro | Golpe até 16 blocos e velocidade por 4 s | Golpe em área de 10 blocos e velocidade II por 8 s | +15% dano de projéteis |
| Assassino | Golpe até 6 blocos e invisibilidade por 3 s | Golpe no alvo até 6 blocos e invisibilidade por 5 s | +15% dano a hostis com menos de metade da vida |
| Paladino | Cura pessoal e resistência por 5 s | Cura pessoal maior e resistência II por 10 s | +15% dano em mortos-vivos |
| Necromante | Golpe até 16 blocos e cura pessoal | Golpe em área de 7 blocos e cura por alvo, até 6 pontos de vida | cura pessoal em um ataque a cada 10 s |

O aperfeiçoamento adiciona 1 ponto percentual às passivas de dano quando a condição delas é satisfeita. A passiva do necromante cura 1 + 0,2 por aperfeiçoamento. Combinação: use a ativa e, até 6 s depois, a suprema; no mesmo alvo marcado pela ativa, a suprema causa mais 3 de dano. Para o paladino, a mesma sequência aumenta em 3 a cura pessoal da suprema. Combos expiram em ticks de jogo, não se transferem a outro alvo e são apagados ao morrer, sem apagar recargas ou aperfeiçoamentos.

## Árvore de habilidades

Há quatro nós por classe, com IDs permanentes `classe.training`, `classe.passive`, `classe.active` e `classe.ultimate` (24 nós no total). O aperfeiçoamento original é a raiz: até 5 graus, 1 ponto por grau. Os ramos passivo e ativo exigem a raiz no grau 1 e nível 4; cada um tem até 3 graus, custando 1 ponto por grau. O nó de melhoria da suprema exige nível 15, passiva grau 1 e ativa grau 2; tem até 2 graus e custa 2 pontos por grau. Estes nós melhoram habilidades já disponíveis: a ativa original continua acessível sem investimento e a suprema original continua acessível no nível 15, independentemente da árvore. Há pontos compartilhados para todas as classes. Com apenas os gastos mínimos em pré-requisitos, a primeira melhoria da suprema exige pelo menos nível 19, para haver pontos suficientes.

| Classe | Passiva por grau | Ativa por grau | Suprema por grau |
| --- | --- | --- | --- |
| Guerreiro | +3 pontos percentuais na passiva corpo a corpo | +1 dano | +2 dano e +2 s resistência |
| Mago | +3 pontos percentuais quando a passiva de inteligência se aplica | +1 dano | +2 dano e +2 s regeneração |
| Arqueiro | +3 pontos percentuais na passiva de projétil | +1 dano | +2 dano e +2 s velocidade |
| Assassino | +3 pontos percentuais na passiva contra inimigos com pouca vida | +1 dano | +2 dano e +2 s invisibilidade |
| Paladino | +3 pontos percentuais na passiva contra mortos-vivos | +1 cura pessoal | +2 cura pessoal e +2 s resistência |
| Necromante | +0,5 cura pessoal na passiva | +1 dano e +0,5 cura pessoal | +2 dano e +1 cura pessoal |

Os bônus de dano continuam restritos aos hostis elegíveis, e os bônus pessoais não alteram blocos nem atacam aliados. A cura da suprema do necromante é a cura anterior (até 6) mais o bônus investido.

**Compatibilidade:** a raiz lê diretamente os antigos campos `rank_<classe>`, sem copiar, converter ou apagar. Logo, migração é virtual, imediata e idempotente; pontos gastos anteriormente continuam descontados globalmente, e recargas e demais dados permanecem. Os nós novos ficam em `NexusRpgSkills.tree_v1`, sem apagar outros campos. Morte copia os dados e apaga somente os combos temporários, como antes. Graus ou árvore em tipo/valor inválido, ou nós comprados sem seus pré-requisitos, bloqueiam compras e ativações e desativam passivas, sem sobrescrever o conteúdo. Recargas e combos em tipo/valor inválido também bloqueiam ativações e passivas. Em caso de dados inválidos, o clone preserva inclusive combos para recuperação; peça a um administrador para restaurar um backup íntegro. Faça backup antes de instalar ou atualizar. Voltar para uma versão antiga não é suportado: ela ignora gastos nos novos nós e pode permitir gastos duplicados.

## Limites e segurança

Alvos ofensivos são apenas entidades vivas que implementam `IMob`, com linha de visão; jogadores, criaturas mansas, entidades com proprietário e membros da mesma equipe são excluídos. Supremas em área limitam-se a 12 alvos, sem seleção por distância entre os elegíveis. Criaturas de outros mods sem essas interfaces não são reconhecidas automaticamente e sistemas próprios de amizade podem não ser reconhecidos. Não há explosões, alteração de blocos, invocação, teleporte nem dano PvP intencional. O dano usa `DamageSource.causePlayerDamage` e passa pelos eventos de dano do Forge; efeitos adicionais são aplicados somente ao próprio jogador, para não contornar proteção por cancelamento de dano. A cura passiva do necromante só é tentada no evento `LivingDamageEvent` com dano positivo, depois dos cancelamentos anteriores; um outro mod que cancele depois dela ainda pode impedir o dano. A passiva não é aplicada por jogadores mortos ou espectadores. Compatibilidade com todas as proteções e mods não é garantida: teste no servidor antes de uso público.

## Integração técnica

`com.nexusrpg.skills.SkillEvents` é registrado por `@Mod.EventBusSubscriber(modid="nexusrpg")`. Usa `ServerChatEvent`, `LivingHurtEvent`, `LivingDamageEvent` e `PlayerEvent.Clone`, sem alterar a classe principal, redes ou código de personagem. Consome as APIs estáticas `CharacterProfile.characterClass`, `level`, `intelligence` e `consumeMana`. Os dados ficam no subcomposto `NexusRpgSkills` dentro de `EntityPlayer.PERSISTED_NBT_TAG`, preservando os dados de outros sistemas no clone. Todo o controle ocorre no servidor lógico. Os nomes de classe esperados são `warrior`, `mage`, `archer`, `assassin`, `paladin`, `necromancer`. Este sistema não implementa nem consulta subclasses; usa somente a classe retornada pelo perfil.

### API para interface e rede futura

- `SkillTree.nodes(String classe)` e `SkillTree.allNodes()` fornecem listas imutáveis de `Node`. `SkillTree.node(String id)` consulta um nó. Campos públicos imutáveis: `id`, `characterClass`, `name`, `description`, `kind` (`training`, `passive`, `active`, `ultimate`), `x`, `y`, `maxRank`, `cost`, `minLevel` e `requirements`. Cada requisito tem `nodeId` e `rank`; desenhe ligações usando essas referências. As coordenadas são relativas, não pixels.
- `SkillTreeService.snapshot(EntityPlayerMP)` deve rodar na thread do servidor e devolve NBT independente: `ok` booleano; se verdadeiro, `class` string, `level` int, `points` int e `ranks` composto de `id` para int; se falso, `reason` string. O servidor deve enviar o snapshot ao cliente após abrir, comprar e ao receber mudanças relevantes de personagem. A interface pode calcular disponibilidade para exibição, mas o servidor sempre revalida.
- `SkillTreeService.allocate(EntityPlayerMP, String nodeId)` deve rodar na thread do servidor, retornando string vazia em sucesso ou mensagem de erro em português. Identifica jogador pelo contexto da conexão, não por ID recebido do cliente; envia só o ID do nó e nunca aceita custo, grau, pontos, nível ou classe como autoridade. O servidor valida classe, disponibilidade, máximo e todos os pré-requisitos antes de gastar.
- `SkillTreeService.activate(EntityPlayerMP, boolean ultimate)` solicita ativa (`false`) ou suprema (`true`) e usa as validações existentes; informa resultado pelo chat. Chamar na thread principal. Solicitações de rede devem ser agendadas na thread do servidor e responder com snapshot novo. A API não registra pacotes nem abre tela; isso pertence à futura integração da interface.

Os testes `tests/skills/SkillRulesTest.java` e `SkillTreeTest.java` verificam limites, dados legados, compras de todas as classes, pré-requisitos, custos, bloqueio de NBT inválido e preservação de dados. As fontes Java 8 foram compiladas contra o Forge 1.12.2 mapeado. Isso não equivale a teste dentro do jogo, teste de multiplayer, teste de proteção de território, build Gradle completo ou teste de balanceamento.
