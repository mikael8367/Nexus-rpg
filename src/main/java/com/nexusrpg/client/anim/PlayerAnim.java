package com.nexusrpg.client.anim;

/** Animações do jogador. durationTicks <= 0 significa "mantém enquanto a tecla estiver pressionada". */
public enum PlayerAnim {
    SLASH("Golpe horizontal", 14),
    OVERHEAD("Golpe de cima", 18),
    THRUST("Estocada", 12),
    ROLL("Rolamento", 14),
    GUARD("Guarda", 0),
    DRINK("Beber poção", 30),
    STUN("Atordoado", 36),
    VICTORY("Vitória", 44);

    public final String label;
    public final int durationTicks;

    PlayerAnim(String label, int durationTicks) {
        this.label = label;
        this.durationTicks = durationTicks;
    }

    public float seconds() {
        return durationTicks / 20F;
    }
}
