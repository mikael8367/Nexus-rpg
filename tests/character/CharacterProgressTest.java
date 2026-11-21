package com.nexusrpg.character;

import java.util.Random;

/** Pure rule checks; do not replace an in-game integration test. */
public final class CharacterProgressTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(CharacterProgress.required(1)==100 && CharacterProgress.required(2)==130, "curve");
        CharacterProgress.Result r=CharacterProgress.advance(1,99,0,1);
        check(r.level==2 && r.xp==0 && r.points==3, "exact level threshold");
        r=CharacterProgress.advance(1,0,0,231);
        check(r.level==3 && r.xp==1 && r.points==6, "multiple levels and remainder");
        r=CharacterProgress.advance(999999,0,Integer.MAX_VALUE-1,CharacterProgress.required(999999)+1);
        check(r.level==1000000 && r.xp==1 && r.points==Integer.MAX_VALUE, "level and point caps");
        r=CharacterProgress.advance(1000000,Integer.MAX_VALUE,7,Integer.MAX_VALUE);
        check(r.level==1000000 && r.xp==Integer.MAX_VALUE && r.points==7, "XP overflow at max level");
        try { CharacterProgress.advance(1,0,0,-1); throw new AssertionError("negative amount"); }
        catch (IllegalArgumentException expected) { }
        Random random=new Random(1827);
        for (int i=0;i<10000;i++) {
            int level=1+random.nextInt(1000000), xp=random.nextInt(Integer.MAX_VALUE);
            int points=random.nextInt(Integer.MAX_VALUE), amount=random.nextInt(Integer.MAX_VALUE);
            int expectedLevel=level, expectedPoints=points;
            long total=(long)xp+amount;
            while (expectedLevel<1000000) {
                long need=100L+30L*(expectedLevel-1);
                if (total<need) break;
                total-=need; expectedLevel++; expectedPoints=(int)Math.min(Integer.MAX_VALUE,(long)expectedPoints+3);
            }
            r=CharacterProgress.advance(level,xp,points,amount);
            check(r.level==expectedLevel && r.points==expectedPoints && r.xp==(int)Math.min(Integer.MAX_VALUE,total), "legacy equivalence " + i);
        }
        System.out.println("CharacterProgressTest: passed (thresholds, caps, overflow and randomized equivalence)");
    }
}
