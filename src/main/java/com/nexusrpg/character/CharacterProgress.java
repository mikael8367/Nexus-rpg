package com.nexusrpg.character;

/** Pure arithmetic for the persisted XP curve. The caller validates profile fields. */
final class CharacterProgress {
    static final int MAX_LEVEL = 1000000;
    private CharacterProgress() {}
    static int required(int level) {
        int safe = Math.max(1, Math.min(MAX_LEVEL, level));
        return (int)Math.min(Integer.MAX_VALUE, 100L + 30L * (safe - 1));
    }
    static Result advance(int level, int xp, int points, int amount) {
        if (level < 1 || level > MAX_LEVEL || xp < 0 || points < 0 || amount < 0)
            throw new IllegalArgumentException("Invalid progress");
        long total = (long)xp + amount;
        while (level < MAX_LEVEL && total >= required(level)) {
            total -= required(level++);
            points = (int)Math.min(Integer.MAX_VALUE, (long)points + 3);
        }
        return new Result(level, (int)Math.min(Integer.MAX_VALUE, total), points);
    }
    static final class Result {
        final int level, xp, points;
        Result(int level, int xp, int points) { this.level=level; this.xp=xp; this.points=points; }
    }
}
