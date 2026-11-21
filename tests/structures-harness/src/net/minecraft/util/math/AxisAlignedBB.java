package net.minecraft.util.math;
public class AxisAlignedBB {
 public final double minX,minY,minZ,maxX,maxY,maxZ;
 public AxisAlignedBB(double x1,double y1,double z1,double x2,double y2,double z2) {minX=x1;minY=y1;minZ=z1;maxX=x2;maxY=y2;maxZ=z2;}
 public boolean intersects(AxisAlignedBB b) {return maxX>b.minX && minX<b.maxX && maxY>b.minY && minY<b.maxY && maxZ>b.minZ && minZ<b.maxZ;}
}
