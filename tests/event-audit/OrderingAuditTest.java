package com.nexusrpg.achievements;

import com.nexusrpg.character.CharacterEvents;
import com.nexusrpg.skills.SkillEvents;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Audit model, not an in-game integration test: real annotations and real
 * PendingDeaths, but callbacks/side effects and event ordering are simulated. */
public final class OrderingAuditTest {
    private static final class CancelableEvent extends Event {
        @Override public boolean isCancelable() { return true; }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static SubscribeEvent handler(Class<?> type, String name, Class<?> event) throws Exception {
        SubscribeEvent annotation = type.getMethod(name, event).getAnnotation(SubscribeEvent.class);
        check(annotation != null, "missing subscription: " + type.getName() + "." + name);
        return annotation;
    }
    // Simulates a listener at the same LOWEST priority registered after Nexus.
    // It is not the Forge EventBus and does not call production handlers.
    private static void lateCancellation(SubscribeEvent annotation, String name) {
        check(annotation.priority() == EventPriority.LOWEST && !annotation.receiveCanceled(), name + " annotation changed; reassess");
        CancelableEvent event = new CancelableEvent();
        int[] sideEffect = {0};
        if (!event.isCanceled() || annotation.receiveCanceled()) sideEffect[0]++;
        event.setCanceled(true);
        check(event.isCanceled() && sideEffect[0] == 1, name + " modeled side effect survives later cancellation");
        System.out.println("CONFIRMED MODEL: " + name + " can run before a same-priority late cancellation");
    }
    public static void main(String[] args) throws Exception {
        lateCancellation(handler(CharacterEvents.class, "death", LivingDeathEvent.class), "XP");
        SubscribeEvent zombie = handler(ZombieDeathEvidence.class, "death", LivingDeathEvent.class);
        check(zombie.priority() == EventPriority.LOWEST && zombie.receiveCanceled(), "zombie candidate subscription changed; reassess");
        System.out.println("Zombie candidate only: separate behavioral tests are required");
        lateCancellation(handler(SkillEvents.class, "leech", LivingDamageEvent.class), "necromancer leech");
        PendingDeaths<Object, CancelableEvent> pending = new PendingDeaths<Object, CancelableEvent>();
        Object victim = new Object();
        CancelableEvent cancelled = new CancelableEvent();
        pending.offer(victim, cancelled, 0);
        cancelled.setCanceled(true);
        check(pending.drain(2).isEmpty(), "unconfirmed canceled candidate should be dropped");
        Object victim2 = new Object();
        CancelableEvent death = new CancelableEvent();
        pending.offer(victim2, death, 2);
        pending.confirm(victim2);
        check(pending.drain(4).size() == 1, "confirmed candidate should be drained");
        System.out.println("PendingDeaths: unconfirmed discarded, confirmed drained (model only)");
    }
}
