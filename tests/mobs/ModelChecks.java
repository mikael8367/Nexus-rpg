import com.nexusrpg.mobs.client.*;
import net.minecraft.client.model.*;
import java.lang.reflect.Field;

/** Runs without an OpenGL context against mapped Forge/Minecraft. */
public final class ModelChecks {
    public static void main(String[] args) throws Exception {
        check(new ModelBramble(), "legs", 4);
        check(new ModelCavern(), "legs", 6);
        check(new ModelBasalt(), null, 0);
        check(new ModelStilt(), "legs", 2);
        check(new ModelPrism(), "legs", 4);
        check(new ModelLumenMoth(), "legs", 6);
        check(new ModelDuneMole(), "legs", 4);
        check(new ModelBellback(), "legs", 4);
        check(new ModelBurrower(), "legs", 6);
        check(new ModelRootwarden(), "legs", 2);
        System.out.println("PASS: ten real ModelBase constructors, UV bounds and animation");
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
        if (model instanceof ModelLumenMoth || model instanceof ModelDuneMole || model instanceof ModelBellback) {
            Field f = model.getClass().getDeclaredField(legField); f.setAccessible(true);
            ModelRenderer[] parts = (ModelRenderer[]) f.get(model);
            model.setRotationAngles(0F, 0F, 10F, 0F, 0F, .0625F, null);
            for (ModelRenderer part : parts) if (Math.abs(part.rotateAngleX) > 0.00001F) throw new AssertionError("legs move while idle");
            model.setRotationAngles(0F, .7F, 10F, 0F, 0F, .0625F, null);
            if (Math.abs(parts[0].rotateAngleX) < 0.01F || Math.abs(parts[0].rotateAngleX - parts[1].rotateAngleX) < 0.01F)
                throw new AssertionError("walk animation missing or unsynchronized");
        }
        model.setRotationAngles(1F, .7F, 10F, 20F, 10F, .0625F, null);
        int moving=0;
        for (ModelRenderer part : model.boxList) {
            if (!Float.isFinite(part.rotateAngleX) || !Float.isFinite(part.rotateAngleY) || !Float.isFinite(part.rotateAngleZ)) throw new AssertionError("non-finite angle");
            if (part.rotateAngleX != 0F || part.rotateAngleY != 0F || part.rotateAngleZ != 0F) moving++;
        }
        if (moving < 4) throw new AssertionError("missing animation");
    }
}
