package com.nexusrpg.character;

import com.nexusrpg.NexusRpg;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid=NexusRpg.MODID,value=Side.CLIENT)
public final class CharacterClient {
    private static KeyBinding key; private static EntityPlayer player; private static boolean received, prompt;
    private CharacterClient() {}
    static void init() { key=new KeyBinding("key.nexusrpg.character",Keyboard.KEY_K,"key.categories.nexusrpg"); ClientRegistry.registerKeyBinding(key); }
    static void receive(NBTTagCompound data) {
        if(data==null) return;
        Minecraft mc=Minecraft.getMinecraft(); mc.addScheduledTask(() -> {
            if(mc.player==null) return;
            CharacterProfile.root(mc.player); CharacterProfile.replace(mc.player,data); received=true;
            if(CharacterProfile.characterClass(mc.player).isEmpty()) prompt=true;
            else { prompt=false; if(mc.currentScreen instanceof SelectScreen) mc.displayGuiScreen(new CharacterRpgScreen()); }
        });
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.player!=player) { player=mc.player; received=false; prompt=false; if(player!=null) CharacterNetwork.send(2,0); }
        if(player==null||!received) return;
        if(prompt && mc.currentScreen==null) { prompt=false; mc.displayGuiScreen(new SelectScreen()); }
        if(key!=null && key.isPressed() && mc.currentScreen==null)
            mc.displayGuiScreen(CharacterProfile.characterClass(player).isEmpty()?new SelectScreen():new CharacterRpgScreen());
    }
    private static String tr(String suffix,Object... args) { return I18n.format("character.nexusrpg."+suffix,args); }
    private static class SelectScreen extends GuiScreen {
        @Override public void initGui() {
            buttonList.clear(); for(int i=0;i<CharacterProfile.CLASSES.length;i++) buttonList.add(new GuiButton(i,width/2-100,height/2-56+i*22,200,20,tr("class."+CharacterProfile.CLASSES[i])));
        }
        @Override protected void actionPerformed(GuiButton b) throws IOException { if(b.id>=0&&b.id<6) CharacterNetwork.send(0,b.id); }
        @Override public void drawScreen(int x,int y,float partial) {
            drawDefaultBackground(); drawCenteredString(fontRenderer,tr("choose"),width/2,height/2-83,0xFFFFFF);
            super.drawScreen(x,y,partial); drawCenteredString(fontRenderer,tr("choose_hint"),width/2,height/2+80,0xCCCCCC);
            for(GuiButton b:buttonList) if(b.isMouseOver()) { drawHoveringText(tr("bonus."+CharacterProfile.CLASSES[b.id]),x,y); break; }
        }
        @Override public boolean doesGuiPauseGame() { return false; }
    }
}
