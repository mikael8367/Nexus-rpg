import com.nexusrpg.mage.EntityTowerMage;
import com.nexusrpg.mage.ModelTowerMage;
import java.lang.reflect.Field;
import net.minecraft.client.model.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import sun.misc.Unsafe;

/** Mapped-Minecraft model and transient status checks without a game or GL context. */
public final class MageModelChecks {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static ModelRenderer part(ModelTowerMage m, String name) throws Exception {
        Field f = ModelTowerMage.class.getDeclaredField(name); f.setAccessible(true); return (ModelRenderer)f.get(m);
    }
    public static void main(String[] args) throws Exception {
        ModelTowerMage m = new ModelTowerMage();
        check(m.textureWidth == 64 && m.textureHeight == 64, "atlas dimensions");
        Field q = ModelBox.class.getDeclaredField("quadList"); q.setAccessible(true);
        int boxes = 0;
        for (ModelRenderer part : m.boxList) for (ModelBox box : part.cubeList) {
            boxes++;
            for (TexturedQuad quad : (TexturedQuad[])q.get(box)) for (PositionTextureVertex v : quad.vertexPositions)
                check(v.texturePositionX >= 0 && v.texturePositionX <= 1 && v.texturePositionY >= 0 && v.texturePositionY <= 1, "UV out of bounds");
        }
        check(boxes >= 8, "missing parts");
        ModelRenderer head = part(m, "head"), rightArm = part(m, "rightArm"), leftArm = part(m, "leftArm"), rightLeg = part(m, "rightLeg"), leftLeg = part(m, "leftLeg");
        check(head.childModels != null && head.childModels.size() == 2, "hood must follow head");
        m.setRotationAngles(0, 0.8F, 0, 200, -200, .0625F, null);
        check(Math.abs(head.rotateAngleY - 60F * .017453292F) < .001F && Math.abs(head.rotateAngleX + 45F * .017453292F) < .001F, "head clamp");
        check(rightLeg.rotateAngleX > 0 && leftLeg.rotateAngleX < 0, "opposite gait");
        float walkArm = rightArm.rotateAngleX;
        Field uf = Unsafe.class.getDeclaredField("theUnsafe"); uf.setAccessible(true); Unsafe u = (Unsafe)uf.get(null);
        EntityTowerMage mage = (EntityTowerMage)u.allocateInstance(EntityTowerMage.class);
        World world = (World)u.allocateInstance(WorldClient.class);
        Field remote = World.class.getDeclaredField("isRemote"); remote.setAccessible(true); remote.setBoolean(world, true); mage.world = world;
        mage.handleStatusUpdate((byte)70); check(mage.getCastAnimationTicks() == 12, "client event");
        Field ticks = EntityTowerMage.class.getDeclaredField("castAnimationTicks"); ticks.setAccessible(true); ticks.setInt(mage, 6);
        m.setRotationAngles(0, 0.8F, 0, 0, 0, .0625F, mage);
        check(rightArm.rotateAngleX < -1.79F && leftArm.rotateAngleX < -1.34F, "cast pose");
        ticks.setInt(mage, 0); m.setRotationAngles(0, 0.8F, 0, 0, 0, .0625F, mage);
        check(Math.abs(rightArm.rotateAngleX - walkArm) < .001F, "cast pose resets");
        remote.setBoolean(world, false); mage.handleStatusUpdate((byte)70); check(mage.getCastAnimationTicks() == 0, "server must not set visual timer");
        System.out.println("PASS: real mapped model UVs, anatomy, hood parenting, gaze, gait, cast event, cast pose and reset");
    }
}
