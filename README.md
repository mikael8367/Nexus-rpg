# Nexus RPG

Mod de aventura e RPG para **Minecraft Java 1.12.2**, usando **Minecraft Forge 14.23.5.2860**.

## Requisitos

- Java JDK 8
- Git
- Minecraft Forge MDK 1.12.2 (14.23.5.2860)
- Gradle Wrapper incluído no MDK

> Esta versão do Minecraft/Forge usa uma toolchain antiga. Use Java 8; não atualize o Gradle ou o ForgeGradle para versões modernas sem adaptar o projeto.

## Preparar o ambiente

1. Instale o JDK 8 e configure `JAVA_HOME`.
2. Importe/extraia o Forge MDK 1.12.2.
3. Copie os arquivos deste repositório para a pasta do MDK, preservando `gradlew`, `gradlew.bat` e a pasta `gradle/wrapper` fornecidos pelo MDK.
4. No Windows, execute:

   ```bat
   gradlew.bat setupDecompWorkspace
   gradlew.bat idea
   gradlew.bat runClient
   ```

   Para Eclipse, use `gradlew.bat eclipse` no lugar de `idea`.

No Linux/macOS, use `./gradlew` no lugar de `gradlew.bat`.

## Gerar o mod

```bat
gradlew.bat build
```

O JAR será gerado em `build/libs/`.

## Caçadas

Consulte [CACADAS.md](CACADAS.md) para comandos, objetivos, recompensas e limitações das caçadas experimentais.

## Jornadas automáticas

No modo sobrevivência ou aventura, há duas jornadas independentes de exploração e fabricação. Veja [objetivos, recompensas e limitações](tests/journeys/README.md).

## Estrutura

- `com.nexusrpg.NexusRpg`: classe principal e ciclo de vida do mod.
- `init`: registro central de itens, blocos e entidades.
- `bosses`: lógica específica dos chefes.
- `entities`: entidades e criaturas.
- `structures`: estruturas exploráveis.
- `world`: geração e recursos do mundo.
- `items`, `weapons`, `armor`: conteúdo de equipamento.
- `assets/nexusrpg`: modelos, texturas, idiomas e sons.

## Convenções

- Mod ID: `nexusrpg`
- Pacote Java: `com.nexusrpg`
- Java: 8
- Minecraft: 1.12.2
- Forge: 14.23.5.2860

Desenvolvimento em andamento.
