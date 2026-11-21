package com.nexusrpg.character;

import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-only readout. Only attribute buttons send requests to the existing server channel. */
@SideOnly(Side.CLIENT)
final class CharacterRpgScreen extends GuiScreen {
    private static final int TAB_COUNT = 7, TAB_ID = 100, STAT_ID = 200;
    private static final EntityEquipmentSlot[] SLOTS = { EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
        EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET, EntityEquipmentSlot.MAINHAND, EntityEquipmentSlot.OFFHAND };
    private int tab;
    private int left, panelWidth, tabsTop;

    private boolean portuguese() { return mc.gameSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("pt"); }
    private String text(String pt, String en) { return portuguese() ? pt : en; }
    private String[] tabNames() {
        return portuguese() ? new String[]{"Personagem", "Atributos", "Habilidades", "Equipamentos", "Missões", "Mapa", "Conquistas"}
            : new String[]{"Character", "Attributes", "Skills", "Equipment", "Quests", "Map", "Achievements"};
    }
    private String tr(String key, Object... args) { return I18n.format("character.nexusrpg." + key, args); }

    @Override public void initGui() {
        buttonList.clear();
        panelWidth = Math.min(520, width - 12);
        left = (width - panelWidth) / 2;
        tabsTop = height - 45;
        String[] names = tabNames();
        for (int i = 0; i < TAB_COUNT; i++) {
            int x1 = left + (i % 4) * panelWidth / 4;
            int x2 = left + (i % 4 + 1) * panelWidth / 4;
            GuiButton b = new GuiButton(TAB_ID + i, x1, tabsTop + (i / 4) * 22, x2 - x1 - 2, 20, names[i]);
            b.enabled = i != tab;
            buttonList.add(b);
        }
        if (tab == 1) {
            for (int i = 0; i < CharacterProfile.STATS.length; i++)
                buttonList.add(new GuiButton(STAT_ID + i, left + panelWidth - 29, 43 + i * 20, 25, 20, "+"));
        }
        updateStatsButtons();
    }
    private void updateStatsButtons() {
        if (mc.player == null) return;
        for (GuiButton b : buttonList) {
            int stat = b.id - STAT_ID;
            if (stat >= 0 && stat < CharacterProfile.STATS.length) {
                int allocated = CharacterProfile.root(mc.player).getInteger("stat_" + CharacterProfile.STATS[stat]);
                b.enabled = CharacterProfile.points(mc.player) > 0 && allocated >= 0 && allocated < 10000;
            }
        }
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        int index = button.id - TAB_ID;
        if (index >= 0 && index < TAB_COUNT) { tab = index; initGui(); return; }
        index = button.id - STAT_ID;
        if (tab == 1 && index >= 0 && index < CharacterProfile.STATS.length && button.enabled)
            CharacterNetwork.send(1, index);
    }
    @Override public void updateScreen() { updateStatsButtons(); }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        EntityPlayer p = mc.player;
        if (p == null) return;
        drawCenteredString(fontRenderer, tabNames()[tab], width / 2, 10, 0xFFFFFF);
        if (tab == 0) drawCharacter(p);
        else if (tab == 1) drawAttributes(p);
        else if (tab == 2) drawSkills();
        else if (tab == 3) drawEquipment(p);
        else drawCenteredString(fontRenderer, text("Ainda não disponível", "Not yet available"), width / 2, 60, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (tab == 3) equipmentTooltip(p, mouseX, mouseY);
    }
    private void drawCharacter(EntityPlayer p) {
        int y = 35, x = left + 8;
        String cls = CharacterProfile.characterClass(p);
        drawString(fontRenderer, text("Classe: ", "Class: ") + tr("class." + cls), x, y, 0xFFFFFF); y += 20;
        drawString(fontRenderer, tr("level", CharacterProfile.level(p)), x, y, 0xFFFFFF); y += 20;
        drawString(fontRenderer, tr("xp", CharacterProfile.xp(p), CharacterProfile.xpForNextLevel(p)), x, y, 0xFFFFFF); y += 20;
        drawString(fontRenderer, tr("points", CharacterProfile.points(p)), x, y, 0xFFFFFF); y += 20;
        drawString(fontRenderer, tr("mana_display", CharacterProfile.mana(p), CharacterProfile.maxMana(p)), x, y, 0x66CCFF); y += 20;
        drawString(fontRenderer, text("Vida: ", "Health: ") + String.format(java.util.Locale.ROOT, "%.1f / %.1f", p.getHealth(), p.getMaxHealth()), x, y, 0xFFAAAA);
    }
    private void drawAttributes(EntityPlayer p) {
        drawString(fontRenderer, tr("points", CharacterProfile.points(p)), left + 8, 27, 0xFFFFAA);
        for (int i = 0; i < CharacterProfile.STATS.length; i++) {
            String stat = CharacterProfile.STATS[i];
            drawString(fontRenderer, tr("stat." + stat) + ": " + CharacterProfile.stat(p, stat), left + 8, 49 + i * 20, 0xFFFFFF);
        }
    }
    private void drawSkills() {
        int x = left + 8, y = 30;
        String[] lines = portuguese() ? new String[]{
            "Use o chat normal (sem /):",
            "!skill info — mostra classe, nível, aperfeiçoamento e pontos.",
            "!skill up — gasta ponto de habilidade (máximo 5).",
            "!skill cast — ativa; mire em um monstro hostil, quando necessário.",
            "!skill ultimate — suprema a partir do nível 15.",
            "1 ponto de habilidade a cada 3 níveis, a partir do nível 4."
        } : new String[]{
            "Use normal chat (without /):",
            "!skill info — shows class, level, rank and points.",
            "!skill up — spends a skill point (maximum rank 5).",
            "!skill cast — active skill; aim at a hostile mob when required.",
            "!skill ultimate — ultimate, from level 15.",
            "1 skill point every 3 levels, starting at level 4."
        };
        for (String line : lines) {
            List<String> wrapped = fontRenderer.listFormattedStringToWidth(line, panelWidth - 16);
            for (String part : wrapped) {
                if (y + fontRenderer.FONT_HEIGHT >= tabsTop) return;
                drawString(fontRenderer, part, x, y, 0xFFFFFF); y += 10;
            }
            y += 4;
        }
    }
    private String slotName(int i) {
        String[] pt = {"Cabeça", "Peito", "Pernas", "Pés", "Mão principal", "Mão secundária"};
        String[] en = {"Head", "Chest", "Legs", "Feet", "Main hand", "Off hand"};
        return portuguese() ? pt[i] : en[i];
    }
    private void drawEquipment(EntityPlayer p) {
        drawString(fontRenderer, text("Somente leitura", "Read only"), left + 8, 26, 0xAAAAAA);
        for (int i = 0; i < SLOTS.length; i++) {
            int y = 41 + i * 22;
            ItemStack stack = p.getItemStackFromSlot(SLOTS[i]);
            if (!stack.isEmpty()) {
                GlStateManager.enableRescaleNormal();
                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(stack, left + 8, y);
                itemRender.renderItemOverlayIntoGUI(fontRenderer, stack, left + 8, y, null);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
            }
            String description = slotName(i) + ": " + (stack.isEmpty() ? text("Vazio", "Empty") : stack.getDisplayName());
            drawString(fontRenderer, fontRenderer.trimStringToWidth(description, panelWidth - 42), left + 31, y + 5, 0xFFFFFF);
        }
    }
    private void equipmentTooltip(EntityPlayer p, int x, int y) {
        for (int i = 0; i < SLOTS.length; i++) {
            int rowY = 41 + i * 22;
            ItemStack stack = p.getItemStackFromSlot(SLOTS[i]);
            if (!stack.isEmpty() && x >= left + 8 && x < left + panelWidth - 4 && y >= rowY && y < rowY + 18) {
                renderToolTip(stack, x, y); return;
            }
        }
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
