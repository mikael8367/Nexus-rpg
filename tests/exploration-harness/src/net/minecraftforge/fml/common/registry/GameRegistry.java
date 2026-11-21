package net.minecraftforge.fml.common.registry;
import java.util.*;
import net.minecraftforge.fml.common.IWorldGenerator;
public class GameRegistry {
 public static final List<IWorldGenerator> generators = new ArrayList<IWorldGenerator>();
 public static int weight;
 public static void registerWorldGenerator(IWorldGenerator generator, int w) { generators.add(generator); weight=w; }
}
