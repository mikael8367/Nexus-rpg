import com.nexusrpg.mobs.client.*;
import net.minecraft.client.model.*;
import java.lang.reflect.Field;

/** Runs without an OpenGL context against mapped Forge/Minecraft. */
public final class ModelChecks {
    public static void main(String[] args) throws Exception {
        check(new ModelBramble(), "legs", 4);
        check(new ModelCavern(), "legs", 6);
        check(new ModelBasalt(), null, 0);
        System.out.println("PASS: three real ModelBase constructors, UV bounds and walk animation");
    }
    private static void check(ModelBase model, String legField, int legs) throws Exception {
        if (model.textureWidth != 64 || model.textureHeight != 64) throw new AssertionError("texture size");
        if (legField != null) {
            Field f = model.getClass().getDeclaredField(legField); f.setAccessible(true);
            if (((Object[])f.get(model)).length != legs) throw new AssertionError("leg count");
        }
        Field quads = ModelBox.class.getDeclaredField("quadList"); quads.setAccessible(true);
        int boxes=0;
        for (ModelRenderer part : model.boxList) for (ModelBox box : part.cubeList) {
            boxes++;
            for (TexturedQuad quad : (TexturedQuad[])quads.get(box)) for (PositionTextureVertex v : quad.vertexPositions) {
                if (v.texturePositionX < 0 || v.texturePositionX > 1 || v.texturePositionY < 0 || v.texturePositionY > 1)
                    throw new AssertionError(model.getClass().getSimpleName()+" UV outside texture: "+v.texturePositionX+", "+v.texturePositionY);
            }
        }
        if (boxes < 6) throw new AssertionError("missing anatomy");
        model.setRotationAngles(1F, .7F, 10F, 20F, 10F, .0625F, null);
        int moving=0;
        for (ModelRenderer part : model.boxList) {
            if (!Float.isFinite(part.rotateAngleX) || !Float.isFinite(part.rotateAngleY) || !Float.isFinite(part.rotateAngleZ)) throw new AssertionError("non-finite angle");
            if (part.rotateAngleX != 0F || part.rotateAngleY != 0F || part.rotateAngleZ != 0F) moving++;
        }
        if (moving < 4) throw new AssertionError("missing animation");
    }
}
