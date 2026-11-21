package com.nexusrpg.compass;

import com.nexusrpg.init.ModItems;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid="nexusrpg", value=Side.CLIENT)
public final class CompassClient {
    private static int seq, selected=-1, status=-1, x, z;
    private static World resultWorld;
    private CompassClient() {}
    public static void open() { Minecraft.getMinecraft().displayGuiScreen(new Screen()); }
    public static void receive(CompassNetwork.Reply m) {
        Minecraft mc=Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (m.seq != seq || m.id != selected || mc.world != resultWorld) return;
            status=m.status; x=m.x; z=m.z;
        });
    }
    private static String name(int id) { return I18n.format("compass.nexusrpg.biome."+CompassNetwork.IDS[id]); }
    private static String statusText() {
        switch(status) {
            case -1: return I18n.format("compass.nexusrpg.choose");
            case -2: return I18n.format("compass.nexusrpg.searching");
            case 0: return I18n.format("compass.nexusrpg.confirmed");
            case 1: return I18n.format("compass.nexusrpg.predicted");
            case 2: return I18n.format("compass.nexusrpg.notfound");
            case 3: return I18n.format("compass.nexusrpg.cooldown");
            case 4: return I18n.format("compass.nexusrpg.busy");
            case 5: return I18n.format("compass.nexusrpg.dimension");
            case 6: return I18n.format("compass.nexusrpg.cancelled");
            default: return "";
        }
    }
    private static String coords(EntityPlayer p) {
        return I18n.format("compass.nexusrpg.coords", x, z, (int)Math.round(Math.hypot(x-p.posX,z-p.posZ)));
    }
    private static boolean hasResult(Minecraft mc) { return (status==0 || status==1) && mc.world==resultWorld && mc.player!=null && mc.player.dimension==0; }
    private static void arrow(int x0,int y0, EntityPlayer player) {
        double dx=x-player.posX, dz=z-player.posZ;
        float angle=(float)Math.toDegrees(Math.atan2(-dx,dz))-player.rotationYaw;
        GlStateManager.pushMatrix(); GlStateManager.translate(x0,y0,0); GlStateManager.rotate(angle,0,0,1);
        Gui.drawRect(-1,-9,2,8,0xFFFFE770);
        Gui.drawRect(-3,-7,4,-4,0xFFFFE770);
        Gui.drawRect(-2,-10,3,-7,0xFFFFE770);
        GlStateManager.popMatrix();
    }
    @SubscribeEvent public static void render(RenderGameOverlayEvent.Post e) {
        if (e.getType()!=RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc=Minecraft.getMinecraft(); EntityPlayer p=mc.player;
        if (!hasResult(mc) || mc.currentScreen!=null || (p.getHeldItemMainhand().getItem()!=ModItems.BIOME_COMPASS && p.getHeldItemOffhand().getItem()!=ModItems.BIOME_COMPASS)) return;
        int left=8, top=8;
        Gui.drawRect(left,top,left+190,top+43,0xA0000000);
        arrow(left+13,top+21,p);
        mc.fontRenderer.drawStringWithShadow(name(selected),left+27,top+4,0xFFFFFF);
        mc.fontRenderer.drawStringWithShadow(coords(p),left+27,top+16,0xFFFFFF);
        mc.fontRenderer.drawStringWithShadow(statusText(),left+27,top+28,0xFFFF99);
    }
    private static final class Screen extends GuiScreen {
        @Override public void initGui() {
            if (resultWorld != mc.world) { status=-1; selected=-1; resultWorld=mc.world; }
            buttonList.clear();
            for(int i=0;i<CompassNetwork.IDS.length;i++) buttonList.add(new GuiButton(i,width/2-154+(i%2)*157,height/2-65+(i/2)*22,154,20,name(i)));
        }
        @Override protected void actionPerformed(GuiButton button) throws IOException {
            // Retain the outstanding request until its reply arrives.
            if (status == -2 || button.id < 0 || button.id >= CompassNetwork.IDS.length) return;
            selected=button.id; status=-2; resultWorld=mc.world; seq++;
            CompassNetwork.CHANNEL.sendToServer(new CompassNetwork.Request(selected,seq));
        }
        @Override public void drawScreen(int mx,int my,float partial) {
            drawDefaultBackground();
            drawCenteredString(fontRenderer,I18n.format("compass.nexusrpg.title"),width/2,height/2-88,0xFFFFFF);
            for (GuiButton button : buttonList) button.enabled = status != -2;
            super.drawScreen(mx,my,partial);
            drawCenteredString(fontRenderer,statusText(),width/2,height/2+45,0xFFFF99);
            if (hasResult(mc)) {
                drawCenteredString(fontRenderer,coords(mc.player),width/2,height/2+57,0xFFFFFF);
                arrow(width/2,height/2+80,mc.player);
            }
            drawCenteredString(fontRenderer,I18n.format("compass.nexusrpg.hint"),width/2,height/2+98,0xCCCCCC);
        }
        @Override public boolean doesGuiPauseGame() { return false; }
    }
}
