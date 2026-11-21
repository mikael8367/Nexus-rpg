# Habilidades Nexus RPG (Forge 1.12.2)

## Uso pelo jogador

Escolha uma das classes no sistema de personagem. Abra o chat normal do Minecraft (não é um comando com barra) e envie:

- `!skill` ou `!skill info`: classe, nível, aperfeiçoamento e pontos de habilidade livres.
- `!skill up`: gasta um ponto no aperfeiçoamento da classe atual, até 5.
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

## Limites e segurança

Alvos ofensivos são apenas entidades vivas que implementam `IMob`, com linha de visão; jogadores, criaturas mansas, entidades com proprietário e membros da mesma equipe são excluídos. Supremas em área limitam-se a 12 alvos, sem seleção por distância entre os elegíveis. Criaturas de outros mods sem essas interfaces não são reconhecidas automaticamente e sistemas próprios de amizade podem não ser reconhecidos. Não há explosões, alteração de blocos, invocação, teleporte nem dano PvP intencional. O dano usa `DamageSource.causePlayerDamage` e passa pelos eventos de dano do Forge; efeitos adicionais são aplicados somente ao próprio jogador, para não contornar proteção por cancelamento de dano. A cura passiva do necromante só é tentada no evento `LivingDamageEvent` com dano positivo, depois dos cancelamentos anteriores; um outro mod que cancele depois dela ainda pode impedir o dano. A passiva não é aplicada por jogadores mortos ou espectadores. Compatibilidade com todas as proteções e mods não é garantida: teste no servidor antes de uso público.

## Integração técnica

`com.nexusrpg.skills.SkillEvents` é registrado por `@Mod.EventBusSubscriber(modid="nexusrpg")`. Usa `ServerChatEvent`, `LivingHurtEvent`, `LivingDamageEvent` e `PlayerEvent.Clone`, sem alterar a classe principal, redes ou código de personagem. Consome as APIs estáticas `CharacterProfile.characterClass`, `level`, `intelligence` e `consumeMana`. Os dados ficam no subcomposto `NexusRpgSkills` dentro de `EntityPlayer.PERSISTED_NBT_TAG`, preservando os dados de outros sistemas no clone. Todo o controle ocorre no servidor lógico. Os nomes de classe esperados são `warrior`, `mage`, `archer`, `assassin`, `paladin`, `necromancer`. Este sistema não implementa nem consulta subclasses; usa somente a classe retornada pelo perfil.

O teste `tests/skills/SkillRulesTest.java` verifica limites de pontos, aperfeiçoamento, identificação de classes e limpeza de combos preservando recargas e aperfeiçoamento. A compilação Java 8 foi testada contra o jar mapeado Forge 1.12.2 e as fontes presentes. Isso não equivale a teste dentro do jogo, teste de multiplayer, teste de proteção de território, build Gradle completo ou teste de balanceamento.
