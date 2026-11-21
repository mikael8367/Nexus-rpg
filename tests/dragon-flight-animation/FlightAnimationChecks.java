import com.nexusrpg.dragons.client.DragonModel;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

public final class FlightAnimationChecks {
    public static final class Dummy extends net.minecraft.entity.EntityLiving {
        boolean water, riding, passenger;
        public Dummy() { super(null); }
        @Override public boolean isInWater() { return water; }
        @Override public boolean isRiding() { return riding; }
        @Override public boolean isBeingRidden() { return passenger; }
    }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static ModelRenderer part(DragonModel model, String name) throws Exception {
        Field f = DragonModel.class.getDeclaredField(name); f.setAccessible(true);
        return (ModelRenderer)f.get(model);
    }
    static ModelRenderer[] legs(DragonModel model) throws Exception {
        Field f = DragonModel.class.getDeclaredField("legs"); f.setAccessible(true);
        return (ModelRenderer[])f.get(model);
    }
    static void animate(DragonModel m, Entity d, float swing, float amount, float age, float yaw, float pitch) {
        m.setRotationAngles(swing, amount, age, yaw, pitch, 1, d);
    }
    static void finitePose(DragonModel m) throws Exception {
        for (String n : new String[]{"body", "head", "tail", "tailTip", "wingLeft", "wingRight"}) {
            ModelRenderer p = part(m, n);
            check(Float.isFinite(p.rotateAngleX) && Float.isFinite(p.rotateAngleY) && Float.isFinite(p.rotateAngleZ), "non-finite " + n);
        }
        for (ModelRenderer p : legs(m)) check(Float.isFinite(p.rotateAngleX), "non-finite leg");
    }
    public static void main(String[] args) throws Exception {
        Field u = Unsafe.class.getDeclaredField("theUnsafe"); u.setAccessible(true);
        Dummy d = (Dummy)((Unsafe)u.get(null)).allocateInstance(Dummy.class);
        for (DragonModel.Kind kind : DragonModel.Kind.values()) {
            DragonModel m = new DragonModel(kind);
            d.onGround = true; d.water = false; d.riding = false;
            animate(m, d, 1, 1, 3, 90, -90);
            check(Math.abs(part(m, "head").rotateAngleY - (float)Math.PI/4) < 0.001F, "yaw clamp");
            check(Math.abs(part(m, "head").rotateAngleX + (float)Math.PI/6) < 0.001F, "pitch clamp");
            ModelRenderer[] l = legs(m);
            check(Math.abs(l[0].rotateAngleX-l[3].rotateAngleX) < 1e-5F, "diagonal gait");
            check(Math.abs(l[0].rotateAngleX+l[1].rotateAngleX) < 1e-5F, "opposed gait");
            float idle = part(m, "wingLeft").rotateAngleZ;
            d.onGround = false;
            animate(m, d, 1, 1, 3.49F, 0, 0);
            float up = part(m, "wingLeft").rotateAngleZ;
            check(up > idle + 0.4F, "upstroke");
            check(Math.abs(up + part(m, "wingRight").rotateAngleZ) < 1e-6F, "mirrored wings");
            check(part(m, "body").rotateAngleX < -0.08F, "air body pose");
            float front = l[0].rotateAngleX, back = l[2].rotateAngleX;
            check(front < -0.4F && back > 0.35F, "tucked legs");
            animate(m, d, 100, 0, 3.49F, 0, 0);
            check(l[0].rotateAngleX == front && l[2].rotateAngleX == back, "air legs independent of walking");
            animate(m, d, 1, 1, 10.47F, 0, 0);
            check(part(m, "wingLeft").rotateAngleZ < -0.5F, "downstroke");
            d.passenger = true;
            animate(m, d, 0, 0, 3.49F, 0, 0);
            check(part(m, "wingLeft").rotateAngleZ > 0.6F, "carrying passenger can fly");
            d.water = true;
            animate(m, d, 0, 0, 3.49F, 0, 0);
            check(part(m, "body").rotateAngleX == 0 && Math.abs(part(m, "wingLeft").rotateAngleZ) < .21F, "water is not flight");
            d.water = false; d.riding = true;
            animate(m, d, 0, 0, 3.49F, 0, 0);
            check(part(m, "body").rotateAngleX == 0 && Math.abs(part(m, "wingLeft").rotateAngleZ) < .21F, "riding is not flight");
            d.riding = false; d.passenger = false;
            animate(m, d, 0, 0, 3.49F, 0, 0);
            d.onGround = true;
            animate(m, d, 1, 0, 3.49F, 0, 0);
            check(part(m, "body").rotateAngleX == 0 && part(m, "tail").rotateAngleX == 0 && part(m, "tailTip").rotateAngleX == 0, "landing resets pitch");
            for (ModelRenderer leg : l) check(leg.rotateAngleX == 0, "landing resets legs");
            animate(m, null, 0, 0, 3.49F, 0, 0);
            check(part(m, "body").rotateAngleX == 0, "null entity safe");
            for (float bad : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.MAX_VALUE, -Float.MAX_VALUE}) {
                d.onGround = false;
                animate(m, d, bad, bad, bad, bad, bad); finitePose(m);
                d.onGround = true;
                animate(m, d, bad, bad, bad, bad, bad); finitePose(m);
            }
        }
        System.out.println("PASS: four dragon models, ground/flight, mounts/water, switching and invalid inputs");
    }
}
