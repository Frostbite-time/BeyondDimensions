package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.item.XpExchangeSettings;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * 经验交换：只有一个开关和一个目标等级，两个设置各自校验。
 */
public class XpExchangeMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<XpExchangeMenu>> XP_EXCHANGE_MENU = MENU_TYPES.register("xp_exchange_menu", () -> IMenuTypeExtension.create(XpExchangeMenu::new));

    private static final int invSlotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_CONNECTION_HEIGHT * 5 + 7;

    public final ItemStack menuStack;

    // 以下字段仅用于客户端镜像，由同步schema的setter写入
    private boolean clientKeep;
    private boolean keepEditable;
    private int clientTarget;
    private boolean targetEditable;

    private static final SyncSchema<XpExchangeMenu> SCHEMA = SyncSchema.<XpExchangeMenu>builder("beyonddimensions:xp_exchange", 1)
            .field("value.keep", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.clientKeep : m.menuStack.getOrDefault(BDDataComponents.XP_NET_KEEP_MODE, false),
                    (m, value) -> m.clientKeep = value)
            .field("editable.keep", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.keepEditable : editable(m),
                    (m, value) -> m.keepEditable = value)
            .field("value.target", SyncCodecs.INT,
                    m -> m.player.level().isClientSide() ? m.clientTarget : XpExchangeSettings.getTargetLevel(m.menuStack),
                    (m, value) -> m.clientTarget = value)
            .field("editable.target", SyncCodecs.BOOLEAN,
                    m -> m.player.level().isClientSide() ? m.targetEditable : editable(m),
                    (m, value) -> m.targetEditable = value)
            .build();

    private static final MenuAction<XpExchangeMenu, Boolean> SET_KEEP = MenuAction.of("set.keep",
            SyncCodecs.BOOLEAN, (m, player, value) -> {
                if (!editable(m)) return false;
                if (m.menuStack.getOrDefault(BDDataComponents.XP_NET_KEEP_MODE, false) != value)
                {
                    m.menuStack.set(BDDataComponents.XP_NET_KEEP_MODE, value);
                    m.markItemChanged();
                }
                return true;
            });

    private static final MenuAction<XpExchangeMenu, Integer> SET_TARGET = MenuAction.of("set.target",
            SyncCodecs.INT, (m, player, value) -> {
                if (!editable(m) || value < 0 || value > XpExchangeSettings.MAX_TARGET_LEVEL) return false;
                if (XpExchangeSettings.getTargetLevel(m.menuStack) != value)
                {
                    XpExchangeSettings.setTargetLevel(m.menuStack, value);
                    m.markItemChanged();
                }
                return true;
            });

    private final MenuSync<XpExchangeMenu> synchronization;

    @Override
    public MenuSync<XpExchangeMenu> menuSync()
    {
        return synchronization;
    }

    public XpExchangeMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, playerInventory.player.getItemInHand(data.readEnum(InteractionHand.class)));
    }

    public XpExchangeMenu(int containerId, Inventory playerInventory, ItemStack menuStack)
    {
        super(XP_EXCHANGE_MENU.get(), containerId, playerInventory);
        this.menuStack = menuStack;
        if (!player.level().isClientSide()) XpExchangeSettings.ensureComponents(this.menuStack);
        addPlayerInv(playerInventory);
        synchronization = BdMenuActions.attach(this, MenuSync.bind(this, SCHEMA).action(SET_KEEP).action(SET_TARGET));
    }

    private static boolean editable(XpExchangeMenu menu)
    {
        return !menu.player.isSpectator();
    }

    private void markItemChanged()
    {
        player.getInventory().setChanged();
    }

    // 客户端读取接口：本页只暴露自己的开关与目标等级

    public boolean keep()
    {
        return clientKeep;
    }

    public boolean keepEditable()
    {
        return keepEditable;
    }

    public int target()
    {
        return clientTarget;
    }

    public boolean targetEditable()
    {
        return targetEditable;
    }

    public boolean requestKeep(boolean value)
    {
        return keepEditable && synchronization.request(SET_KEEP, value).queued();
    }

    public boolean requestTarget(int value)
    {
        return targetEditable && value >= 0 && value <= XpExchangeSettings.MAX_TARGET_LEVEL
                && synchronization.request(SET_TARGET, value).queued();
    }

    private void addPlayerInv(Inventory playerInventory)
    {
        inventoryStartIndex = slots.size();
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, invSlotStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 4 + invSlotStartY + 3 * 18));
        }
        inventoryEndIndex = slots.size();
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return player.level().isClientSide() || com.wintercogs.beyonddimensions.util.InventoryHelper.containsExactStack(player, menuStack);
    }
}
