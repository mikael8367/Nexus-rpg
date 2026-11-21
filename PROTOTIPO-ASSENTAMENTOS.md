# Protótipo offline de cabanas e pequeno povoado

`SettlementPrototype` é um planejador puro Java 8: **não registra comandos, não lê mundos nem coloca blocos**. Não está acessível ao jogador no jogo, não há spawn, geração natural nova, edição de chunks, vilarejos vanilla alterados, aldeões ou saques. Não é um sistema seguro de colocação. O mapa é imutável, usa coordenadas relativas inteiras e nomes simbólicos de blocos vanilla sem metadados; não contém NBT nem cria tile entities. Nada é carregado do Minecraft.

Cada um dos oito IDs `nexusrpg:*` de biomas existentes é reconhecido exatamente e recebe uma paleta. IDs desconhecidos ou nulos são rejeitados. `cabin(style)` fornece um desenho 7×7 com piso, parede, vãos de janela, entrada aberta de dois blocos de altura e cobertura escalonada. `village(style)` combina três cabanas e um caminho plano; limites relativos x=-12..12, y=0..5 e z=-4..3. É uma maquete, não um assentamento funcional. Posições não incluídas no mapa não representam ar a ser escrito: a aplicação futura deve exigir previamente que entradas, janelas e espaços de circulação estejam desocupados, não escavá-los.

## Estado anterior preservado

Este commit não altera geração existente. Na base inspecionada, `ModBiomes` **já** adiciona biomas às listas do `BiomeManager`, e `NexusRpg` **já** registra `NexusStructureGenerator`, além de outros geradores. Assim, dizer que toda geração natural está desativada por padrão seria falso. A torre grande `LargeWizardTower` permanece um plano separado, sem chamada no gerador inspecionado; não foi reativada. Não se afirma que qualquer geração existente seja segura ou funcione no jogo. O protótipo de povoado deste commit continua sem conexão com qualquer gerador.

## Condições para eventual integração (não implementadas)

Não ligar ao mundo sem projeto e testes reais específicos. Qualquer adaptação precisará operar apenas com autorização explícita, servidor, em área apropriada e pré-visualização, ser desativada por padrão e evitar chunks existentes, sem carregar ou gerar outros chunks. Deve validar limites de mundo/altura e todos os chunks envolvidos sem carregá-los; o povoado cobre mais de um chunk e não cabe na validação antiga 7×7. Deve rejeitar blocos existentes em todos os espaços escritos **e** espaços de circulação, dados NBT/tile entities, entidades, regiões protegidas, estruturas e dados ambíguos; checar novamente imediatamente antes de cada escrita e lidar com callbacks, falhas e rollback sem sobrescrever modificações externas. Não há garantia de atomicidade entre mods. Validar IDs/estados e aparências no Forge 1.12.2; backups e testes em cópia de mundo são indispensáveis. Não reaproveitar automaticamente o gerador antigo como se essas garantias já estivessem demonstradas.

## Testes

`JAVA_HOME=/caminho/do/jdk8 sh tests/settlement-prototype/run.sh` compila somente esse planejador e seus testes com JDK 8+, em `tests/settlement-prototype/out/` (ignorado pelo Git), sem Gradle ou Minecraft. Verifica oito biomas, paletas, geometria, acesso aberto, caminho contínuo, limites, determinismo, imutabilidade e rejeição de entradas inválidas. Não testa Minecraft, Forge, mundo, NBT, chunks, permissões, placement ou integração visual; não comprova que seja seguro colocar as maquetes em um mundo.
