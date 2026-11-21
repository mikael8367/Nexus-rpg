# Auronita e Umbrilita

Dois materiais ficcionais adicionais para Nexus RPG / Forge 1.12.2, independentes dos conjuntos mithril, adamantita e draconita já existentes. Cada um oferece minério, barra, espada, picareta, machado, pá, enxada e quatro peças de armadura. Tudo está disponível na aba criativa Nexus e pode ser fabricado no modo sobrevivência. Não adiciona geração natural: nenhum mundo recebe estes minérios automaticamente; geração futura depende de auditoria e de uma alteração separada.

## Obtenção

* Misture **2 barras de ferro + 1 barra de ouro + 1 redstone** em qualquer disposição para criar **2 barras de auronita**.
* Misture **2 barras de auronita + 1 pérola do End + 1 obsidiana** em qualquer disposição para criar **2 barras de umbrilita**.
* Misture **1 pedra normal + 1 barra do material** para criar um bloco de minério artificial desse material. Fundir um bloco no forno devolve **1 barra** e concede 0,5 XP (auronita) ou 0,8 XP (umbrilita). Isso é uma alternativa decorativa/creativa, não um recurso natural: a ida e volta não multiplica barras. O bloco exige picareta de nível 2 ou 3 respectivamente para ser coletado.
* Ferramentas e armaduras usam os formatos vanilla com barras do próprio material e gravetos quando aplicável, como as receitas existentes de `GEAR.md`. Barras do material correspondente reparam os equipamentos.

## Equilíbrio inicial

| Material | Nível de mineração | Usos ferramentas | Velocidade | Bônus de dano do material | Encantabilidade | Fator durabilidade armadura | Proteção pés/pernas/peito/cabeça | Tenacidade |
|---|---:|---:|---:|---:|---:|---:|---|---:|
| Auronita | 2 | 700 | 7 | 2,5 | 14 | 20 | 2/5/6/2 | 0 |
| Umbrilita | 3 | 1300 | 8 | 3 | 16 | 28 | 3/5/7/3 | 1 |

São valores iniciais, abaixo dos conjuntos avançados já presentes no projeto, sem efeitos especiais ou alterações de outras mecânicas. As texturas são adaptações de paleta e silhueta dos equipamentos existentes, não desenhos independentes; ficam em `.png.b64` e usam a decodificação do `processResources`. Ambas usam os modelos de armadura existentes, e suas camadas 64×32 próprias.

## Integração e teste

* Registro independente `com.nexusrpg.gear.OriginalGear` (blocos, ItemBlocks e itens), modelos exclusivos do lado cliente em `OriginalGearModels`, e chamada `OriginalGear.registerSmelting()` em `NexusRpg.init`. O coletor da aba criativa existente reconhece itens pelo namespace.
* `python3 tests/original-gear/check_resources.py` valida JSON, caminhos de modelo, imagens PNG (CRC e dimensões), traduções, associação estática aos registradores e ausência de chamada de geração no registro novo. Verifica também que a receita de minério devolve no máximo a barra investida.
* A compilação Java pontual deve incluir o Forge mapeado, log4j-api, Guava e as classes já compiladas do projeto, com JDK 8. A validação leve não substitui uma execução do jogo: antes da entrega é necessário testar no cliente e no servidor dedicado os registros, aba, bancada, forno, reparo, visual e equilíbrio, e fazer o build completo quando for seguro executar Gradle. A geração existente de outros minérios permanece intacta.
