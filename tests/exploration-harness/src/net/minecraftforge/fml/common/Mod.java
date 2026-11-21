package net.minecraftforge.fml.common;
import java.lang.annotation.*;
public @interface Mod {
 @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
 @interface EventBusSubscriber { String modid(); }
}
