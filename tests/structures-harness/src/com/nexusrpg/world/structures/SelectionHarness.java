package com.nexusrpg.world.structures;

import java.util.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;

/** Exhaustive ticket/roll tests with fake worlds; not a Minecraft runtime test. */
public class SelectionHarness {
    static void ok(boolean v, String m) { if (!v) throw new AssertionError(m); }
    static final class Rolls extends Random {
        final int candidate, ticket, expectedTickets;
        int calls;
        Rolls(int candidate, int ticket, int expectedTickets) {
            this.candidate=candidate; this.ticket=ticket; this.expectedTickets=expectedTickets;
        }
        @Override public int nextInt(int bound) {
            calls++;
            if (calls==1) { ok(bound==4,"candidate bound"); return candidate; }
            ok(calls==2 && bound==expectedTickets && ticket<bound,"selection bound");
            return ticket;
        }
        @Override public long nextLong() { return 123; }
    }
    public static void main(String[] args) {
        final int[] spawned={0};
        WizardTower.setSpawnHandler((world, feet) -> spawned[0]++);
        for (int style=0;style<8;style++) {
            int nonTower=NovaHarness.count[style]+1;
            int total=nonTower+3;
            for (int ticket=0;ticket<total;ticket++) {
                Chunk chunk=new Chunk(-2,3,NovaHarness.bs[style],64);
                World world=new World(chunk);
                int before=spawned[0];
                Rolls rolls=new Rolls(0,ticket,total);
                new NexusStructureGenerator().generate(rolls,chunk.x,chunk.z,world,null,(x,z)->{
                    ok(x==chunk.x && z==chunk.z,"neighbor chunk queried"); return chunk;
                });
                ok(rolls.calls==2,"selection not reached");
                ok(chunk.tiles.containsKey(new BlockPos(-24,65,56)),"no chest style="+style+" ticket="+ticket);
                ok(spawned[0]-before==(ticket>=nonTower ? 1 : 0),"wrong ticket type style="+style+" ticket="+ticket);
                for (BlockPos pos:world.writes)
                    ok((pos.getX()>>4)==chunk.x && (pos.getZ()>>4)==chunk.z,"write crossed chunk");
            }
            for (int roll=1;roll<4;roll++) {
                Chunk chunk=new Chunk(-2,3,NovaHarness.bs[style],64);
                World world=new World(chunk);
                Rolls rolls=new Rolls(roll,0,total);
                new NexusStructureGenerator().generate(rolls,chunk.x,chunk.z,world,null,(x,z)->{
                    throw new AssertionError("provider queried for rejected candidate");
                });
                ok(rolls.calls==1 && world.writes.isEmpty(),"candidate skip");
            }
            // All tower tickets still fail closed on saved chunks and unsafe foundations.
            for (int ticket=nonTower;ticket<total;ticket++) {
                Chunk old=new Chunk(-2,3,NovaHarness.bs[style],64); World ow=new World(old);
                NexusStructureGenerator gen=new NexusStructureGenerator();
                gen.onChunkDataLoad(new ChunkDataEvent.Load(ow,old));
                int before=spawned[0];
                gen.generate(new Rolls(0,ticket,total),old.x,old.z,ow,null,(x,z)->old);
                ok(ow.writes.isEmpty() && spawned[0]==before,"saved chunk changed");
                Chunk unsafe=new Chunk(-2,3,NovaHarness.bs[style],64); World uw=new World(unsafe);
                unsafe.overlay.put(new BlockPos(-24,62,56),net.minecraft.init.Blocks.AIR.getDefaultState());
                new NexusStructureGenerator().generate(new Rolls(0,ticket,total),unsafe.x,unsafe.z,uw,null,(x,z)->unsafe);
                ok(uw.writes.isEmpty() && spawned[0]==before,"unsafe foundation changed");
            }
        }
        WizardTower.setSpawnHandler(null);
        System.out.println("PASS candidate rolls and every weighted ticket in 8 biomes; saved chunks, foundations, spawn and chunk bounds");
    }
}
