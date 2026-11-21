package com.nexusrpg.ent;

/**
 * Opcional. Implemente na entidade do boss (classe comum, não só cliente) para controlar as animações.
 * getAnimTicks() deve contar os ticks desde o início da animação atual.
 * Duração: ROAR 60 ticks, SPIN 80 ticks, SLAM 52 ticks (o golpe cai no tick 20 a 24).
 */
public interface IEntAnciaoAnimated {
    int NONE = 0;
    int ROAR = 1;
    int SPIN = 2;
    int SLAM = 3;
    int DEATH = 4;

    int getAnimState();

    int getAnimTicks();

    boolean isEnraged();
}
