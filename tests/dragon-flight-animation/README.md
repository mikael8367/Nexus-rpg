# Animação dos dragões

Execute `python3 tests/dragon-flight-animation/run.py`; requer JDK 8 e Forge 1.12.2 mapeado, com caminhos configuráveis em `JAVA8_HOME`, `GRADLE_USER_HOME` e `FORGE_MAPPED_JAR`. O teste instancia o modelo real e usa um EntityLiving de teste sem carregar o jogo nem OpenGL.

No ar, a pose inclina o corpo e a cauda, recolhe as patas e usa batidas de asas espelhadas com variação de abertura. No chão, em água ou quando o dragão monta outra entidade, permanece a animação discreta e a marcha; um dragão que carrega passageiro pode exibir a pose aérea. Como não há um estado vanilla genérico de voo disponível, esta é uma aproximação visual: durante saltos e quedas fora d'água também aparecerá a pose aérea. As transições são imediatas, sem interpolação de estado entre quadros, para evitar compartilhar estado entre entidades renderizadas pelo mesmo modelo.

**Não verificado dentro do Minecraft:** aparência real, inclinação, amplitude, possíveis sobreposições das membranas com corpo/terreno e a suavidade visual de pouso/decolagem. Testes sem jogo não demonstram voo físico nem sincronização visual com a futura implementação de voo.
