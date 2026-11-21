#!/usr/bin/env python3
"""Create only new files; abort instead of overwriting anything."""
from pathlib import Path
from PIL import Image, ImageDraw
from io import BytesIO
import base64, json, random
root=Path(__file__).resolve().parent
j=root/'src/main/java/com/nexusrpg/mobs'
r=root/'src/main/resources/assets/nexusrpg'
configs=[('Stilt','reed_stilt',30,0.28,4,0.8,2.1,'feather','wheat_seeds',(81,116,91),(206,185,104)),('Prism','prism_tortoise',36,0.19,6,1.3,1.0,'quartz','gold_nugget',(75,77,100),(116,205,202))]
outputs={}
for cls,name,h,s,d,w,height,common,rare,base,accent in configs:
    outputs[j/f'Entity{cls}.java']=f'''package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
/** Hostile ground creature using standard melee AI, without secondary hit effects. */
public final class Entity{cls} extends EntityNexusMob {{
    private static final ResourceLocation LOOT = new ResourceLocation("nexusrpg", "entities/{name}");
    public Entity{cls}(World world) {{ super(world); setSize({w}F, {height}F); }}
    @Override protected double health() {{ return {h}D; }}
    @Override protected double speed() {{ return {s}D; }}
    @Override protected double damage() {{ return {d}D; }}
    @Override protected ResourceLocation getLootTable() {{ return LOOT; }}
}}
'''
    data={'pools':[{'name':name+'_common','rolls':1,'entries':[{'type':'item','name':'minecraft:'+common,'weight':1,'functions':[{'function':'minecraft:set_count','count':{'min':1,'max':2}}]}]},{'name':name+'_extra','rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':0.25}],'entries':[{'type':'item','name':'minecraft:'+rare,'weight':1}]}]}
    outputs[r/f'loot_tables/entities/{name}.json']=json.dumps(data,indent=2)+'\n'
    random.seed(name); im=Image.new('RGBA',(64,64)); pix=im.load()
    for y in range(64):
        for x in range(64):
            c=accent if ((x//4+y//4)%7==0 or (x-2*y)%31==0) else base
            t=random.randrange(-13,14); pix[x,y]=tuple(max(0,min(255,v+t)) for v in c)+(255,)
    image=BytesIO(); im.save(image,format='PNG'); outputs[r/f'textures/entity/mobs/{name}.png.b64']=base64.b64encode(image.getvalue()).decode()+'\n'
outputs[j/'client/ModelStilt.java']='''package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
/** Tall, long-necked ground bird with two wing-fins and a fan tail. */
@SideOnly(Side.CLIENT)
public final class ModelStilt extends ModelBase {
    private final ModelRenderer body, neck, head, tail;
    private final ModelRenderer[] legs = new ModelRenderer[2], wings = new ModelRenderer[2];
    public ModelStilt() {
        textureWidth=64; textureHeight=64;
        body=new ModelRenderer(this,0,0); body.addBox(-4,-4,-6,8,8,12); body.setRotationPoint(0,8,0);
        neck=new ModelRenderer(this,0,22); neck.addBox(-2,-10,-2,4,11,4); neck.setRotationPoint(0,6,-4);
        head=new ModelRenderer(this,20,22); head.addBox(-3,-4,-3,6,5,5); head.setRotationPoint(0,-4,-4);
        head.setTextureOffset(42,22).addBox(-1,-2,-8,2,2,5);
        head.setTextureOffset(42,30).addBox(-1,-7,0,2,3,2);
        tail=new ModelRenderer(this,0,42); tail.addBox(-4,-1,0,8,2,7); tail.setRotationPoint(0,6,5);
        for(int i=0;i<2;i++) { int side=i==0?-1:1;
            legs[i]=new ModelRenderer(this,32,40); legs[i].addBox(-1,0,-1,2,12,2); legs[i].setTextureOffset(40,40).addBox(-1,11,-3,2,1,4); legs[i].setRotationPoint(side*2,12,1);
            wings[i]=new ModelRenderer(this,0,52); wings[i].addBox(side<0?-5:0,0,-4,5,1,8); wings[i].setRotationPoint(side*4,6,0);
        }
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e); body.render(scale); neck.render(scale); head.render(scale); tail.render(scale); for(ModelRenderer p:legs)p.render(scale); for(ModelRenderer p:wings)p.render(scale);
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        head.rotateAngleY=MathHelper.clamp(yaw,-30,30)*0.017453292F; head.rotateAngleX=MathHelper.clamp(pitch,-20,20)*0.017453292F;
        float step=MathHelper.cos(swing*0.6662F)*0.7F*amount; legs[0].rotateAngleX=step; legs[1].rotateAngleX=-step;
        wings[0].rotateAngleZ=0.1F+MathHelper.cos(age*0.1F)*0.05F; wings[1].rotateAngleZ=-wings[0].rotateAngleZ; tail.rotateAngleY=MathHelper.cos(age*0.1F)*0.1F;
    }
}
'''
outputs[j/'client/ModelPrism.java']='''package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
/** Low four-legged creature with layered shell and three crystalline plates. */
@SideOnly(Side.CLIENT)
public final class ModelPrism extends ModelBase {
    private final ModelRenderer shell, head, tail;
    private final ModelRenderer[] legs = new ModelRenderer[4];
    public ModelPrism() {
        textureWidth=64; textureHeight=64;
        shell=new ModelRenderer(this,0,0); shell.addBox(-6,-4,-7,12,7,14); shell.setRotationPoint(0,15,0);
        shell.setTextureOffset(0,22).addBox(-5,-6,-6,10,2,12);
        shell.setTextureOffset(46,22).addBox(-1,-10,-4,2,4,3);
        shell.setTextureOffset(46,30).addBox(-4,-9,0,2,3,3);
        shell.setTextureOffset(46,30).addBox(2,-9,0,2,3,3);
        head=new ModelRenderer(this,0,38); head.addBox(-3,-2,-5,6,4,5); head.setRotationPoint(0,17,-7);
        head.setTextureOffset(24,38).addBox(-2,0,-7,4,2,2);
        tail=new ModelRenderer(this,24,46); tail.addBox(-1,-1,0,2,2,6); tail.setRotationPoint(0,17,7);
        for(int i=0;i<4;i++) { legs[i]=new ModelRenderer(this,40,46); legs[i].addBox(-2,0,-2,4,5,4); legs[i].setRotationPoint(i%2==0?-5:5,19,i<2?-4:4); }
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e); shell.render(scale); head.render(scale); tail.render(scale); for(ModelRenderer p:legs)p.render(scale);
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        head.rotateAngleY=MathHelper.clamp(yaw,-25,25)*0.017453292F; head.rotateAngleX=MathHelper.clamp(pitch,-15,15)*0.017453292F;
        for(int i=0;i<4;i++)legs[i].rotateAngleX=MathHelper.cos(swing*0.6662F+((i==0||i==3)?0F:3.14159F))*0.55F*amount;
        tail.rotateAngleY=MathHelper.cos(age*0.08F)*0.12F;
    }
}
'''
existing=[str(p) for p in outputs if p.exists()]
if existing: raise SystemExit('Refusing to overwrite: '+repr(existing))
for path,content in outputs.items():
    path.parent.mkdir(parents=True,exist_ok=True); path.write_text(content,encoding='utf-8')
print('Created',len(outputs),'new files')
