import com.nexusrpg.ent.client.ModelEntAnciao;
import com.nexusrpg.ent.IEntAnciaoAnimated;
import net.minecraft.client.model.ModelRenderer;
import java.lang.reflect.*;

public class EntModelAnimationCheck {
  static Field field(String name) throws Exception { Field f=ModelEntAnciao.class.getDeclaredField(name); f.setAccessible(true); return f; }
  static void pose(ModelEntAnciao m,int state,float tick, boolean rage) throws Exception {
    field("animState").setInt(m,state); field("animTime").setFloat(m,tick/20F); m.enraged=rage;
    m.setRotationAngles(2.3F,0.4F,120F,15F,8F,0.0625F,null);
  }
  static float[] read(ModelEntAnciao m) throws Exception {
    String[] names={"root","torso","head","armR","armL","legR","legL","crown","antR","antL","tineAR","tineBR","tineAL","tineBL"};
    float[] r=new float[names.length*6+18]; int i=0;
    for(String name:names){ ModelRenderer b=(ModelRenderer)field(name).get(m); r[i++]=b.rotateAngleX; r[i++]=b.rotateAngleY; r[i++]=b.rotateAngleZ; r[i++]=b.rotationPointX; r[i++]=b.rotationPointY; r[i++]=b.rotationPointZ; }
    for(ModelRenderer b:(ModelRenderer[])field("vines").get(m)){r[i++]=b.rotateAngleX;r[i++]=b.rotateAngleY;r[i++]=b.rotateAngleZ;}
    return r;
  }
  static void close(float a,float b,float eps,String what) { if(Math.abs(a-b)>eps || !Float.isFinite(a) || !Float.isFinite(b)) throw new AssertionError(what+": "+a+" / "+b); }
  static void same(float[] a,float[] b,float eps,String what){ for(int i=0;i<a.length;i++) close(a[i],b[i],eps,what+"["+i+"]"); }
  public static void main(String[] args) throws Exception {
    ModelEntAnciao m=new ModelEntAnciao();
    for(boolean rage:new boolean[]{false,true}){
      pose(m,0,0,rage); float[] idle=read(m);
      for(int state=1;state<=3;state++){
        int end=state==1?60:state==2?80:52;
        pose(m,state,0,rage); same(idle,read(m),0.0001F,"start "+state);
        pose(m,state,end,rage); float[] finish=read(m);
        if(state==2) finish[1]+=(float)(6*Math.PI); // Y rotation is visually the same at 3 revolutions
        same(idle,finish,0.0001F,"end "+state);
        for(float t=0;t<=end;t+=0.25F){
          pose(m,state,t,rage); float[] a=read(m); pose(m,state,t,rage); same(a,read(m),0F,"repeat");
          for(float v:a) if(!Float.isFinite(v) || Math.abs(v)>30F) throw new AssertionError("invalid pose");
          if(t<end){ pose(m,state,t+0.01F,rage); float[] b=read(m); for(int i=0;i<a.length;i++) if(Math.abs(a[i]-b[i])>0.04F) throw new AssertionError("jump "+state+" "+t+" index "+i); }
        }
      }
      pose(m,3,18,rage); float wind=((ModelRenderer)field("armR").get(m)).rotateAngleX;
      pose(m,3,22,rage); float hit=((ModelRenderer)field("armR").get(m)).rotateAngleX;
      if(!(wind < -2F && hit > -1.7F && hit > wind)) throw new AssertionError("slam phases");
      pose(m,4,16,rage); close(m.glow,0F,0.0001F,"death glow");
      pose(m,0,0,rage); same(idle,read(m),0.0001F,"entity state reset");
    }
    System.out.println("Ent model poses: endpoints, 1/4-tick samples, continuity, repeat, slam and reset OK");
  }
}
