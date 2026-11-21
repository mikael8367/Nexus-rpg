import com.nexusrpg.dragons.EntityFireDragon;
import com.nexusrpg.dragons.client.DragonModel;
import net.minecraft.client.model.ModelRenderer;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

/** Exercises the real mapped model's animation logic without opening Minecraft or OpenGL. */
public final class DragonModelChecks {
    public static final class Dummy extends EntityFireDragon {
        public Dummy() { super(null); }
    }
    private static ModelRenderer part(DragonModel m, String name) throws Exception {
        Field f = DragonModel.class.getDeclaredField(name);
        f.setAccessible(true);
        return (ModelRenderer) f.get(m);
    }
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Field u = Unsafe.class.getDeclaredField("theUnsafe");
        u.setAccessible(true);
        Dummy d = (Dummy) ((Unsafe) u.get(null)).allocateInstance(Dummy.class);
        for (DragonModel.Kind kind : DragonModel.Kind.values()) {
            DragonModel m = new DragonModel(kind);
            d.onGround = true;
            m.setRotationAngles(1, 1, 3, 90, -90, 1, d);
            require(Math.abs(part(m, "head").rotateAngleY - (float)Math.PI / 4) < .001F, "yaw clamp");
            require(Math.abs(part(m, "head").rotateAngleX + (float)Math.PI / 6) < .001F, "pitch clamp");
            float idle = part(m, "wingLeft").rotateAngleZ;
            require(Math.abs(idle + part(m, "wingRight").rotateAngleZ) < .0001F, "symmetric wings");
            d.onGround = false;
            m.setRotationAngles(1, 1, 3, 0, 0, 1, d);
            require(Math.abs(part(m, "wingLeft").rotateAngleZ - idle) > .1F, "flight flaps more than idle");
            float tail = part(m, "tail").rotateAngleY;
            m.setRotationAngles(1, 1, 9, 0, 0, 1, d);
            require(Math.abs(part(m, "tail").rotateAngleY - tail) > .01F, "tail sways");
            Field f = DragonModel.class.getDeclaredField("legs");
            f.setAccessible(true);
            ModelRenderer[] legs = (ModelRenderer[]) f.get(m);
            require(Math.abs(legs[0].rotateAngleX - legs[3].rotateAngleX) < .0001F, "diagonal gait");
            require(Math.abs(legs[0].rotateAngleX + legs[1].rotateAngleX) < .0001F, "opposed gait");
            m.setRotationAngles(1, 0, 9, 0, 0, 1, d);
            for (ModelRenderer leg : legs) require(leg.rotateAngleX == 0, "idle legs");
        }
        System.out.println("PASS: model anatomy construction and animation checks for all kinds");
    }
}
