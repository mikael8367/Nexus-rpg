package com.nexusrpg.energy;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Drawn in code so the interface does not depend on a texture asset. */
@SideOnly(Side.CLIENT)
public final class MachineScreen extends GuiContainer {
    private final MachineContainer machine;
    MachineScreen(MachineContainer container) {
        super(container);
        machine = container;
        xSize = 176;
        ySize = container.inventoryTop + 82;
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xffc6c6c6);
        drawRect(guiLeft + 6, guiTop + 30, guiLeft + 170, guiTop + 38, 0xff555555);
        int width = Math.max(0, Math.min(162, (int) (162L * machine.energy() / 100000)));
        drawRect(guiLeft + 7, guiTop + 31, guiLeft + 7 + width, guiTop + 37, 0xffdf503d);
        for (Slot slot : machine.inventorySlots) {
            int x = guiLeft + slot.xPos, y = guiTop + slot.yPos;
            drawRect(x - 1, y - 1, x + 17, y + 17, 0xff777777);
            drawRect(x, y, x + 16, y + 16, 0xff8b8b8b);
        }
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = machine.tile instanceof TileDigitalMiner ? "Minerador digital" : machine.tile instanceof TileCoalGenerator ? "Gerador a carvao" : "Painel solar";
        fontRenderer.drawString(title, 8, 6, 0x303030);
        fontRenderer.drawString("Energia: " + machine.energy() + " / 100000 FE", 8, 18, 0x303030);
        if (machine.tile instanceof TileCoalGenerator) fontRenderer.drawString("Queima: " + machine.extra() + " ticks", 8, 41, 0x303030);
        else if (machine.tile instanceof TileSolarPanel) fontRenderer.drawString(machine.extra() == 1 ? "Estrutura completa" : "Estrutura incompleta", 8, 48, 0x303030);
        fontRenderer.drawString("Inventario", 8, machine.inventoryTop - 11, 0x303030);
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }
}
