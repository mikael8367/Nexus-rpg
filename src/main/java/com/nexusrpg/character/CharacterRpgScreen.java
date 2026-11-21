package com.nexusrpg.character;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import com.nexusrpg.skills.SkillTree;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Mouse;
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

/** Client-only character interface. Skill decisions remain server-authoritative. */
@SideOnly(Side.CLIENT)
final class CharacterRpgScreen extends GuiScreen {
    private static final int TAB_COUNT = 7, TAB_ID = 100, STAT_ID = 200;
    private static final EntityEquipmentSlot[] SLOTS = { EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
        EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET, EntityEquipmentSlot.MAINHAND, EntityEquipmentSlot.OFFHAND };
    private int tab;
    private int left, panelWidth, tabsTop;
    private static int nextSession;
    private final int skillSession = ++nextSession;
    private static final int BUY_ID = 300, REFRESH_ID = 301, UP_ID = 302, DOWN_ID = 303;
    private static final int PREV_SKILL_ID = 304, NEXT_SKILL_ID = 305, PREV_STATS_ID = 400, NEXT_STATS_ID = 401;
    private int skillSequence, pendingTicks, refreshTicks, detailScroll, statOffset, statRows;
    private boolean compactSkills() { return height < 210; }
    private boolean requested, pending, pendingBuy;
    private NBTTagCompound skillData;
    private EntityPlayer skillOwner;
    private String selectedSkill, skillMessage = "";

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
            boolean pages = 43 + CharacterProfile.STATS.length * 20 > tabsTop;
            statRows = pages ? Math.max(1, (tabsTop - 22 - 43) / 20) : CharacterProfile.STATS.length;
            statOffset = Math.max(0, Math.min(statOffset, Math.max(0, CharacterProfile.STATS.length - statRows)));
            for (int i = statOffset; i < Math.min(CharacterProfile.STATS.length, statOffset + statRows); i++)
                buttonList.add(new GuiButton(STAT_ID + i, left + panelWidth - 29, 43 + (i-statOffset) * 20, 25, 20, "+"));
            if (pages) {
                GuiButton prev = new GuiButton(PREV_STATS_ID, left + 6, tabsTop - 22, 40, 20, "<");
                GuiButton next = new GuiButton(NEXT_STATS_ID, left + panelWidth - 46, tabsTop - 22, 40, 20, ">");
                prev.enabled = statOffset > 0; next.enabled = statOffset + statRows < CharacterProfile.STATS.length;
                buttonList.add(prev); buttonList.add(next);
            }
        }
        if (tab == 2) {
            int half = Math.max(30, (panelWidth - 18) / 2);
            buttonList.add(new GuiButton(BUY_ID, left + 6, tabsTop - 22, half, 20, text("Comprar grau", "Buy rank")));
            buttonList.add(new GuiButton(REFRESH_ID, left + 10 + half, tabsTop - 22, half, 20, text("Atualizar", "Refresh")));
            if (compactSkills()) {
                buttonList.add(new GuiButton(PREV_SKILL_ID, left + 6, 25, 24, 20, "<"));
                buttonList.add(new GuiButton(NEXT_SKILL_ID, left + panelWidth - 30, 25, 24, 20, ">"));
                if (tabsTop - 22 >= 75) {
                    buttonList.add(new GuiButton(UP_ID, left + panelWidth - 20, 49, 16, 12, "^"));
                    buttonList.add(new GuiButton(DOWN_ID, left + panelWidth - 20, 62, 16, 12, "v"));
                }
            } else if (tabsTop - 22 >= 141) {
                buttonList.add(new GuiButton(UP_ID, left + panelWidth - 20, 103, 16, 18, "^"));
                buttonList.add(new GuiButton(DOWN_ID, left + panelWidth - 20, 123, 16, 18, "v"));
            }
        }
        if (!requested && mc.player != null) { requested = true; requestSkill(-1); }
        updateStatsButtons();
        updateSkillButtons();
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
        if (index >= 0 && index < TAB_COUNT) {
            tab = index;
            if (tab == 2 && !pending) requestSkill(-1);
            initGui(); return;
        }
        if (tab == 1 && button.enabled && (button.id == PREV_STATS_ID || button.id == NEXT_STATS_ID)) {
            statOffset += button.id == PREV_STATS_ID ? -statRows : statRows;
            initGui(); return;
        }
        if (tab == 2 && button.enabled && (button.id == PREV_SKILL_ID || button.id == NEXT_SKILL_ID)) {
            List<SkillTree.Node> nodes = SkillTree.nodes(skillData.getString("class"));
            if (!nodes.isEmpty()) {
                int current = nodes.indexOf(SkillTree.node(selectedSkill));
                int delta = button.id == PREV_SKILL_ID ? -1 : 1;
                selectedSkill = nodes.get((current + delta + nodes.size()) % nodes.size()).id;
                detailScroll = 0; updateSkillButtons();
            }
            return;
        }
        index = button.id - STAT_ID;
        if (tab == 1 && index >= 0 && index < CharacterProfile.STATS.length && button.enabled)
            CharacterNetwork.send(1, index);
        else if (tab == 2 && button.enabled && button.id == BUY_ID) {
            SkillTree.Node n = SkillTree.node(selectedSkill);
            if (n != null) requestSkill(SkillTree.allNodes().indexOf(n));
        } else if (tab == 2 && button.enabled && button.id == REFRESH_ID) requestSkill(-1);
        else if (tab == 2 && button.enabled && button.id == UP_ID) detailScroll = Math.max(0, detailScroll - 1);
        else if (tab == 2 && button.enabled && button.id == DOWN_ID) detailScroll++;
    }
    @Override public void updateScreen() {
        updateStatsButtons();
        if (skillOwner != null && skillOwner != mc.player) {
            skillOwner = mc.player; skillData = null; pending = false; pendingBuy = false;
            selectedSkill = null; skillSequence++; refreshTicks = 40;
        }
        if (pending && ++pendingTicks > 100) {
            skillMessage = pendingBuy ? text("Resposta não recebida; compra pode ter ocorrido. Atualizando.", "No response; purchase may have succeeded. Refreshing.")
                : text("Sem resposta do servidor; tentando novamente.", "No server response; retrying.");
            pending = false; skillData = null;
            if (tab == 2) requestSkill(-1);
        } else if (tab == 2 && !pending && ++refreshTicks >= 40) requestSkill(-1);
        updateSkillButtons();
    }
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
        for (int i = statOffset; i < Math.min(CharacterProfile.STATS.length, statOffset + statRows); i++) {
            String stat = CharacterProfile.STATS[i];
            drawString(fontRenderer, fontRenderer.trimStringToWidth(tr("stat." + stat) + ": " + CharacterProfile.stat(p, stat), panelWidth - 45), left + 8, 49 + (i-statOffset) * 20, 0xFFFFFF);
        }
    }
    private void requestSkill(int nodeIndex) {
        if (mc.player == null || mc.getConnection() == null) return;
        skillOwner = mc.player;
        pending = true; pendingBuy = nodeIndex >= 0; pendingTicks = 0; refreshTicks = 0;
        if (pendingBuy) skillMessage = "";
        CharacterNetwork.requestSkills(skillSession, ++skillSequence, nodeIndex);
        updateSkillButtons();
    }
    void receiveSkills(int session, int sequence, NBTTagCompound data) {
        if (session != skillSession || sequence != skillSequence || !pending || data == null || skillOwner != mc.player) return;
        boolean wasBuy = pendingBuy;
        pending = false; pendingBuy = false; refreshTicks = 0;
        skillData = data.copy();
        if (!wasBuy && skillMessage.equals(text("Sem resposta do servidor; tentando novamente.", "No server response; retrying."))) skillMessage = "";
        if (wasBuy) {
            String result = data.getString("result");
            skillMessage = result.isEmpty() ? text("Grau comprado.", "Rank purchased.") : result;
            detailScroll = 0;
        }
        if (!data.getBoolean("ok") || SkillTree.nodes(data.getString("class")).isEmpty()) selectedSkill = null;
        else {
            SkillTree.Node selected = SkillTree.node(selectedSkill);
            if (selected == null || !selected.characterClass.equals(data.getString("class"))) {
                selectedSkill = SkillTree.nodes(data.getString("class")).get(0).id;
                detailScroll = 0;
            }
        }
        updateSkillButtons();
    }
    private int skillRank(SkillTree.Node n) {
        return skillData == null ? 0 : skillData.getCompoundTag("ranks").getInteger(n.id);
    }
    private boolean canBuy(SkillTree.Node n) {
        if (pending || skillData == null || !skillData.getBoolean("ok") || n == null || mc.player == null
            || !mc.player.isEntityAlive() || mc.player.isSpectator() || !n.characterClass.equals(skillData.getString("class"))
            || skillRank(n) >= n.maxRank || skillData.getInteger("level") < n.minLevel
            || skillData.getInteger("points") < n.cost) return false;
        for (SkillTree.Requirement r : n.requirements) {
            SkillTree.Node dependency = SkillTree.node(r.nodeId);
            if (dependency == null || skillRank(dependency) < r.rank) return false;
        }
        return true;
    }
    private void updateSkillButtons() {
        for (GuiButton b : buttonList) {
            if (b.id == BUY_ID) b.enabled = canBuy(SkillTree.node(selectedSkill));
            else if (b.id == REFRESH_ID) b.enabled = !pending && mc.player != null && mc.getConnection() != null;
            else if (b.id == UP_ID || b.id == DOWN_ID || b.id == PREV_SKILL_ID || b.id == NEXT_SKILL_ID)
                b.enabled = skillData != null && skillData.getBoolean("ok") && !SkillTree.nodes(skillData.getString("class")).isEmpty();
        }
    }
    private int nodeX(SkillTree.Node n) { return width / 2 + n.x * Math.max(24, Math.min(65, (panelWidth - 46) / 2)); }
    private int nodeY(SkillTree.Node n) { return 43 + n.y * 22; }
    private String nodeLabel(SkillTree.Node n) {
        if ("training".equals(n.kind)) return text("Treino", "Train");
        if ("passive".equals(n.kind)) return text("Passiva", "Passive");
        if ("active".equals(n.kind)) return text("Ativa", "Active");
        return text("Suprema", "Ultimate");
    }
    private void drawSkills() {
        if (skillData == null || !skillData.getBoolean("ok")) {
            String message = skillData == null ? (skillMessage.isEmpty() ? text("Carregando habilidades...", "Loading skills...") : skillMessage) : skillData.getString("reason");
            List<String> lines = fontRenderer.listFormattedStringToWidth(message, Math.max(1, panelWidth - 16));
            int y = compactSkills() ? 49 : 35;
            for (String line : lines) { if (y + 10 >= tabsTop - 22) break; drawString(fontRenderer,line,left+8,y,0xFFCCAA); y+=10; }
            return;
        }
        if (compactSkills()) {
            SkillTree.Node selected = SkillTree.node(selectedSkill);
            if (selected != null) {
                String label = nodeLabel(selected) + " " + skillRank(selected) + "/" + selected.maxRank;
                drawCenteredString(fontRenderer, fontRenderer.trimStringToWidth(label, Math.max(1, panelWidth - 65)), width / 2, 31, 0xFFFFAA);
            }
            List<String> details = skillDetails();
            int top = 49, maxLines = Math.max(0, (tabsTop - 22 - top) / 10);
            detailScroll = Math.max(0, Math.min(detailScroll, Math.max(0, details.size() - maxLines)));
            for (int i = 0; i < maxLines && detailScroll + i < details.size(); i++)
                drawString(fontRenderer, details.get(detailScroll + i), left + 8, top + i * 10, 0xFFFFFF);
            return;
        }
        String header = tr("class." + skillData.getString("class")) + " | " + text("Nível ", "Level ") + skillData.getInteger("level") + text(" | Pontos: ", " | Points: ") + skillData.getInteger("points");
        drawCenteredString(fontRenderer, fontRenderer.trimStringToWidth(header,panelWidth-10), width/2, 24, 0xFFFFAA);
        List<SkillTree.Node> nodes = SkillTree.nodes(skillData.getString("class"));
        for (SkillTree.Node n : nodes) for (SkillTree.Requirement r : n.requirements) {
            SkillTree.Node dep = SkillTree.node(r.nodeId);
            if (dep == null || !dep.characterClass.equals(n.characterClass)) continue;
            int x1=nodeX(dep), y1=nodeY(dep)+10, x2=nodeX(n), y2=nodeY(n)-9, mid=(y1+y2)/2;
            int color=skillRank(dep)>=r.rank ? 0xFF55AA77 : 0xFF777777;
            drawRect(x1, y1, x1+1, mid+1, color);
            drawRect(Math.min(x1,x2),mid,Math.max(x1,x2)+1,mid+1,color);
            drawRect(x2,mid,x2+1,y2+1,color);
        }
        for (SkillTree.Node n : nodes) {
            int x=nodeX(n), y=nodeY(n), rank=skillRank(n);
            int border=n.id.equals(selectedSkill) ? 0xFFFFFF55 : rank>0 ? 0xFF55BB77 : 0xFF777777;
            drawRect(x-23,y-9,x+24,y+11,border); drawRect(x-22,y-8,x+23,y+10,0xFF222233);
            drawCenteredString(fontRenderer, fontRenderer.trimStringToWidth(nodeLabel(n),43),x,y-8,0xFFFFFF);
            drawCenteredString(fontRenderer, rank+"/"+n.maxRank,x,y+1,0xAADDFF);
        }
        List<String> lines=skillDetails();
        int top=103, bottom=tabsTop-25, maxLines=Math.max(0,(bottom-top)/10);
        detailScroll=Math.max(0,Math.min(detailScroll,Math.max(0,lines.size()-maxLines)));
        for (int i=0; i<maxLines && detailScroll+i<lines.size(); i++)
            drawString(fontRenderer,lines.get(detailScroll+i),left+8,top+i*10,0xFFFFFF);
        if (lines.size()>maxLines && maxLines>0)
            drawString(fontRenderer, detailScroll+maxLines<lines.size()?"v":"^",left+panelWidth-9,bottom-10,0xFFFF55);
    }
    private void detailLine(List<String> out,String line) {
        out.addAll(fontRenderer.listFormattedStringToWidth(line,Math.max(1,panelWidth-40)));
    }
    private List<String> skillDetails() {
        List<String> lines=new ArrayList<>();
        if (!skillMessage.isEmpty()) detailLine(lines,skillMessage);
        SkillTree.Node n=SkillTree.node(selectedSkill);
        if (n==null) return lines;
        if (compactSkills()) detailLine(lines, text("Nível ", "Level ") + skillData.getInteger("level") + text(" | Pontos: ", " | Points: ") + skillData.getInteger("points"));
        detailLine(lines,n.name + " (" + skillRank(n) + "/" + n.maxRank + ")");
        detailLine(lines,n.description);
        detailLine(lines,text("Custo: ","Cost: ")+n.cost+text(" | Nível mínimo: "," | Min. level: ")+n.minLevel);
        if (n.requirements.isEmpty()) detailLine(lines,text("Sem pré-requisitos.","No prerequisites."));
        for (SkillTree.Requirement r:n.requirements) {
            SkillTree.Node dep=SkillTree.node(r.nodeId);
            if (dep!=null) detailLine(lines,text("Exige: ","Requires: ")+dep.name+" "+skillRank(dep)+"/"+r.rank);
        }
        detailLine(lines,"Chat: !skill info, !skill tree, !skill up, !skill up <id>");
        detailLine(lines,"Chat: !skill cast, !skill ultimate");
        detailLine(lines,text("Role detalhes com roda ou setas.","Scroll details with wheel or arrows."));
        return lines;
    }
    @Override protected void mouseClicked(int x,int y,int button) throws IOException {
        super.mouseClicked(x,y,button);
        if (button!=0 || tab!=2 || compactSkills() || skillData==null || !skillData.getBoolean("ok")) return;
        for (SkillTree.Node n:SkillTree.nodes(skillData.getString("class"))) {
            if (x>=nodeX(n)-23 && x<nodeX(n)+24 && y>=nodeY(n)-9 && y<nodeY(n)+10) {
                selectedSkill=n.id; detailScroll=0; updateSkillButtons(); return;
            }
        }
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel=Mouse.getEventDWheel();
        if (tab==2 && wheel!=0) detailScroll=Math.max(0,detailScroll+(wheel<0?1:-1));
    }
    @Override protected void keyTyped(char typed,int key) throws IOException {
        if (tab==2 && key==org.lwjgl.input.Keyboard.KEY_DOWN) { detailScroll++; return; }
        if (tab==2 && key==org.lwjgl.input.Keyboard.KEY_UP) { detailScroll=Math.max(0,detailScroll-1); return; }
        super.keyTyped(typed,key);
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
