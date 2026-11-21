package com.nexusrpg.character;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import com.nexusrpg.skills.SkillTree;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Mouse;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
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
    private static final int CAST_ID = 500, ULTIMATE_ID = 501;
    private static final int MAP_UP_ID = 600, MAP_DOWN_ID = 601, ADVANCEMENTS_ID = 602;
    private static final int PAGE_UP_ID = 603, PAGE_DOWN_ID = 604;
    private int skillSequence, pendingTicks, pendingRetries, pendingNode, refreshTicks, detailScroll, statOffset, statRows, mapScroll;
    private int characterOffset, equipmentOffset;
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
            int buttonWidth = Math.max(1, x2 - x1 - 2);
            GuiButton b = new GuiButton(TAB_ID + i, x1, tabsTop + (i / 4) * 22, buttonWidth, 20,
                fontRenderer.trimStringToWidth(names[i], Math.max(1, buttonWidth - 8)));
            b.enabled = i != tab;
            buttonList.add(b);
        }
        if (tab == 0) characterOffset = Math.max(0, Math.min(characterOffset, 6 - contentRows()));
        if (tab == 3) equipmentOffset = Math.max(0, Math.min(equipmentOffset, SLOTS.length - contentRows()));
        // Chat commands still work on short screens where these controls would overlap text.
        if (tab == 0 && height >= 212) {
            int half = Math.max(30, (panelWidth - 18) / 2);
            buttonList.add(new GuiButton(CAST_ID, left + 6, tabsTop - 22, half, 20, text("Ativa", "Active")));
            buttonList.add(new GuiButton(ULTIMATE_ID, left + 10 + half, tabsTop - 22, half, 20, text("Suprema", "Ultimate")));
        }
        if ((tab == 0 && height < 212) || (tab == 3 && height < 218)) {
            int rows = contentRows();
            int offset = tab == 0 ? characterOffset : equipmentOffset;
            int total = tab == 0 ? 6 : SLOTS.length;
            offset = Math.max(0, Math.min(offset, Math.max(0, total - rows)));
            if (tab == 0) characterOffset = offset; else equipmentOffset = offset;
            if (rows > 0) {
                GuiButton prev = new GuiButton(PAGE_UP_ID, left + 6, tabsTop - 22, 40, 20, "^");
                GuiButton next = new GuiButton(PAGE_DOWN_ID, left + panelWidth - 46, tabsTop - 22, 40, 20, "v");
                prev.enabled = offset > 0; next.enabled = offset + rows < total;
                buttonList.add(prev); buttonList.add(next);
            }
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
        if (tab == 5 && tabsTop >= 58) {
            buttonList.add(new GuiButton(MAP_UP_ID, left + 6, tabsTop - 22, 40, 20, "^"));
            buttonList.add(new GuiButton(MAP_DOWN_ID, left + panelWidth - 46, tabsTop - 22, 40, 20, "v"));
        }
        if (tab == 6 && tabsTop >= 75) {
            GuiButton advancements = new GuiButton(ADVANCEMENTS_ID, left + 6, 47, Math.max(1, panelWidth - 12), 20,
                fontRenderer.trimStringToWidth(text("Abrir progressos do Minecraft", "Open Minecraft advancements"), Math.max(1, panelWidth - 20)));
            advancements.enabled = mc.getConnection() != null;
            buttonList.add(advancements);
        }
        if (!requested && mc.player != null) { requested = true; requestSkill(-1); }
        updateStatsButtons();
        updateSkillButtons();
        updateCastButtons();
    }
    private int contentRows() {
        if (tab == 0) return height >= 212 ? 6 : Math.max(0, (tabsTop - 22 - 35) / 20);
        return height >= 218 ? SLOTS.length : Math.max(0, (tabsTop - 22 - 41) / 22);
    }
    private void updateCastButtons() {
        for (GuiButton b : buttonList)
            if (b.id == CAST_ID || b.id == ULTIMATE_ID)
                b.enabled = mc.player != null && mc.getConnection() != null
                    && mc.player.isEntityAlive() && !mc.player.isSpectator();
    }
    private void updateStatsButtons() {
        if (mc.player == null) return;
        for (GuiButton b : buttonList) {
            int stat = b.id - STAT_ID;
            if (stat >= 0 && stat < CharacterProfile.STATS.length) {
                int allocated = CharacterProfile.root(mc.player).getInteger("stat_" + CharacterProfile.STATS[stat]);
                b.displayString = CharacterClient.allocationStalled() ? "?" : "+";
                b.enabled = !CharacterClient.allocationPending() && mc.getConnection() != null
                    && (CharacterClient.allocationStalled() || (mc.player.isEntityAlive() && !mc.player.isSpectator()
                    && CharacterProfile.points(mc.player) > 0 && allocated >= 0 && allocated < 10000));
            }
        }
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (tab == 0 && button.enabled && (button.id == CAST_ID || button.id == ULTIMATE_ID)) {
            CharacterNetwork.send(button.id == CAST_ID ? 3 : 4, 0);
            // The server reports activation, target and cooldown in chat.
            mc.displayGuiScreen(null);
            return;
        }
        if (tab == 6 && button.id == ADVANCEMENTS_ID && button.enabled && mc.getConnection() != null) {
            mc.displayGuiScreen(new GuiScreenAdvancements(mc.getConnection().getAdvancementManager()));
            return;
        }
        if (tab == 5 && button.enabled && (button.id == MAP_UP_ID || button.id == MAP_DOWN_ID)) {
            mapScroll = Math.max(0, mapScroll + (button.id == MAP_UP_ID ? -1 : 1));
            return;
        }
        if ((tab == 0 || tab == 3) && button.enabled && (button.id == PAGE_UP_ID || button.id == PAGE_DOWN_ID)) {
            int delta = button.id == PAGE_UP_ID ? -1 : 1;
            if (tab == 0) characterOffset += delta; else equipmentOffset += delta;
            initGui(); return;
        }
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
        if (tab == 1 && index >= 0 && index < CharacterProfile.STATS.length && button.enabled) {
            CharacterClient.allocate(index);
            updateStatsButtons();
        }
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
        if (pending && ++pendingTicks > 40) {
            pendingTicks=0;
            if (++pendingRetries <= 3 && mc.player!=null && mc.getConnection()!=null) {
                // Same token, never a fresh purchase: safe if the first reply was lost.
                CharacterNetwork.requestSkills(skillSession,skillSequence,pendingNode);
            } else {
                skillMessage = pendingBuy ? text("Resposta não recebida; compra pode ter ocorrido. Atualizando.", "No response; purchase may have succeeded. Refreshing.")
                    : text("Sem resposta do servidor; tentando novamente.", "No server response; retrying.");
                pending = false; skillData = null;
                // Higher sequence is a read-only barrier against delayed older requests.
                if (tab == 2) requestSkill(-1);
            }
        } else if (tab == 2 && !pending && ++refreshTicks >= 40) requestSkill(-1);
        updateSkillButtons();
        updateCastButtons();
        for (GuiButton b : buttonList) if (b.id == ADVANCEMENTS_ID) b.enabled = mc.getConnection() != null;
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
        else if (tab == 5) drawMap(p);
        else if (tab == 6) drawAdvancements();
        else drawCenteredString(fontRenderer, text("Ainda não disponível", "Not yet available"), width / 2, 60, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (tab == 3) equipmentTooltip(p, mouseX, mouseY);
    }
    // This is a live location readout, not a rendered map or a chunk search.
    private void drawMap(EntityPlayer p) {
        List<String> lines = new ArrayList<>();
        if (mc.world == null || p.world != mc.world) {
            lines.add(text("Mundo indisponível.", "World unavailable."));
        } else {
            BlockPos pos = p.getPosition();
            lines.add(text("Posição em blocos:", "Position in blocks:"));
            lines.add("X: " + pos.getX() + "   Y: " + pos.getY() + "   Z: " + pos.getZ());
            lines.add(text("Dimensão: ", "Dimension: ") + mc.world.provider.getDimensionType().getName()
                + " (ID " + mc.world.provider.getDimension() + ")");
            String biomeName = text("indisponível", "unavailable");
            // Read the received chunk biome byte directly: getBiome() can invent a
            // plains fallback when the byte is 255, and even cache that fallback.
            if (mc.world.isBlockLoaded(pos, false)) {
                byte[] biomes = mc.world.getChunkFromBlockCoords(pos).getBiomeArray();
                int index = ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
                if (biomes != null && index < biomes.length) {
                    int id = biomes[index] & 255;
                    Biome biome = id == 255 ? null : Biome.getBiome(id);
                    if (biome != null) biomeName = biome.getBiomeName();
                }
            }
            lines.add(text("Bioma: ", "Biome: ") + biomeName);
            lines.add(text("Dados locais; sem mapa de terreno.", "Local data; no terrain map."));
        }
        List<String> wrapped = new ArrayList<>();
        for (String line : lines) wrapped.addAll(fontRenderer.listFormattedStringToWidth(line, Math.max(1, panelWidth - 16)));
        int bottom = tabsTop >= 58 ? tabsTop - 22 : tabsTop;
        int rows = Math.max(0, (bottom - 26) / 10);
        mapScroll = Math.max(0, Math.min(mapScroll, Math.max(0, wrapped.size() - rows)));
        for (int i = 0; i < rows && mapScroll + i < wrapped.size(); i++)
            drawString(fontRenderer, wrapped.get(mapScroll + i), left + 8, 26 + i * 10, 0xFFFFFF);
        for (GuiButton b : buttonList) {
            if (b.id == MAP_UP_ID) b.enabled = mapScroll > 0;
            else if (b.id == MAP_DOWN_ID) b.enabled = rows > 0 && mapScroll + rows < wrapped.size();
        }
    }
    private void drawAdvancements() {
        String label = mc.getConnection() == null ? text("Sem conexão com o servidor.", "No server connection.")
            : text("Progressos existentes do Minecraft", "Existing Minecraft advancements");
        drawString(fontRenderer, fontRenderer.trimStringToWidth(label, Math.max(1, panelWidth - 16)), left + 8, 27, 0xFFFFFF);
        if (tabsTop >= 90) drawString(fontRenderer, fontRenderer.trimStringToWidth(
            text("A tela padrão fecha este menu.", "The standard screen closes this menu."), Math.max(1, panelWidth - 16)),
            left + 8, 73, 0xAAAAAA);
    }
    private void drawCharacter(EntityPlayer p) {
        int x = left + 8;
        String cls = CharacterProfile.characterClass(p);
        String[] lines = { text("Classe: ", "Class: ") + tr("class." + cls),
            tr("level", CharacterProfile.level(p)), tr("xp", CharacterProfile.xp(p), CharacterProfile.xpForNextLevel(p)),
            tr("points", CharacterProfile.points(p)), tr("mana_display", CharacterProfile.mana(p), CharacterProfile.maxMana(p)),
            text("Vida: ", "Health: ") + String.format(java.util.Locale.ROOT, "%.1f / %.1f", p.getHealth(), p.getMaxHealth()) };
        for (int i = characterOffset; i < Math.min(lines.length, characterOffset + contentRows()); i++) {
            int y = 35 + (i - characterOffset) * 20;
            int color = i == 4 ? 0x66CCFF : i == 5 ? 0xFFAAAA : 0xFFFFFF;
            drawString(fontRenderer, fontRenderer.trimStringToWidth(lines[i], Math.max(1, panelWidth - 16)), x, y, color);
            if (i == 2) {
                int barWidth = Math.max(0, panelWidth - 16);
                int needed = CharacterProfile.xpForNextLevel(p);
                int filled = needed <= 0 ? 0 : (int)(barWidth * Math.min(1.0D, (double)CharacterProfile.xp(p) / needed));
                drawRect(x, y + 11, x + barWidth, y + 15, 0xFF333333);
                drawRect(x, y + 11, x + filled, y + 15, 0xFF55CC66);
            }
        }
    }
    private void drawAttributes(EntityPlayer p) {
        drawString(fontRenderer, tr("points", CharacterProfile.points(p)), left + 8, 27, 0xFFFFAA);
        if (!CharacterClient.allocationStatus().isEmpty())
            drawString(fontRenderer, fontRenderer.trimStringToWidth(CharacterClient.allocationStatus(), Math.max(1,panelWidth-16)), left+8, tabsTop-11, 0xFFCC88);
        for (int i = statOffset; i < Math.min(CharacterProfile.STATS.length, statOffset + statRows); i++) {
            String stat = CharacterProfile.STATS[i];
            drawString(fontRenderer, fontRenderer.trimStringToWidth(tr("stat." + stat) + ": " + CharacterProfile.stat(p, stat), panelWidth - 45), left + 8, 49 + (i-statOffset) * 20, 0xFFFFFF);
        }
    }
    private void requestSkill(int nodeIndex) {
        if (mc.player == null || mc.getConnection() == null) return;
        skillOwner = mc.player;
        pending = true; pendingBuy = nodeIndex >= 0; pendingTicks = 0; pendingRetries = 0; pendingNode = nodeIndex; refreshTicks = 0;
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
        if (tab==5 && wheel!=0) mapScroll=Math.max(0,mapScroll+(wheel<0?1:-1));
    }
    @Override protected void keyTyped(char typed,int key) throws IOException {
        if (tab==2 && key==org.lwjgl.input.Keyboard.KEY_DOWN) { detailScroll++; return; }
        if (tab==2 && key==org.lwjgl.input.Keyboard.KEY_UP) { detailScroll=Math.max(0,detailScroll-1); return; }
        if (tab==5 && key==org.lwjgl.input.Keyboard.KEY_DOWN) { mapScroll++; return; }
        if (tab==5 && key==org.lwjgl.input.Keyboard.KEY_UP) { mapScroll=Math.max(0,mapScroll-1); return; }
        super.keyTyped(typed,key);
    }
    private String slotName(int i) {
        String[] pt = {"Cabeça", "Peito", "Pernas", "Pés", "Mão principal", "Mão secundária"};
        String[] en = {"Head", "Chest", "Legs", "Feet", "Main hand", "Off hand"};
        return portuguese() ? pt[i] : en[i];
    }
    private void drawEquipment(EntityPlayer p) {
        drawString(fontRenderer, text("Somente leitura", "Read only"), left + 8, 26, 0xAAAAAA);
        for (int i = equipmentOffset; i < Math.min(SLOTS.length, equipmentOffset + contentRows()); i++) {
            int y = 41 + (i - equipmentOffset) * 22;
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
        for (int i = equipmentOffset; i < Math.min(SLOTS.length, equipmentOffset + contentRows()); i++) {
            int rowY = 41 + (i - equipmentOffset) * 22;
            ItemStack stack = p.getItemStackFromSlot(SLOTS[i]);
            if (!stack.isEmpty() && x >= left + 8 && x < left + panelWidth - 4 && y >= rowY && y < rowY + 18) {
                renderToolTip(stack, x, y); return;
            }
        }
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
