package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetOption;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetSwitchAction;
import com.wintercogs.beyonddimensions.client.gui.widget.scroller.BigScroller;
import com.wintercogs.beyonddimensions.client.init.BDShortKeys;
import com.wintercogs.beyonddimensions.common.menu.PrimaryNetSwitcherMenu;
import com.wintercogs.beyonddimensions.network.packet.c2s.PrimaryNetSwitchActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class PrimaryNetSwitcherGUI extends BDBaseGUI<PrimaryNetSwitcherMenu>
{
    private static final ResourceLocation BACKGROUND = BeyondDimensions.makeId("textures/gui/primary_net_switcher_gui.png");
    private static final int BACKGROUND_WIDTH = 176;
    private static final int BACKGROUND_HEIGHT = 239;

    private static final int RECENT_BUTTON_COUNT = 3;
    private static final int VISIBLE_OPTION_COUNT = 6;
    private static final int OPTION_BUTTON_WIDTH = 140;
    private static final int OPTION_BUTTON_HEIGHT = 20;
    private static final LinkedList<Integer> RECENT_PRIMARY_NET_IDS = new LinkedList<>();

    private final List<PrimaryNetOptionButton> optionButtons = new ArrayList<>();
    private final List<RecentNetButton> recentButtons = new ArrayList<>();

    private EditBox searchField;
    private BigScroller scroller;
    private Button clearPrimaryButton;
    private List<PrimaryNetOption> filteredOptions = List.of();
    private List<PrimaryNetOption> lastObservedOptions = List.of();
    private int topIndex;
    private int selectedIndex;
    private int lastObservedPrimaryNetId = DimensionsNet.NO_PRIMARY_NET_ID;

    public PrimaryNetSwitcherGUI(PrimaryNetSwitcherMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init()
    {
        super.init();
        clearWidgets();
        optionButtons.clear();
        recentButtons.clear();

        this.imageWidth = BACKGROUND_WIDTH;
        this.imageHeight = rebuildImageHeight();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.titleLabelX = 8;
        this.titleLabelY = 8;

        searchField = new EditBox(getFont(), this.leftPos + 8, this.topPos + 20, 160, this.getFont().lineHeight + 6,
                Component.translatable("menu.label.beyonddimensions.primary_net_switcher.search"));
        searchField.setMaxLength(100);
        searchField.setBordered(true);
        searchField.setVisible(true);
        searchField.setTextColor(16777215);
        searchField.setTooltip(Tooltip.create(Component.translatable("tooltip.editbox.beyonddimensions.primary_net_switcher.search")));
        searchField.setResponder(this::onSearchTextChanged);
        searchField.setSuggestion(Component.translatable("menu.label.beyonddimensions.primary_net_switcher.search").getString());
        addRenderableWidget(searchField);

        clearPrimaryButton = Button.builder(
                        Component.translatable("menu.button.beyonddimensions.primary_net_switcher.clear"),
                        button -> PacketDistributor.sendToServer(new PrimaryNetSwitchActionPacket(PrimaryNetSwitchAction.CLEAR_PRIMARY, DimensionsNet.NO_PRIMARY_NET_ID))
                )
                .pos(this.leftPos + 8, this.topPos + 47)
                .size(160, 20)
                .tooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.primary_net_switcher.clear")))
                .build();
        addRenderableWidget(clearPrimaryButton);

        int recentButtonY = this.topPos + 78;
        for (int i = 0; i < RECENT_BUTTON_COUNT; i++)
        {
            RecentNetButton recentButton = new RecentNetButton(this.leftPos + 8 + i * 54, recentButtonY, 50, 16);
            recentButton.visible = false;
            recentButton.active = false;
            recentButtons.add(recentButton);
            addRenderableWidget(recentButton);
        }

        int optionStartY = this.topPos + 107;
        for (int i = 0; i < VISIBLE_OPTION_COUNT; i++)
        {
            PrimaryNetOptionButton optionButton = new PrimaryNetOptionButton(this.leftPos + 8, optionStartY + i * 20, OPTION_BUTTON_WIDTH, OPTION_BUTTON_HEIGHT);
            optionButtons.add(optionButton);
            addRenderableWidget(optionButton);
        }

        scroller = new BigScroller(this.leftPos + 160, optionStartY + 2, VISIBLE_OPTION_COUNT * OPTION_BUTTON_HEIGHT - 17, 0, 0, pos -> {
            if (topIndex != pos)
            {
                topIndex = pos;
                syncOptionButtons();
            }
        });
        scroller.setStep(1);
        addRenderableWidget(scroller);

        lastObservedPrimaryNetId = menu.currentPrimaryNetId;
        lastObservedOptions = menu.options;
        rebuildFilteredOptions();
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (menu.options != lastObservedOptions)
        {
            lastObservedOptions = menu.options;
            rebuildFilteredOptions();
        }

        if (menu.currentPrimaryNetId != lastObservedPrimaryNetId)
        {
            lastObservedPrimaryNetId = menu.currentPrimaryNetId;
            rememberRecentNet(menu.currentPrimaryNetId);
            syncRecentButtons();
            syncOptionButtons();
        }
        else
        {
            syncRecentButtons();
            syncOptionButtons();
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT,
                0, 0,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("menu.text.beyonddimensions.primary_net_switcher.current", describeCurrentPrimary()), 8, 37, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("menu.label.beyonddimensions.primary_net_switcher.recent"), 8, 69, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("menu.label.beyonddimensions.primary_net_switcher.all_networks"), 8, 96, 4210752, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        boolean result = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (!result)
        {
            result = scroller.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        boolean inSearchField = searchField.isMouseOver(mouseX, mouseY);
        if (!inSearchField && this.getFocused() == searchField)
        {
            searchField.setFocused(false);
            this.setFocused(null);
        }
        else if (inSearchField && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
        {
            searchField.setValue("");
            return true;
        }
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);

        if (searchField != null && searchField.canConsumeInput() && key.getValue() != GLFW.GLFW_KEY_ESCAPE)
        {
            searchField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        if (this.minecraft.options.keyInventory.isActiveAndMatches(key) || BDShortKeys.OPEN_PRIMARY_NET_SWITCHER_KEY.getKey() == key)
        {
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers)
    {
        if (searchField != null && searchField.canConsumeInput())
        {
            return searchField.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void rebuildFilteredOptions()
    {
        String searchText = searchField == null ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
        List<PrimaryNetOption> nextFilteredOptions = new ArrayList<>();
        for (PrimaryNetOption option : menu.options)
        {
            String searchableText = buildSearchableText(option);
            if (searchText.isEmpty() || searchableText.contains(searchText))
            {
                nextFilteredOptions.add(option);
            }
        }

        filteredOptions = nextFilteredOptions;
        if (filteredOptions.isEmpty())
        {
            selectedIndex = -1;
            topIndex = 0;
        }
        else
        {
            selectedIndex = Math.max(0, Math.min(selectedIndex, filteredOptions.size() - 1));
            topIndex = Math.max(0, Math.min(topIndex, Math.max(0, filteredOptions.size() - VISIBLE_OPTION_COUNT)));
            ensureSelectionVisible();
        }

        syncRecentButtons();
        syncOptionButtons();
    }

    private void onSearchTextChanged(String text)
    {
        if (searchField != null)
        {
            searchField.setSuggestion(text.isEmpty()
                    ? Component.translatable("menu.label.beyonddimensions.primary_net_switcher.search").getString()
                    : "");
        }
        rebuildFilteredOptions();
    }

    private String buildSearchableText(PrimaryNetOption option)
    {
        return ("#" + option.netId() + " " + buildPermissionLabel(option.permission()).getString()).toLowerCase(Locale.ROOT);
    }

    private Component buildPermissionLabel(NetPermissionlevel permission)
    {
        return Component.translatable("menu.text.beyonddimensions.primary_net_switcher.permission." + permission.name().toLowerCase(Locale.ROOT));
    }

    private void syncRecentButtons()
    {
        List<Integer> recentNetIds = new ArrayList<>();
        for (int recentNetId : RECENT_PRIMARY_NET_IDS)
        {
            if (menu.options.stream().anyMatch(option -> option.netId() == recentNetId))
            {
                recentNetIds.add(recentNetId);
            }
            if (recentNetIds.size() >= RECENT_BUTTON_COUNT)
            {
                break;
            }
        }

        for (int i = 0; i < recentButtons.size(); i++)
        {
            RecentNetButton recentButton = recentButtons.get(i);
            if (i < recentNetIds.size())
            {
                recentButton.visible = true;
                recentButton.active = true;
                recentButton.load(recentNetIds.get(i));
            }
            else
            {
                recentButton.visible = false;
                recentButton.active = false;
            }
        }
    }

    private void syncOptionButtons()
    {
        int maxTopIndex = Math.max(0, filteredOptions.size() - VISIBLE_OPTION_COUNT);
        topIndex = Math.max(0, Math.min(topIndex, maxTopIndex));
        scroller.updateScrollPosition(topIndex, maxTopIndex);

        for (int i = 0; i < optionButtons.size(); i++)
        {
            int optionIndex = topIndex + i;
            PrimaryNetOptionButton optionButton = optionButtons.get(i);
            if (optionIndex < filteredOptions.size())
            {
                PrimaryNetOption option = filteredOptions.get(optionIndex);
                optionButton.load(option, optionIndex == selectedIndex, option.netId() == menu.currentPrimaryNetId);
            }
            else
            {
                optionButton.clear();
            }
        }
    }

    private void ensureSelectionVisible()
    {
        if (selectedIndex < 0)
        {
            return;
        }
        if (selectedIndex < topIndex)
        {
            topIndex = selectedIndex;
        }
        else if (selectedIndex >= topIndex + VISIBLE_OPTION_COUNT)
        {
            topIndex = selectedIndex - VISIBLE_OPTION_COUNT + 1;
        }
    }

    private void sendSetPrimary(int netId)
    {
        PacketDistributor.sendToServer(new PrimaryNetSwitchActionPacket(PrimaryNetSwitchAction.SET_EXPLICIT, netId));
    }

    private void selectNetAndSend(int netId)
    {
        for (int i = 0; i < filteredOptions.size(); i++)
        {
            if (filteredOptions.get(i).netId() == netId)
            {
                selectedIndex = i;
                ensureSelectionVisible();
                syncOptionButtons();
                break;
            }
        }
        sendSetPrimary(netId);
    }

    private Component describeCurrentPrimary()
    {
        return menu.currentPrimaryNetId == DimensionsNet.NO_PRIMARY_NET_ID
                ? Component.translatable("menu.text.beyonddimensions.primary_net_switcher.none")
                : Component.literal("#" + menu.currentPrimaryNetId);
    }

    private static void rememberRecentNet(int netId)
    {
        if (netId < 0)
        {
            return;
        }

        RECENT_PRIMARY_NET_IDS.removeIf(existingNetId -> existingNetId == netId);
        RECENT_PRIMARY_NET_IDS.addFirst(netId);
        while (RECENT_PRIMARY_NET_IDS.size() > 8)
        {
            RECENT_PRIMARY_NET_IDS.removeLast();
        }
    }

    private int rebuildImageHeight()
    {
        return BACKGROUND_HEIGHT;
    }

    private final class PrimaryNetOptionButton extends Button
    {
        private PrimaryNetOption option;
        private int optionIndex = -1;

        private PrimaryNetOptionButton(int x, int y, int width, int height)
        {
            super(x, y, width, height, Component.empty(), button -> {
                if (((PrimaryNetOptionButton) button).option != null)
                {
                    ((PrimaryNetOptionButton) button).onSelected();
                }
            }, DEFAULT_NARRATION);
            this.visible = false;
            this.active = false;
        }

        private void load(PrimaryNetOption option, boolean selected, boolean currentPrimary)
        {
            this.option = option;
            this.optionIndex = PrimaryNetSwitcherGUI.this.topIndex + optionButtons.indexOf(this);
            this.visible = true;
            this.active = !currentPrimary;
            this.setMessage(buildLabel(option, currentPrimary));
            this.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.primary_net_switcher.option", option.netId(), buildPermissionLabel(option.permission()))));
        }

        private void clear()
        {
            this.option = null;
            this.optionIndex = -1;
            this.visible = false;
            this.active = false;
            this.setMessage(Component.empty());
            this.setTooltip(null);
        }

        private void onSelected()
        {
            if (option == null)
            {
                return;
            }
            if (optionIndex >= 0)
            {
                selectedIndex = optionIndex;
            }
            selectNetAndSend(option.netId());
        }

        private Component buildLabel(PrimaryNetOption option, boolean currentPrimary)
        {
            return Component.literal("#" + option.netId() + " ").append(PrimaryNetSwitcherGUI.this.buildPermissionLabel(option.permission())).append(currentPrimary ? Component.translatable("menu.text.beyonddimensions.primary_net_switcher.current_suffix") : Component.empty());
        }
    }

    private final class RecentNetButton extends Button
    {
        private int targetNetId = DimensionsNet.NO_PRIMARY_NET_ID;

        private RecentNetButton(int x, int y, int width, int height)
        {
            super(x, y, width, height, Component.empty(), button -> {
                int netId = ((RecentNetButton) button).targetNetId;
                if (netId >= 0)
                {
                    sendSetPrimary(netId);
                }
            }, DEFAULT_NARRATION);
        }

        private void load(int netId)
        {
            this.targetNetId = netId;
            this.setMessage(Component.literal("#" + netId));
            this.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.primary_net_switcher.recent", netId)));
        }

        @Override
        public void onPress()
        {
            if (targetNetId >= 0)
            {
                selectNetAndSend(targetNetId);
            }
        }
    }
}
