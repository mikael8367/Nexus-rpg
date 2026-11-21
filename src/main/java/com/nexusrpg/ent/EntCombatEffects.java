package com.nexusrpg.ent;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

/**
 * Efeitos cosmeticos enviados somente pelo servidor; nao causam dano nem leem,
 * criam ou alteram blocos. Os avisos ficam na altura dos pes e podem aparecer
 * dentro de blocos ou no ar em terreno irregular. Configuracoes de particulas,
 * latencia e sincronizacao podem atrasar ou ocultar a visualizacao no cliente.
 * Os sons de ataque e rugido pertencem exclusivamente a entidade.
 */
public final class EntCombatEffects {
    private EntCombatEffects() { }

    /**
     * Uma chamada apos incrementar o contador sincronizado de cada tick ativo.
     * O vetor do SLAM e a direcao horizontal normalizada travada no inicio do
     * ataque. Raios e angulo correspondem ao teste horizontal de dano, mas a
     * altura, visibilidade e demais condicoes nao sao delimitadas pelas particulas.
     * SLAM avisa em 4, 12 e 18 e impacta em 22; SPIN avisa em 4, 12, 17,
     * 37 e 57 e impacta em 20, 40 e 60. ROAR nao tem dano de area.
     */
    public static void onAnimationTick(EntityLivingBase ent, int state, int tick,
                                       double facingX, double facingZ) {
        WorldServer world = serverWorld(ent);
        if (world == null) return;
        switch (state) {
            case IEntAnciaoAnimated.SLAM:
                if (tick == 4 || tick == 12 || tick == 18) {
                    perimeter(world, ent, 3.2D, true, facingX, facingZ, false);
                } else if (tick == 22) {
                    perimeter(world, ent, 3.2D, true, facingX, facingZ, true);
                }
                break;
            case IEntAnciaoAnimated.SPIN:
                if (tick == 4 || tick == 12 || tick == 17 || tick == 37 || tick == 57) {
                    perimeter(world, ent, 3.6D, false, facingX, facingZ, false);
                } else if (tick == 20 || tick == 40 || tick == 60) {
                    perimeter(world, ent, 3.6D, false, facingX, facingZ, true);
                }
                break;
            case IEntAnciaoAnimated.ROAR:
                if (tick == 12 || tick == 24 || tick == 36) {
                    leaves(world, ent.posX, ent.posY + ent.height * 0.72D, ent.posZ, 8, 0.65D, 0.35D);
                }
                break;
            default:
                break;
        }
    }

    /** Uma vez no inicio do rugido de furia; nao emite outro som. */
    public static void rageStart(EntityLivingBase ent) {
        WorldServer world = serverWorld(ent);
        if (world == null) return;
        leaves(world, ent.posX, ent.posY + ent.height * 0.55D, ent.posZ, 12, 0.85D, 0.6D);
    }

    private static WorldServer serverWorld(EntityLivingBase ent) {
        if (ent == null || !ent.isEntityAlive() || ent.world == null || ent.world.isRemote
                || !(ent.world instanceof WorldServer)) return null;
        return (WorldServer) ent.world;
    }

    private static void perimeter(WorldServer world, EntityLivingBase ent, double radius,
                                  boolean slam, double facingX, double facingZ, boolean impact) {
        // Use the same dot-product threshold as the horizontal SLAM hit test.
        int points = slam ? 17 : 24;
        double center = Math.atan2(facingZ, facingX);
        double halfArc = Math.acos(0.6428D);
        for (int i = 0; i < points; i++) {
            double angle = slam ? center - halfArc + 2.0D * halfArc * i / (points - 1)
                                : 2.0D * Math.PI * i / points;
            double x = ent.posX + Math.cos(angle) * radius;
            double z = ent.posZ + Math.sin(angle) * radius;
            if (impact) {
                world.spawnParticle(EnumParticleTypes.BLOCK_DUST, x, ent.posY + 0.12D, z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D, Block.getStateId(Blocks.DIRT.getDefaultState()));
            } else {
                world.spawnParticle(EnumParticleTypes.VILLAGER_ANGRY, x, ent.posY + 0.15D, z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void leaves(WorldServer world, double x, double y, double z, int count,
                               double horizontalSpread, double verticalSpread) {
        world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, y, z, count,
                horizontalSpread, verticalSpread, horizontalSpread, 0.02D,
                Block.getStateId(Blocks.LEAVES.getDefaultState()));
    }
}
