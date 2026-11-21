package com.nexusrpg.magma;

/**
 * Opcional. Implemente na entidade do boss (classe comum, não só cliente) para controlar as animações.
 * getAnimTicks() deve contar os ticks desde o início da animação atual.
 * Duração em ticks: PUNCH 38 (golpe final no tick 28), BREATH 56 (fogo do tick 16 ao 46),
 * METEOR 60 (arremesso no tick 20), CHARGE 52, NOVA 60 (explosão no tick 24).
 * O modelo só anima o corpo: chamas, meteoros e ondas de choque você cria na entidade.
 */
public interface IColossoMagmaAnimated {
    int NONE = 0;
    int PUNCH = 1;
    int BREATH = 2;
    int METEOR = 3;
    int CHARGE = 4;
    int NOVA = 5;
    int DEATH = 6;

    int getAnimState();

    int getAnimTicks();

    boolean isEnraged();
}
