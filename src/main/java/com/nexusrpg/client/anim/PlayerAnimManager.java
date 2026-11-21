package com.nexusrpg.client.anim;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Guarda qual animação cada jogador está tocando (somente no cliente). */
@SideOnly(Side.CLIENT)
public final class PlayerAnimManager {
    private static final class Entry {
        final PlayerAnim anim;
        final float start;

        Entry(PlayerAnim anim, float start) {
            this.anim = anim;
            this.start = start;
        }
    }

    private static final Map<UUID, Entry> MAP = new HashMap<UUID, Entry>();

    private PlayerAnimManager() {}

    public static void play(EntityPlayer p, PlayerAnim anim) {
        MAP.put(p.getUniqueID(), new Entry(anim, p.ticksExisted));
    }

    public static void clear() { MAP.clear(); }

    public static void stop(EntityPlayer p) {
        MAP.remove(p.getUniqueID());
    }

    /** Animação ativa neste instante (age = ageInTicks), ou null. */
    public static PlayerAnim get(Entity e, float age) {
        Entry en = MAP.get(e.getUniqueID());
        if (en == null) return null;
        if (en.anim.durationTicks > 0 && age - en.start > en.anim.durationTicks) {
            MAP.remove(e.getUniqueID());
            return null;
        }
        return en.anim;
    }

    /** Segundos desde o início da animação. */
    public static float time(Entity e, float age) {
        Entry en = MAP.get(e.getUniqueID());
        return en == null ? 0F : Math.max(0F, (age - en.start) / 20F);
    }

    /** Efeitos no corpo inteiro: {graus do rolamento para frente, altura do pulo em blocos}, ou null. */
    public static float[] bodyFx(Entity e, float age) {
        PlayerAnim a = get(e, age);
        if (a == PlayerAnim.ROLL) {
            float k = PlayerPose.ease(time(e, age) / a.seconds());
            return new float[] {360F * k, MathHelper.sin(k * (float) Math.PI) * 0.15F};
        }
        if (a == PlayerAnim.VICTORY) {
            float t = time(e, age);
            return new float[] {0F, t < 1.1F ? Math.abs(MathHelper.sin(t * 5.7F)) * 0.3F : 0F};
        }
        return null;
    }
}
