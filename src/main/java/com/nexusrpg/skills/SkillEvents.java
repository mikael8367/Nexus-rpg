package com.nexusrpg.skills;

import com.nexusrpg.character.CharacterProfile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Server authoritative chat controls; no client packet or mod entrypoint hooks needed. */
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class SkillEvents {
    private static final String KEY = "NexusRpgSkills";
    private static final String[] CLASSES = {"warrior", "mage", "archer", "assassin", "paladin", "necromancer"};
    private SkillEvents() {}

    static NBTTagCompound data(EntityPlayer p) {
        NBTTagCompound root = p.getEntityData();
        String key = EntityPlayer.PERSISTED_NBT_TAG;
        if (root.hasKey(key) && !root.hasKey(key, 10)) return null;
        if (!root.hasKey(key)) root.setTag(key, new NBTTagCompound());
        NBTTagCompound persisted = root.getCompoundTag(key);
        if (persisted.hasKey(KEY) && !persisted.hasKey(KEY, 10)) return null;
        if (!persisted.hasKey(KEY)) persisted.setTag(KEY, new NBTTagCompound());
        return persisted.getCompoundTag(KEY);
    }
    private static boolean known(String name) {
        for (String c : CLASSES) if (c.equals(name)) return true;
        return false;
    }
    private static int rank(NBTTagCompound d, String c) { return Math.max(0, Math.min(5, d.getInteger("rank_" + c))); }
    private static int points(NBTTagCompound d, int level) {
        return SkillTreeService.available(d, level);
    }
    private static long now(EntityPlayerMP p) {
        WorldServer overworld = p.getServer().getWorld(0);
        return overworld == null ? p.world.getTotalWorldTime() : overworld.getTotalWorldTime();
    }
    private static void say(EntityPlayer p, String text) { p.sendMessage(new TextComponentString("[Habilidades] " + text)); }
    private static boolean hostile(EntityPlayer p, EntityLivingBase target) {
        return target != p && target.isEntityAlive() && target instanceof IMob
            && !(target instanceof EntityPlayer) && !p.isOnSameTeam(target)
            && !(target instanceof EntityTameable && ((EntityTameable) target).isTamed())
            && !(target instanceof IEntityOwnable && ((IEntityOwnable) target).getOwnerId() != null);
    }
    // Aim by viewing direction and line of sight. Only hostile mobs are eligible, never players or pets.
    private static EntityLivingBase aimed(EntityPlayerMP p, double reach) {
        Vec3d eye = p.getPositionEyes(1.0F), look = p.getLookVec();
        AxisAlignedBB box = p.getEntityBoundingBox().grow(reach);
        EntityLivingBase best = null;
        double nearest = reach + 1;
        for (EntityLivingBase e : p.world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (!hostile(p, e) || !p.canEntityBeSeen(e)) continue;
            Vec3d center = new Vec3d(e.posX, e.posY + e.height * 0.5, e.posZ);
            Vec3d delta = center.subtract(eye);
            double distance = delta.lengthVector();
            if (distance < 0.01 || distance > reach || distance >= nearest) continue;
            if (look.dotProduct(delta.scale(1.0 / distance)) < 0.95) continue;
            best = e; nearest = distance;
        }
        return best;
    }
    private static List<EntityLivingBase> nearby(EntityPlayerMP p, double radius) {
        List<EntityLivingBase> result = new ArrayList<>();
        for (EntityLivingBase e : p.world.getEntitiesWithinAABB(EntityLivingBase.class, p.getEntityBoundingBox().grow(radius))) {
            if (result.size() >= 12) break;
            if (hostile(p, e) && p.getDistanceSq(e) <= radius * radius && p.canEntityBeSeen(e)) result.add(e);
        }
        return result;
    }
    private static void buff(EntityLivingBase target, net.minecraft.potion.Potion potion, int ticks, int amp) {
        target.addPotionEffect(new PotionEffect(potion, ticks, amp));
    }
    static DamageSource skillDamageSource(EntityPlayerMP p, boolean fire) {
        DamageSource source = DamageSource.causePlayerDamage(p);
        return fire ? source.setFireDamage() : source;
    }
    private static void damage(EntityPlayerMP p, EntityLivingBase target, float amount, boolean fire) {
        // LivingEntity.attackEntityFrom checks Fire Resistance, but not the entity's
        // isImmuneToFire flag for arbitrary damage sources (e.g. our player source).
        if (fire && target.isImmuneToFire()) return;
        target.attackEntityFrom(skillDamageSource(p, fire), amount);
    }
    private static boolean combo(NBTTagCompound d, String c, EntityLivingBase target, long tick) {
        return tick <= d.getLong("combo_until_" + c) && tick >= d.getLong("combo_start_" + c)
            && target != null && target.getUniqueID().toString().equals(d.getString("combo_target_" + c));
    }
    private static void mark(NBTTagCompound d, String c, EntityLivingBase target, long tick) {
        d.setLong("combo_start_" + c, tick); d.setLong("combo_until_" + c, SkillTreeService.deadline(tick, 120));
        d.setString("combo_target_" + c, target.getUniqueID().toString());
    }

    @SubscribeEvent
    public static void chat(ServerChatEvent event) {
        String raw = event.getMessage().trim();
        if (!raw.regionMatches(true, 0, "!skill", 0, 6)
            || (raw.length() > 6 && !Character.isWhitespace(raw.charAt(6)))) return;
        event.setCanceled(true);
        EntityPlayerMP p = event.getPlayer();
        if (p.world.isRemote || !p.isEntityAlive() || p.isSpectator()) return;
        String[] args = raw.toLowerCase(java.util.Locale.ROOT).split("\\s+");
        if (args.length > 3 || (args.length == 3 && !"up".equals(args[1]))) { say(p, "Use !skill info, !skill tree, !skill up [id], !skill cast, !skill fire ou !skill ultimate."); return; }
        String cmd = args.length == 1 ? "info" : args[1];
        String c = CharacterProfile.characterClass(p);
        if (!known(c)) { say(p, "Escolha uma classe no sistema de personagem primeiro."); return; }
        NBTTagCompound d = data(p);
        if (d == null) { say(p, "Dados de habilidades incompatíveis; nada foi alterado."); return; }
        int level = Math.max(1, CharacterProfile.level(p));
        int r = rank(d, c);
        if ("info".equals(cmd) || "tree".equals(cmd)) {
            NBTTagCompound snapshot=SkillTreeService.snapshot(p);
            if (!snapshot.getBoolean("ok")) { say(p, snapshot.getString("reason")); return; }
            say(p, "Classe: " + c + "; nível: " + level + "; aperfeiçoamento: " + r + "/5; pontos livres: " + snapshot.getInteger("points") + ". Suprema no nível 15.");
            if ("tree".equals(cmd)) for (SkillTree.Node node:SkillTree.nodes(c)) {
                say(p, node.id + " — " + node.name + " " + SkillTreeService.rank(d,node) + "/" + node.maxRank + ", custo " + node.cost + ": " + node.description);
            }
            else say(p, "!skill tree = árvore; !skill up [id] = gastar ponto(s); !skill cast/ultimate = usar." + ("mage".equals(c) ? " !skill fire = dano de fogo sem incendiar; compartilha a mana e a recarga da ativa normal. Imunidade a fogo pode bloquear." : ""));
            return;
        }
        if ("up".equals(cmd)) {
            String id=args.length==3 ? args[2] : c+".training";
            String result=SkillTreeService.allocate(p,id);
            say(p,result.isEmpty()?"Habilidade aprimorada: " + id + ".":result);
            return;
        }
        if ("fire".equals(cmd)) { SkillTreeService.activateFire(p); return; }
        boolean ultimate = "ultimate".equals(cmd);
        if (!ultimate && !"cast".equals(cmd)) { say(p, "Use !skill info, !skill up, !skill cast, !skill fire ou !skill ultimate."); return; }
        SkillTreeService.activate(p, ultimate);
    }

    // Entrada interna: somente chamada por SkillTreeService após checagem do servidor.
    static void activateServer(EntityPlayerMP p, boolean ultimate, boolean fire) {
        String c=CharacterProfile.characterClass(p);
        if (fire && (ultimate || !"mage".equals(c))) { say(p, "O ataque de fogo é exclusivo do mago."); return; }
        NBTTagCompound d=data(p);
        if (!known(c) || d==null) { say(p, "Dados de habilidades indisponíveis."); return; }
        if (!SkillTreeService.runtimeValid(d)) { say(p, "Dados de habilidades incompatíveis; nada foi alterado."); return; }
        int level=CharacterProfile.level(p), r=rank(d,c);
        if (ultimate && level < 15) { say(p, "A suprema exige nivel 15."); return; }
        long tick = now(p);
        if (tick < 0 || tick > Long.MAX_VALUE - (ultimate ? 1200 : 160)) {
            say(p, "Relógio do mundo inválido; nada foi alterado."); return;
        }
        String timer = (ultimate ? "ultimate_" : "cast_") + c;
        long ready = d.getLong(timer);
        if (ready > tick) { say(p, "Recarga: " + SkillTreeService.secondsRemaining(ready, tick) + " s."); return; }
        EntityLivingBase target = null;
        List<EntityLivingBase> targets = null;
        if (!"paladin".equals(c)) {
            if (ultimate && !"assassin".equals(c)) {
                targets = nearby(p, "archer".equals(c) ? 10 : 7);
                if (targets.isEmpty()) { say(p, "Nenhum monstro hostil visivel ao alcance."); return; }
            } else {
                target = aimed(p, "warrior".equals(c) ? 5 : "assassin".equals(c) ? 6 : 16);
                if (target == null) { say(p, "Mire em um monstro hostil visivel ao alcance."); return; }
            }
        }
        int cost = ultimate ? 30 : 10;
        if (!CharacterProfile.consumeMana(p, cost)) { say(p, "Mana insuficiente (necessario: " + cost + ")."); return; }
        d.setLong(timer, SkillTreeService.deadline(tick, ultimate ? 1200 : 160));
        int intelligence = Math.max(0, Math.min(100, CharacterProfile.intelligence(p)));
        boolean treeValid = SkillTreeService.valid(d);
        int activeRank = treeValid ? SkillTreeService.rank(d, SkillTree.node(c + ".active")) : 0;
        int ultimateRank = treeValid ? SkillTreeService.rank(d, SkillTree.node(c + ".ultimate")) : 0;
        float power = 4.0F + r + Math.min(5.0F, intelligence * 0.05F) + (ultimate ? ultimateRank * 2 : activeRank);
        if ("paladin".equals(c)) {
            if (ultimate) {
                boolean chained = tick >= d.getLong("pal_combo_start") && tick <= d.getLong("pal_combo_until") && d.getLong("pal_combo_until") > 0;
                p.heal((chained ? 10 : 7) + r + ultimateRank * 2);
                buff(p, MobEffects.RESISTANCE, 200 + 40 * ultimateRank, 1);
                d.setLong("pal_combo_until", 0);
            } else {
                p.heal(2 + r * 0.5F + activeRank); buff(p, MobEffects.RESISTANCE, 100, 0);
                d.setLong("pal_combo_start", tick); d.setLong("pal_combo_until", SkillTreeService.deadline(tick, 120));
            }
        } else if (!ultimate) {
            damage(p, target, power, fire);
            // Only self-buffs: applying harmful effects directly to a target could bypass
            // another mod's canceled damage event / territory protection.
            if ("mage".equals(c)) buff(p, MobEffects.RESISTANCE, 60, 0);
            else if ("archer".equals(c)) buff(p, MobEffects.SPEED, 80, 0);
            else if ("assassin".equals(c)) buff(p, MobEffects.INVISIBILITY, 60, 0);
            else if ("necromancer".equals(c)) p.heal(2 + activeRank * 0.5F);
            else buff(p, MobEffects.STRENGTH, 60, 0);
            mark(d, c, target, tick);
        } else {
            if (target != null) { targets = new ArrayList<>(); targets.add(target); }
            for (EntityLivingBase e : targets) {
                boolean chained = combo(d, c, e, tick);
                damage(p, e, power + 4 + (chained ? 3 : 0), false);
            }
            if ("warrior".equals(c)) buff(p, MobEffects.RESISTANCE, 120 + 40 * ultimateRank, 0);
            else if ("mage".equals(c)) buff(p, MobEffects.REGENERATION, 100 + 40 * ultimateRank, 0);
            else if ("archer".equals(c)) buff(p, MobEffects.SPEED, 160 + 40 * ultimateRank, 1);
            else if ("assassin".equals(c)) buff(p, MobEffects.INVISIBILITY, 100 + 40 * ultimateRank, 0);
            else if ("necromancer".equals(c)) p.heal(Math.min(6, targets.size() * 2) + ultimateRank);
            d.setLong("combo_until_" + c, 0);
        }
        say(p, ultimate ? "Suprema ativada!" : fire ? "Ataque de fogo ativado!" : "Habilidade ativada!");
    }

    // Only ordinary hostile melee and projectile attacks can be dodged: never PvP,
    // environment, unblockable, fire, magical or explosive damage. No damage is redirected.
    static boolean dodgeSourceAllowed(DamageSource source, float amount) {
        return source != null && amount > 0 && !Float.isInfinite(amount) && !Float.isNaN(amount)
            && !source.isUnblockable() && !source.isDamageAbsolute() && !source.isExplosion()
            && !source.isMagicDamage() && !source.isFireDamage();
    }
    static boolean dodgeRoll(long tick, long ready, int roll) {
        return tick >= 0 && tick <= Long.MAX_VALUE - 400 && ready >= 0 && tick >= ready
            && roll >= 0 && roll < 15;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void dodge(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = (EntityPlayerMP) event.getEntityLiving();
        if (p.world.isRemote || p.getServer() == null || !p.getServer().isCallingFromMinecraftThread()
            || !p.isEntityAlive() || p.isSpectator() || !"assassin".equals(CharacterProfile.characterClass(p))) return;
        DamageSource source = event.getSource();
        float amount = event.getAmount();
        if (!dodgeSourceAllowed(source, amount) || p.isEntityInvulnerable(source)
            || (p.capabilities.disableDamage && !source.canHarmInCreative())
            || (source.isDifficultyScaled() && p.world.getDifficulty() == EnumDifficulty.PEACEFUL)) return;
        Entity attacker = source.getTrueSource(), immediate = source.getImmediateSource();
        if (!(attacker instanceof EntityLivingBase) || !hostile(p, (EntityLivingBase) attacker)
            || immediate == null || (!source.isProjectile() && immediate != attacker)) return;
        NBTTagCompound d = data(p);
        if (!SkillTreeService.runtimeValid(d)) return;
        long tick = now(p), ready = d.getLong("assassin_dodge_ready_v1");
        if (tick < 0 || tick > Long.MAX_VALUE - 400 || tick < ready) return;
        if (!dodgeRoll(tick, ready, p.world.rand.nextInt(100))) return;
        d.setLong("assassin_dodge_ready_v1", SkillTreeService.deadline(tick, 400));
        event.setCanceled(true);
        say(p, "Esquiva!");
    }

    static float boostedDamage(float amount, float bonus, int training, int passive) {
        return (float)Math.min(Float.MAX_VALUE,
            (double)amount * (1 + bonus + training * 0.01F + passive * 0.03F));
    }
    // All passive combat bonuses are applied only to hostile mobs, never PvP or allies.
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void hurt(LivingHurtEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityPlayerMP) || event.getEntityLiving().world.isRemote || !(event.getAmount() > 0)
            || Float.isInfinite(event.getAmount()) || Float.isNaN(event.getAmount())) return;
        EntityPlayerMP p = (EntityPlayerMP) source;
        if (!p.isEntityAlive() || p.isSpectator()) return;
        EntityLivingBase victim = event.getEntityLiving();
        if (!hostile(p, victim)) return;
        String c = CharacterProfile.characterClass(p);
        if (!known(c)) return;
        NBTTagCompound d = data(p);
        if (!SkillTreeService.runtimeValid(d)) return;
        int r = rank(d, c);
        float bonus = 0;
        if ("warrior".equals(c) && p.getDistanceSq(victim) <= 25) bonus = 0.10F;
        else if ("mage".equals(c)) bonus = Math.min(0.15F, Math.max(0, CharacterProfile.intelligence(p)) * 0.002F);
        else if ("archer".equals(c) && event.getSource().isProjectile()) bonus = 0.15F;
        else if ("assassin".equals(c) && victim.getHealth() < victim.getMaxHealth() * 0.5F) bonus = 0.15F;
        else if ("paladin".equals(c) && victim.isEntityUndead()) bonus = 0.15F;
        int passiveRank = SkillTreeService.valid(d) ? SkillTreeService.rank(d,SkillTree.node(c+".passive")) : 0;
        if (bonus > 0) event.setAmount(boostedDamage(event.getAmount(), bonus, r, passiveRank));
    }
    // This is after armor and LivingHurtEvent cancellation, but a later listener can
    // still cancel LivingDamageEvent; Forge 1.12 has no uncancellable post-damage event.
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void leech(LivingDamageEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityPlayerMP) || event.getEntityLiving().world.isRemote || !(event.getAmount() > 0)
            || Float.isInfinite(event.getAmount()) || Float.isNaN(event.getAmount())) return;
        EntityPlayerMP p = (EntityPlayerMP) source;
        if (!p.isEntityAlive() || p.isSpectator() || !hostile(p, event.getEntityLiving())
            || !"necromancer".equals(CharacterProfile.characterClass(p))) return;
        NBTTagCompound d = data(p);
        if (!SkillTreeService.runtimeValid(d)) return;
        long t = now(p);
        if (t < 0 || t > Long.MAX_VALUE - 200) return;
        if (t >= d.getLong("leech_ready")) {
            int passiveRank = SkillTreeService.rank(d,SkillTree.node("necromancer.passive"));
            // Reserve before healing: another mod may re-enter damage handling during heal.
            d.setLong("leech_ready", SkillTreeService.deadline(t, 200));
            p.heal(1 + rank(d, "necromancer") * 0.2F + passiveRank * 0.5F);
        }
    }
    private static void clearCombos(NBTTagCompound skills) {
        for (String c : CLASSES) {
            skills.removeTag("combo_start_" + c);
            skills.removeTag("combo_until_" + c);
            skills.removeTag("combo_target_" + c);
        }
        skills.removeTag("pal_combo_start");
        skills.removeTag("pal_combo_until");
    }
    // An entire allocation tree is one snapshot: merging independently spent
    // points or unknown nested fields can create a combination nobody earned.
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.getEntityPlayer().world.isRemote) return;
        cloneSkillData(event.getOriginal().getEntityData(), event.getEntityPlayer().getEntityData(), event.isWasDeath());
    }
    static void cloneSkillData(NBTTagCompound oldRoot, NBTTagCompound root, boolean death) {
        String persistedKey=EntityPlayer.PERSISTED_NBT_TAG;
        // A repeated death event may see the already-cleared result. It is not
        // a conflicting allocation snapshot and must not generate a backup.
        if (death && oldRoot.hasKey(persistedKey,10) && root.hasKey(persistedKey,10)) {
            NBTTagCompound oldPersisted=oldRoot.getCompoundTag(persistedKey), destPersisted=root.getCompoundTag(persistedKey);
            if (oldPersisted.hasKey(KEY,10) && destPersisted.hasKey(KEY,10)) {
                NBTTagCompound original=oldPersisted.getCompoundTag(KEY);
                if (SkillTreeService.runtimeValid(original)) {
                    NBTTagCompound cleared=original.copy();
                    clearCombos(cleared);
                    if (!cleared.equals(original) && cleared.equals(destPersisted.getCompoundTag(KEY))) {
                        com.nexusrpg.character.CharacterEvents.carryCloneBackups(oldRoot,root);
                        return;
                    }
                }
            }
        }
        if (!com.nexusrpg.character.CharacterEvents.cloneSection(oldRoot,root,KEY)) return;
        NBTTagCompound skills=root.getCompoundTag(persistedKey).getCompoundTag(KEY);
        if (death && SkillTreeService.runtimeValid(skills)) {
            NBTTagCompound cleared=skills.copy();
            clearCombos(cleared);
            if (!cleared.equals(skills)) {
                // These validated keys are short-lived combat state, not progress.
                // Saving a whole skill tree on every ordinary death grows the
                // player save without bound; conflicting snapshots still get backups.
                root.getCompoundTag(persistedKey).setTag(KEY,cleared);
            }
        }
    }
}
