import com.nexusrpg.mobs.client.ModelBurrower;
import com.nexusrpg.mobs.client.ModelRootwarden;
import net.minecraft.client.model.*;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import java.lang.reflect.Field;

/** Real mapped Minecraft classes, no OpenGL rendering or game session. */
public final class CreatureChecks {
    private static void model(ModelBase m, int n) throws Exception {
        if (m.textureWidth != 64 || m.textureHeight != 64) throw new AssertionError("texture dimensions");
        Field lf=m.getClass().getDeclaredField("legs"); lf.setAccessible(true);
        ModelRenderer[] legs=(ModelRenderer[])lf.get(m); if (legs.length!=n) throw new AssertionError("leg count");
        Field qf=ModelBox.class.getDeclaredField("quadList"); qf.setAccessible(true);
        int boxes=0;
        for(ModelRenderer part:m.boxList) for(ModelBox box:part.cubeList) {
            boxes++;
            for(TexturedQuad q:(TexturedQuad[])qf.get(box)) for(PositionTextureVertex v:q.vertexPositions)
                if(v.texturePositionX<0 || v.texturePositionX>1 || v.texturePositionY<0 || v.texturePositionY>1) throw new AssertionError("UV out of range");
        }
        if(boxes<10) throw new AssertionError("too few boxes");
        m.setRotationAngles(0,0,0,0,0,0.0625F,null);
        float before=n==6?legs[0].rotateAngleY:legs[0].rotateAngleX;
        m.setRotationAngles(1F,0.8F,10F,20F,10F,0.0625F,null);
        float after=n==6?legs[0].rotateAngleY:legs[0].rotateAngleX;
        if(before==after) throw new AssertionError("no walk animation");
        for(ModelRenderer p:m.boxList) if(!Float.isFinite(p.rotateAngleX)||!Float.isFinite(p.rotateAngleY)||!Float.isFinite(p.rotateAngleZ)) throw new AssertionError("invalid rotation");
    }
    public static void main(String[] args) throws Exception {
        model(new ModelBurrower(),6); model(new ModelRootwarden(),2);
        System.out.println("PASS two real model constructors, anatomy, UV bounds, animations");
        Bootstrap.register(); LootTableManager manager=new LootTableManager(null);
        for(String id:new String[]{"dune_burrower","root_warden"}) {
            LootTable table=manager.getLootTableFromLocation(new ResourceLocation("nexusrpg","entities/"+id));
            if(table==LootTable.EMPTY_LOOT_TABLE || table.getPool(id+"_common")==null || table.getPool(id+"_extra")==null) throw new AssertionError("missing loot pools "+id);
        }
        System.out.println("PASS Minecraft/Forge loot loader: 4 pools");
    }
}
