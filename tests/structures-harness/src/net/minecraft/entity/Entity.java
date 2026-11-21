package net.minecraft.entity;
import net.minecraft.util.math.AxisAlignedBB;
public class Entity {
 public boolean isDead, collidable;
 private final AxisAlignedBB box;
 public Entity(AxisAlignedBB box, boolean collidable) {this.box=box; this.collidable=collidable;}
 public boolean canBeCollidedWith() {return collidable;}
 public AxisAlignedBB getEntityBoundingBox() {return box;}
}
