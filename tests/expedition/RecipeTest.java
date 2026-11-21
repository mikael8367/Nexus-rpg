package com.nexusrpg.expedition;

import com.google.gson.*;
import java.io.FileReader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;

public final class RecipeTest {
    static void check(boolean b, String msg) { if (!b) throw new AssertionError(msg); }
    static class DummyContainer extends Container { public boolean canInteractWith(EntityPlayer p) { return true; } }
    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        String[] names = {"quenching_salve", "divers_reserve", "lantern_dust"};
        Item[] output = {ExpeditionRegistry.QUENCH, ExpeditionRegistry.DIVE, ExpeditionRegistry.SIGHT};
        for (int k=0;k<names.length;k++) {
            JsonObject json;
            try (FileReader reader = new FileReader(System.getProperty("expedition.root", ".") + "/src/main/resources/assets/nexusrpg/recipes/"+names[k]+".json")) {
                json = new JsonParser().parse(reader).getAsJsonObject();
            }
            check(json.get("type").getAsString().equals("minecraft:crafting_shapeless"), "type");
            check(json.getAsJsonObject("result").get("item").getAsString().equals(output[k].getRegistryName().toString()), "output id");
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            check(ingredients.size()==3, "ingredient count");
            NonNullList<Ingredient> list = NonNullList.create();
            Item[] items = new Item[3];
            for (int i=0;i<3;i++) {
                String id = ingredients.get(i).getAsJsonObject().get("item").getAsString();
                items[i] = Item.getByNameOrId(id);
                check(items[i] != null && items[i] != Items.AIR, "unknown ingredient "+id);
                list.add(Ingredient.fromItem(items[i]));
            }
            ShapelessRecipes recipe = new ShapelessRecipes("", new ItemStack(output[k]), list);
            InventoryCrafting grid = new InventoryCrafting(new DummyContainer(), 3, 3);
            grid.setInventorySlotContents(0, new ItemStack(items[2]));
            grid.setInventorySlotContents(4, new ItemStack(items[0]));
            grid.setInventorySlotContents(8, new ItemStack(items[1]));
            check(recipe.matches(grid,null), "permuted recipe " + names[k]);
            check(recipe.getCraftingResult(grid).getItem()==output[k] && recipe.getCraftingResult(grid).getCount()==1, "crafted output");
            grid.setInventorySlotContents(8, ItemStack.EMPTY); check(!recipe.matches(grid,null), "missing ingredient");
            grid.setInventorySlotContents(8, new ItemStack(items[1]));
            grid.setInventorySlotContents(1, new ItemStack(Items.STICK)); check(!recipe.matches(grid,null), "extra ingredient");
        }
        System.out.println("RecipeTest passed");
    }
}
