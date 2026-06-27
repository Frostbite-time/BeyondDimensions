package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetOption;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetSwitchAction;
import com.wintercogs.beyonddimensions.client.gui.widget.scroller.BigScroller;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.IconButton;
import com.wintercogs.beyonddimensions.client.init.BDShortKeys;
import com.wintercogs.beyonddimensions.common.menu.PrimaryNetSwitcherMenu;
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.PrimaryNetSwitchActionPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.RenameNetPacket;
import com.wintercogs.beyonddimensions.util.UIDataHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class PrimaryNetSwitcherGUI extends BDBaseGUI<PrimaryNetSwitcherMenu> {
    private static final Identifier BACKGROUND = BeyondDimensions.makeId("textures/gui/primary_net_switcher_gui.png");
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
    private @Nullable EditBox renameField;
    private BigScroller scroller;
    private IconButton dimensionsNetButton;
    private Button clearPrimaryButton;
    private List<PrimaryNetOption> filteredOptions = List.of();
    private List<PrimaryNetOption> lastObservedOptions = List.of();
    private int topIndex;
    private int selectedIndex = -1;
    private int renamingNetId = DimensionsNet.NO_PRIMARY_NET_ID;
    private int lastObservedPrimaryNetId = DimensionsNet.NO_PRIMARY_NET_ID;

    public PrimaryNetSwitcherGUI(PrimaryNetSwitcherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        optionButtons.clear();
        recentButtons.clear();
        renameField = null;
        renamingNetId = DimensionsNet.NO_PRIMARY_NET_ID;

        if (UIDataHelper.isTransfer) {
            restoreMousePosition();
            UIDataHelper.isTransfer = false;
        }

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
        searchField.setTextColor(0xFFFFFFFF);
        searchField.setTooltip(Tooltip.create(Component.translatable("tooltip.editbox.beyonddimensions.primary_net_switcher.search")));
        searchField.setResponder(this::onSearchTextChanged);
        searchField.setSuggestion(Component.translatable("menu.label.beyonddimensions.primary_net_switcher.search").getString());
        addRenderableWidget(searchField);

        dimensionsNetButton = new IconButton(this.leftPos + 152, this.topPos + 4, 16, 16, BeyondDimensions.makeId("widget/opposite_arrow"), button ->
        {
            if (menu.currentPrimaryNetId == DimensionsNet.NO_PRIMARY_NET_ID)
                return;

            saveMousePosition();
            ClientPacketDistributor.sendToServer(new OpenNetGuiPacket(menu.player.getStringUUID(), NetMenuType.NET_CRAFT_MENU));
        });
        dimensionsNetButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.open_dimensions_net_menu")));
        dimensionsNetButton.active = menu.currentPrimaryNetId != DimensionsNet.NO_PRIMARY_NET_ID;
        addRenderableWidget(dimensionsNetButton);

        clearPrimaryButton = Button.builder(
                        Component.translatable("menu.button.beyonddimensions.primary_net_switcher.clear"),
                        button -> ClientPacketDistributor.sendToServer(new PrimaryNetSwitchActionPacket(PrimaryNetSwitchAction.CLEAR_PRIMARY, DimensionsNet.NO_PRIMARY_NET_ID))
                )
                .pos(this.leftPos + 8, this.topPos + 47)
                .size(160, 20)
                .tooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.primary_net_switcher.clear")))
                .build();
        addRenderableWidget(clearPrimaryButton);

        int recentButtonY = this.topPos + 78;
        for (int i = 0; i < RECENT_BUTTON_COUNT; i++) {
            RecentNetButton recentButton = new RecentNetButton(this.leftPos + 8 + i * 54, recentButtonY, 50, 16);
            recentButton.button.visible = false;
            recentButton.button.active = false;
            recentButtons.add(recentButton);
            addRenderableWidget(recentButton.button);
        }

        int optionStartY = this.topPos + 107;
        for (int i = 0; i < VISIBLE_OPTION_COUNT; i++) {
            PrimaryNetOptionButton optionButton = new PrimaryNetOptionButton(this.leftPos + 8, optionStartY + i * 20, OPTION_BUTTON_WIDTH, OPTION_BUTTON_HEIGHT);
            optionButtons.add(optionButton);
            addRenderableWidget(optionButton.button);
        }

        scroller = new BigScroller(this.leftPos + 160, optionStartY + 2, VISIBLE_OPTION_COUNT * OPTION_BUTTON_HEIGHT - 17, 0, 0, pos -> {
            if (topIndex != pos) {
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
    protected void containerTick() {
        super.containerTick();
        if (menu.options != lastObservedOptions) {
            lastObservedOptions = menu.options;
            rebuildFilteredOptions();
        }

        if (menu.currentPrimaryNetId != lastObservedPrimaryNetId) {
            lastObservedPrimaryNetId = menu.currentPrimaryNetId;
            rememberRecentNet(menu.currentPrimaryNetId);
            syncRecentButtons();
            syncOptionButtons();
        } else {
            syncRecentButtons();
            syncOptionButtons();
        }

        if (dimensionsNetButton != null)
            dimensionsNetButton.active = menu.currentPrimaryNetId != DimensionsNet.NO_PRIMARY_NET_ID;

        updateRenameFieldBounds();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        super.extractBackground(guiGraphics, mouseX, mouseY, a);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0F, 0F, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        guiGraphics.text(this.font, Component.translatable("menu.text.beyonddimensions.primary_net_switcher.current", describeCurrentPrimary()), 8, 37, -12566464, false);
        guiGraphics.text(this.font, Component.translatable("menu.label.beyonddimensions.primary_net_switcher.recent"), 8, 69, -12566464, false);
        guiGraphics.text(this.font, Component.translatable("menu.label.beyonddimensions.primary_net_switcher.all_networks"), 8, 96, -12566464, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (renameField != null) {
            return true;
        }

        boolean result = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (!result) {
            result = scroller.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return result;
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleclick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (renameField != null) {
            if (renameField.mouseClicked(event, doubleclick)) {
                this.setFocused(renameField);
            }
            return true;
        }

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            PrimaryNetOptionButton optionButton = getOptionButtonAt(mouseX, mouseY);
            if (optionButton != null && optionButton.option != null) {
                if (canRename(optionButton.option)) {
                    startRename(optionButton.option, optionButton.optionIndex, optionButton.button.getX(), optionButton.button.getY(), optionButton.button.getWidth(), optionButton.button.getHeight());
                }
                return true;
            }
        }

        boolean result = super.mouseClicked(event, doubleclick);

        boolean inSearchField = searchField.isMouseOver(mouseX, mouseY);
        if (!inSearchField && this.getFocused() == searchField) {
            searchField.setFocused(false);
            this.setFocused(null);
        } else if (inSearchField && event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            searchField.setValue("");
            return true;
        }
        return result;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        InputConstants.Key key = InputConstants.getKey(event);

        if (renameField != null && renameField.canConsumeInput()) {
            if (key.getValue() == GLFW.GLFW_KEY_ENTER || key.getValue() == GLFW.GLFW_KEY_KP_ENTER) {
                submitRename();
                return true;
            }
            if (key.getValue() == GLFW.GLFW_KEY_ESCAPE) {
                cancelRename();
                return true;
            }
            return renameField.keyPressed(event);
        }

        if (searchField != null && searchField.canConsumeInput() && key.getValue() != GLFW.GLFW_KEY_ESCAPE) {
            searchField.keyPressed(event);
            return true;
        }

        if (this.minecraft.options.keyInventory.isActiveAndMatches(key) || BDShortKeys.OPEN_PRIMARY_NET_SWITCHER_KEY.getKey() == key) {
            onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        if (renameField != null && renameField.canConsumeInput()) {
            return renameField.charTyped(event);
        }

        if (searchField != null && searchField.canConsumeInput()) {
            return searchField.charTyped(event);
        }
        return super.charTyped(event);
    }

    private void rebuildFilteredOptions() {
        String searchText = searchField == null ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
        List<PrimaryNetOption> nextFilteredOptions = new ArrayList<>();
        for (PrimaryNetOption option : menu.options) {
            String searchableText = buildSearchableText(option);
            if (searchText.isEmpty() || searchableText.contains(searchText)) {
                nextFilteredOptions.add(option);
            }
        }

        filteredOptions = nextFilteredOptions;
        if (filteredOptions.isEmpty()) {
            selectedIndex = -1;
            topIndex = 0;
        } else {
            if (selectedIndex >= filteredOptions.size()) {
                selectedIndex = filteredOptions.size() - 1;
            }
            if (selectedIndex >= 0) {
                ensureSelectionVisible();
            }
            topIndex = Math.max(0, Math.min(topIndex, Math.max(0, filteredOptions.size() - VISIBLE_OPTION_COUNT)));
        }

        syncRecentButtons();
        syncOptionButtons();
    }

    private void onSearchTextChanged(String text) {
        if (searchField != null) {
            searchField.setSuggestion(text.isEmpty()
                    ? Component.translatable("menu.label.beyonddimensions.primary_net_switcher.search").getString()
                    : "");
        }
        rebuildFilteredOptions();
    }

    private String buildSearchableText(PrimaryNetOption option) {
        return (option.getNetworkName().getString() + " (#" + option.netId() + ") " + buildPermissionLabel(option.permission()).getString()).toLowerCase(Locale.ROOT);
    }

    private Component buildPermissionLabel(NetPermissionlevel permission) {
        return Component.translatable("menu.text.beyonddimensions.primary_net_switcher.permission." + permission.name().toLowerCase(Locale.ROOT));
    }

    private void syncRecentButtons() {
        List<Integer> recentNetIds = new ArrayList<>();
        for (int recentNetId : RECENT_PRIMARY_NET_IDS) {
            if (menu.options.stream().anyMatch(option -> option.netId() == recentNetId)) {
                recentNetIds.add(recentNetId);
            }
            if (recentNetIds.size() >= RECENT_BUTTON_COUNT) {
                break;
            }
        }

        for (int i = 0; i < recentButtons.size(); i++) {
            RecentNetButton recentButton = recentButtons.get(i);
            if (i < recentNetIds.size()) {
                recentButton.button.visible = true;
                recentButton.button.active = true;
                recentButton.load(recentNetIds.get(i));
            } else {
                recentButton.button.visible = false;
                recentButton.button.active = false;
            }
        }
    }

    private void syncOptionButtons() {
        int maxTopIndex = Math.max(0, filteredOptions.size() - VISIBLE_OPTION_COUNT);
        topIndex = Math.max(0, Math.min(topIndex, maxTopIndex));
        scroller.updateScrollPosition(topIndex, maxTopIndex);

        for (int i = 0; i < optionButtons.size(); i++) {
            int optionIndex = topIndex + i;
            PrimaryNetOptionButton optionButton = optionButtons.get(i);
            if (optionIndex < filteredOptions.size()) {
                PrimaryNetOption option = filteredOptions.get(optionIndex);
                optionButton.load(option, optionIndex == selectedIndex, option.netId() == menu.currentPrimaryNetId);
            } else {
                optionButton.clear();
            }
        }
        updateRenameFieldBounds();
    }

    private void ensureSelectionVisible() {
        if (selectedIndex < 0) {
            return;
        }
        if (selectedIndex < topIndex) {
            topIndex = selectedIndex;
        } else if (selectedIndex >= topIndex + VISIBLE_OPTION_COUNT) {
            topIndex = selectedIndex - VISIBLE_OPTION_COUNT + 1;
        }
    }

    private void sendSetPrimary(int netId) {
        ClientPacketDistributor.sendToServer(new PrimaryNetSwitchActionPacket(PrimaryNetSwitchAction.SET_EXPLICIT, netId));
    }

    private void selectNetAndSend(int netId) {
        if (renameField != null) {
            return;
        }

        for (int i = 0; i < filteredOptions.size(); i++) {
            if (filteredOptions.get(i).netId() == netId) {
                selectedIndex = i;
                ensureSelectionVisible();
                syncOptionButtons();
                break;
            }
        }
        sendSetPrimary(netId);
    }

    private Component describeCurrentPrimary() {
        return menu.currentPrimaryNetId == DimensionsNet.NO_PRIMARY_NET_ID
                ? Component.translatable("menu.text.beyonddimensions.primary_net_switcher.none")
                : Component.literal("#" + menu.currentPrimaryNetId);
    }

    private static void rememberRecentNet(int netId) {
        if (netId < 0) {
            return;
        }

        RECENT_PRIMARY_NET_IDS.removeIf(existingNetId -> existingNetId == netId);
        RECENT_PRIMARY_NET_IDS.addFirst(netId);
        while (RECENT_PRIMARY_NET_IDS.size() > 8) {
            RECENT_PRIMARY_NET_IDS.removeLast();
        }
    }

    private int rebuildImageHeight() {
        return BACKGROUND_HEIGHT;
    }

    private boolean startRename(PrimaryNetOption option, int optionIndex, int x, int y, int width, int height) {
        if (!canRename(option)) {
            return false;
        }

        cancelRename();
        selectedIndex = optionIndex;
        renamingNetId = option.netId();
        renameField = new EditBox(getFont(), x, y + 2, width, height - 4,
                Component.translatable("menu.label.beyonddimensions.primary_net_switcher.rename"));
        renameField.setMaxLength(DimensionsNet.MAX_NETWORK_NAME_LENGTH);
        renameField.setBordered(true);
        renameField.setVisible(true);
        renameField.setTextColor(0xFFFFFFFF);
        renameField.setValue(option.customName());
        updateRenameSuggestion(option);
        renameField.setResponder(text -> updateRenameSuggestion(option));
        addRenderableWidget(renameField);
        this.setFocused(renameField);
        renameField.setFocused(true);
        syncOptionButtons();
        return true;
    }

    private boolean canRename(PrimaryNetOption option) {
        return option != null && (option.permission() == NetPermissionlevel.Owner || option.permission() == NetPermissionlevel.Manager);
    }

    private void updateRenameSuggestion(PrimaryNetOption option) {
        if (renameField == null) {
            return;
        }

        renameField.setSuggestion(renameField.getValue().isEmpty()
                ? DimensionsNet.getNetworkName(option.netId(), "").getString()
                : null);
    }

    private void submitRename() {
        if (renameField == null || renamingNetId == DimensionsNet.NO_PRIMARY_NET_ID) {
            return;
        }

        ClientPacketDistributor.sendToServer(new RenameNetPacket(renamingNetId, renameField.getValue()));
        cancelRename();
    }

    private void cancelRename() {
        if (renameField != null) {
            removeWidget(renameField);
            if (this.getFocused() == renameField) {
                this.setFocused(null);
            }
        }
        renameField = null;
        renamingNetId = DimensionsNet.NO_PRIMARY_NET_ID;
        syncOptionButtons();
    }

    private void updateRenameFieldBounds() {
        if (renameField == null) {
            return;
        }

        for (PrimaryNetOptionButton optionButton : optionButtons) {
            if (optionButton.option != null && optionButton.option.netId() == renamingNetId) {
                renameField.setX(optionButton.button.getX());
                renameField.setY(optionButton.button.getY() + 2);
                renameField.setWidth(optionButton.button.getWidth());
                renameField.setHeight(optionButton.button.getHeight() - 4);
                return;
            }
        }
        cancelRename();
    }

    private @Nullable PrimaryNetOptionButton getOptionButtonAt(double mouseX, double mouseY) {
        for (PrimaryNetOptionButton optionButton : optionButtons) {
            Button button = optionButton.button;
            if (button.visible
                    && mouseX >= button.getX() && mouseY >= button.getY()
                    && mouseX < button.getX() + button.getWidth()
                    && mouseY < button.getY() + button.getHeight()) {
                return optionButton;
            }
        }
        return null;
    }

    private void restoreMousePosition() {
        if (UIDataHelper.lastMousePos == null)
            return;

        Window window = Minecraft.getInstance().getWindow();
        GLFW.glfwSetCursorPos(
                window.handle(),
                UIDataHelper.lastMousePos.x,
                UIDataHelper.lastMousePos.y
        );
    }

    private void saveMousePosition() {
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(Minecraft.getInstance().getWindow().handle(), xpos, ypos);
        UIDataHelper.lastMousePos = new Vec2(
                (float) xpos[0],
                (float) ypos[0]
        );
        UIDataHelper.isTransfer = true;
    }

    private final class PrimaryNetOptionButton {
        private final Button button;
        private PrimaryNetOption option;
        private int optionIndex = -1;

        private PrimaryNetOptionButton(int x, int y, int width, int height) {
            this.button = Button.builder(Component.empty(), ignored -> {
                if (option != null) {
                    onSelected();
                }
            }).pos(x, y).size(width, height).build();
            this.button.visible = false;
            this.button.active = false;
        }

        private void load(PrimaryNetOption option, boolean selected, boolean currentPrimary) {
            this.option = option;
            this.optionIndex = PrimaryNetSwitcherGUI.this.topIndex + optionButtons.indexOf(this);
            this.button.visible = option.netId() != renamingNetId;
            this.button.active = !currentPrimary;
            this.button.setMessage(buildLabel(option, currentPrimary));
            this.button.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.primary_net_switcher.option", option.netId(), buildPermissionLabel(option.permission()))));
        }

        private void clear() {
            this.option = null;
            this.optionIndex = -1;
            this.button.visible = false;
            this.button.active = false;
            this.button.setMessage(Component.empty());
            this.button.setTooltip(null);
        }

        private void onSelected() {
            if (option == null) {
                return;
            }
            if (optionIndex >= 0) {
                selectedIndex = optionIndex;
            }
            selectNetAndSend(option.netId());
        }

        private Component buildLabel(PrimaryNetOption option, boolean currentPrimary) {
            return option.getNetworkName().copy()
                    .append(Component.literal(" (#" + option.netId() + ") "))
                    .append(PrimaryNetSwitcherGUI.this.buildPermissionLabel(option.permission()))
                    .append(currentPrimary ? Component.translatable("menu.text.beyonddimensions.primary_net_switcher.current_suffix") : Component.empty());
        }
    }

    private final class RecentNetButton {
        private final Button button;
        private int targetNetId = DimensionsNet.NO_PRIMARY_NET_ID;

        private RecentNetButton(int x, int y, int width, int height) {
            this.button = Button.builder(Component.empty(), ignored -> {
                if (targetNetId >= 0) {
                    selectNetAndSend(targetNetId);
                }
            }).pos(x, y).size(width, height).build();
        }

        private void load(int netId) {
            this.targetNetId = netId;
            this.button.setMessage(Component.literal("#" + netId));
            this.button.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.primary_net_switcher.recent", netId)));
        }

    }
}
