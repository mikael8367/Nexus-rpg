package com.nexusrpg.achievements;

/** Tests the production health gate only; not a Forge event-bus test. */
public final class ConfirmedDeadHealthTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(AchievementEvents.confirmedDeadHealth(0.0F), "zero health");
        check(AchievementEvents.confirmedDeadHealth(-0.0F), "negative zero");
        check(AchievementEvents.confirmedDeadHealth(-1.0F), "negative finite health");
        check(!AchievementEvents.confirmedDeadHealth(Float.MIN_VALUE), "positive health");
        check(!AchievementEvents.confirmedDeadHealth(Float.NaN), "NaN health");
        check(!AchievementEvents.confirmedDeadHealth(Float.POSITIVE_INFINITY), "positive infinite health");
        check(!AchievementEvents.confirmedDeadHealth(Float.NEGATIVE_INFINITY), "negative infinite health");
        System.out.println("ConfirmedDeadHealthTest passed");
    }
}
