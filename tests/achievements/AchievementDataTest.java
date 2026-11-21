package com.nexusrpg.achievements;

import com.nexusrpg.character.CharacterEvents;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.nbt.*;

public final class AchievementDataTest {
    private static void check(boolean b, String message) { if (!b) throw new AssertionError(message); }
    private static NBTTagCompound section(NBTTagCompound root) { return root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getCompoundTag(AchievementData.KEY); }
    private static void immutable(NBTTagCompound root, String message) {
        NBTTagCompound before = root.copy();
        check(AchievementData.status(root) == -1, message + " status");
        check(!AchievementData.recordSkeleton(root, UUID.randomUUID()) && !AchievementData.visit(root, 1), message + " rejects updates");
        check(root.equals(before), message + " unchanged");
    }
    public static void main(String[] args) throws Exception {
        net.minecraftforge.fml.common.Mod.EventBusSubscriber subscriber = AchievementEvents.class.getAnnotation(net.minecraftforge.fml.common.Mod.EventBusSubscriber.class);
        check(subscriber != null && subscriber.modid().equals("nexusrpg"), "event bus auto-registration annotation");
        net.minecraftforge.fml.common.eventhandler.SubscribeEvent death = AchievementEvents.class.getMethod("death", net.minecraftforge.event.entity.living.LivingDeathEvent.class).getAnnotation(net.minecraftforge.fml.common.eventhandler.SubscribeEvent.class);
        check(death != null && !death.receiveCanceled() && death.priority() == net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST, "death handler honors cancellation");
        net.minecraftforge.fml.common.eventhandler.SubscribeEvent drops = AchievementEvents.class.getMethod("drops", net.minecraftforge.event.entity.living.LivingDropsEvent.class).getAnnotation(net.minecraftforge.fml.common.eventhandler.SubscribeEvent.class);
        check(drops != null && drops.receiveCanceled(), "canceled drops still confirm death");
        check(AchievementEvents.class.getMethod("dimension", net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent.class).getAnnotation(net.minecraftforge.fml.common.eventhandler.SubscribeEvent.class) != null, "immediate dimension handler registered");
        PendingDeaths<Object, String> pending = new PendingDeaths<Object, String>();
        Object victim = new Object(), cancelled = new Object();
        pending.offer(victim, "confirmed", 10); pending.offer(cancelled, "cancelled", 10);
        pending.confirm(victim);
        check(pending.drain(11).isEmpty() && pending.size() == 2, "never counts in same tick");
        check(pending.drain(12).equals(java.util.Collections.singletonList("confirmed")) && pending.size() == 0, "unconfirmed death discarded later");
        for (int i = 0; i < PendingDeaths.LIMIT + 10; i++) pending.offer(new Object(), "x", 20);
        check(pending.size() == PendingDeaths.LIMIT, "pending queue bounded");
        check(pending.drain(22).isEmpty() && pending.size() == 0, "unconfirmed queue expires");
        Object previous = new Object(); pending.offer(previous, "first", 30); pending.confirm(previous);
        pending.offer(previous, "second", 30);
        check(pending.drain(32).equals(java.util.Collections.singletonList("first")), "confirmed candidate not overwritten");
        pending.offer(previous, "old", 40); pending.confirm(previous);
        check(pending.drain(1).isEmpty() && pending.size() == 0, "counter reset discards stale candidates");
        NBTTagCompound root = new NBTTagCompound(); root.setString("other", "preserve");
        check(!AchievementData.visit(root, 99) && AchievementData.status(root) == 0, "unknown dimensions don't create data");
        check(!AchievementData.recordSkeleton(root, null) && AchievementData.status(root) == 0, "null doesn't create data");
        UUID first = UUID.randomUUID();
        check(AchievementData.recordSkeleton(root, first), "first skeleton");
        check(!AchievementData.recordSkeleton(root, first) && AchievementData.skeletons(root) == 1, "duplicate ignored");
        check(AchievementData.visit(root, 0) && !AchievementData.visit(root, 0), "overworld once");
        check(AchievementData.visit(root, -1) && AchievementData.visit(root, 1), "other realms");
        check(AchievementData.dimensions(root) == 7 && !AchievementData.visit(root, 1), "exploration complete once");
        NBTTagCompound clone = new NBTTagCompound();
        check(CharacterEvents.cloneSection(root, clone, AchievementData.KEY), "clone copies section");
        check(AchievementData.skeletons(clone) == 1 && AchievementData.dimensions(clone) == 7, "clone retains progress");
        for (int i = 1; i < AchievementData.SKELETON_GOAL; i++) check(AchievementData.recordSkeleton(clone, UUID.randomUUID()), "distinct kill");
        check(!AchievementData.recordSkeleton(clone, UUID.randomUUID()) && AchievementData.skeletons(clone) == 8, "complete stays capped");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(clone, bytes);
        NBTTagCompound restored = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray()));
        check(AchievementData.skeletons(restored) == 8 && AchievementData.dimensions(restored) == 7, "compressed save roundtrip");
        section(restored).setString("future_field", "untouched");
        NBTTagCompound clonedAgain = new NBTTagCompound(); CharacterEvents.cloneSection(restored, clonedAgain, AchievementData.KEY);
        check(section(clonedAgain).getString("future_field").equals("untouched"), "unknown fields survive clone");
        NBTTagCompound conflicting = new NBTTagCompound(); AchievementData.visit(conflicting, 0);
        CharacterEvents.cloneSection(restored, conflicting, AchievementData.KEY);
        check(AchievementData.skeletons(conflicting) == 8, "original clone authoritative");
        boolean backup = false; for (String key : conflicting.getKeySet()) if (key.startsWith("NexusRpgCloneBackup_v1_")) backup = true;
        check(backup, "conflicting destination backed up");
        NBTTagCompound futureDestination = root.copy(); section(futureDestination).setInteger("version", 2);
        NBTTagCompound futureBefore = section(futureDestination).copy();
        check(!AchievementData.cloneProgress(restored, futureDestination), "future destination protected");
        check(section(futureDestination).equals(futureBefore), "future destination unchanged");
        backup = false; for (String key : futureDestination.getKeySet()) if (key.startsWith("NexusRpgCloneBackup_v1_")) backup = true;
        check(backup, "source backed up when destination unsupported");
        NBTTagCompound once = futureDestination.copy(); AchievementData.cloneProgress(restored, futureDestination);
        check(once.equals(futureDestination), "unsupported clone replay is idempotent");
        NBTTagCompound extraDestination = root.copy(); section(extraDestination).setString("unknown_extension", "preserve");
        NBTTagCompound extraBefore = section(extraDestination).copy();
        check(!AchievementData.cloneProgress(restored, extraDestination) && section(extraDestination).equals(extraBefore), "destination extension retained");
        NBTTagCompound goodDestination = root.copy(); NBTTagCompound goodBefore = section(goodDestination).copy();
        check(!AchievementData.cloneProgress(futureDestination, goodDestination), "unsupported source cannot overwrite valid destination");
        check(section(goodDestination).equals(goodBefore), "valid destination retained");
        NBTTagCompound malformedDestination = new NBTTagCompound(); malformedDestination.setString(EntityPlayer.PERSISTED_NBT_TAG, "do-not-replace");
        check(!AchievementData.cloneProgress(root, malformedDestination) && malformedDestination.getString(EntityPlayer.PERSISTED_NBT_TAG).equals("do-not-replace"), "malformed destination persisted remains intact");
        NBTTagCompound unsupportedOnly = new NBTTagCompound();
        AchievementData.cloneProgress(futureDestination, unsupportedOnly);
        check(section(unsupportedOnly).equals(section(futureDestination)), "unsupported source retained when destination absent");
        AttributeMap map = new AttributeMap();
        IAttributeInstance attribute = map.registerAttribute(new RangedAttribute(null,"test.armor",0,0,100));
        AchievementEvents.bonus(attribute, AchievementEvents.EXPLORER_BONUS, "test", true);
        AchievementEvents.bonus(attribute, AchievementEvents.EXPLORER_BONUS, "test", true);
        check(attribute.getAttributeValue() == 1 && attribute.getModifiers().size() == 1, "bonus idempotent");
        AchievementEvents.bonus(attribute, AchievementEvents.EXPLORER_BONUS, "test", false);
        check(attribute.getAttributeValue() == 0, "unearned bonus removed");
        NBTTagCompound bad = new NBTTagCompound(); bad.setString(EntityPlayer.PERSISTED_NBT_TAG, "bad"); immutable(bad, "malformed persisted");
        bad = root.copy(); section(bad).setInteger("version", 2); immutable(bad, "future version");
        bad = root.copy(); section(bad).setInteger("dimensions", 8); immutable(bad, "invalid dimensions");
        bad = root.copy(); NBTTagList wrong = new NBTTagList(); wrong.appendTag(new NBTTagInt(1)); section(bad).setTag("skeletons", wrong); immutable(bad, "wrong list type");
        bad = root.copy(); NBTTagList emptyWrong = new NBTTagList(); emptyWrong.appendTag(new NBTTagInt(7)); emptyWrong.removeTag(0); section(bad).setTag("skeletons", emptyWrong); immutable(bad, "empty list with wrong element type");
        bad = root.copy(); bad.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).setString(AchievementData.KEY, "bad"); immutable(bad, "malformed section");
        bad = root.copy(); NBTTagList dup = new NBTTagList(); dup.appendTag(new NBTTagString(first.toString())); dup.appendTag(new NBTTagString(first.toString())); section(bad).setTag("skeletons", dup); immutable(bad, "duplicate UUID in saved data");
        bad = root.copy(); NBTTagList malformed = new NBTTagList(); malformed.appendTag(new NBTTagString("not-a-uuid")); section(bad).setTag("skeletons", malformed); immutable(bad, "bad UUID");
        bad = root.copy(); section(bad).removeTag("dimensions"); immutable(bad, "missing field");
        check(root.getString("other").equals("preserve"), "other data preserved");
        System.out.println("AchievementDataTest passed");
    }
}
