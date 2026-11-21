package com.nexusrpg.client.anim;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/** Animações visuais locais, registradas exclusivamente no cliente. */
@SideOnly(Side.CLIENT)
public final class PlayerAnimClient {
    private static final int[] KEYS = {Keyboard.KEY_Z, Keyboard.KEY_X, Keyboard.KEY_C, Keyboard.KEY_V,
        Keyboard.KEY_G, Keyboard.KEY_H, Keyboard.KEY_J, Keyboard.KEY_NONE};
    private static final KeyBinding[] BINDS = new KeyBinding[KEYS.length];
    private static boolean initialized;
    private EntityPlayer lastPlayer;

    public static void init() {
        if (initialized) return;
        initialized = true;
        PlayerAnim[] all = PlayerAnim.values();
        for (int i = 0; i < BINDS.length; i++) {
            BINDS[i] = new KeyBinding("key.nexusrpg.anim." + all[i].name().toLowerCase(Locale.ROOT), KEYS[i], "key.categories.nexusrpg");
            ClientRegistry.registerKeyBinding(BINDS[i]);
        }
        MinecraftForge.EVENT_BUS.register(new PlayerAnimClient());
    }

    public static void installRenderers() {
        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        try {
            // getSkinMap() é somente leitura em 1.12.2. Acessa o mapa real com
            // nome SRG remapeado pelo Forge e não substitui renderizadores alterados.
            java.util.Map<String, RenderPlayer> skins = net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(
                RenderManager.class, rm, "field_178636_l");
            installIfVanilla(rm, skins, "default", false);
            installIfVanilla(rm, skins, "slim", true);
        } catch (RuntimeException problem) {
            org.apache.logging.log4j.LogManager.getLogger("nexusrpg").warn("Não foi possível instalar as animações de jogador; renderização original mantida", problem);
        }
    }

    private static void installIfVanilla(RenderManager rm, java.util.Map<String, RenderPlayer> skins, String type, boolean slim) {
        RenderPlayer old = skins.get(type);
        if (old == null || old.getClass() != RenderPlayer.class || old.getMainModel().getClass() != net.minecraft.client.model.ModelPlayer.class) return;
        java.util.List<?> layers = net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(
            net.minecraft.client.renderer.entity.RenderLivingBase.class, old, "field_177097_h");
        Class<?>[] expected = {
            net.minecraft.client.renderer.entity.layers.LayerBipedArmor.class,
            net.minecraft.client.renderer.entity.layers.LayerHeldItem.class,
            net.minecraft.client.renderer.entity.layers.LayerArrow.class,
            net.minecraft.client.renderer.entity.layers.LayerDeadmau5Head.class,
            net.minecraft.client.renderer.entity.layers.LayerCape.class,
            net.minecraft.client.renderer.entity.layers.LayerCustomHead.class,
            net.minecraft.client.renderer.entity.layers.LayerElytra.class,
            net.minecraft.client.renderer.entity.layers.LayerEntityOnShoulder.class
        };
        if (layers == null || layers.size() != expected.length) return;
        for (int i = 0; i < expected.length; i++) {
            if (layers.get(i) == null || layers.get(i).getClass() != expected[i]) return;
        }
        skins.put(type, new RenderPlayerAnim(rm, slim));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player != lastPlayer) {
            PlayerAnimManager.clear();
            lastPlayer = player;
        }
        if (player == null || mc.currentScreen != null || !player.isEntityAlive() || player.isSpectator()) {
            if (player != null) PlayerAnimManager.stop(player);
            for (KeyBinding bind : BINDS) while (bind.isPressed()) { /* descarta teclas antigas */ }
            return;
        }
        PlayerAnim[] all = PlayerAnim.values();
        PlayerAnimManager.get(player, player.ticksExisted);
        for (int i = 0; i < BINDS.length; i++) {
            PlayerAnim anim = all[i];
            if (anim.durationTicks <= 0) {
                PlayerAnim current = PlayerAnimManager.get(player, player.ticksExisted);
                if (BINDS[i].isKeyDown()) {
                    if (current != anim) PlayerAnimManager.play(player, anim);
                } else if (current == anim) {
                    PlayerAnimManager.stop(player);
                }
                while (BINDS[i].isPressed()) { /* não acumular eventos de guarda */ }
            } else if (BINDS[i].isPressed()) {
                while (BINDS[i].isPressed()) { /* uma ação por tick */ }
                PlayerAnimManager.play(player, anim);
            }
        }
    }
}
