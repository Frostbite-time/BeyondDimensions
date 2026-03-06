package com.wintercogs.beyonddimensions.network;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.client.gui.NetMenuType;
import com.wintercogs.beyonddimensions.common.component.ItemStackContents;
import com.wintercogs.beyonddimensions.common.init.ModDataComponents;
import com.wintercogs.beyonddimensions.common.item.NetMagnetItem;
import com.wintercogs.beyonddimensions.common.item.NetTerminalItem;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.common.menu.NetControlMenu;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.network.packet.both.QuickDataTagPacket;
import com.wintercogs.beyonddimensions.network.packet.both.SetSlotDirectlyPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.*;
import com.wintercogs.beyonddimensions.network.packet.s2c.DisorderedSlotGroupSyncPacket;
import com.wintercogs.beyonddimensions.network.packet.s2c.OrderedStackTypedSlotPacket;
import com.wintercogs.beyonddimensions.network.packet.s2c.PutHandItemToNetPacket;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;

public class ServerPayloadHandler
{
    private static final Logger LOGGER = LogUtils.getLogger();
    // 实现单例
    private static final ServerPayloadHandler INSTANCE = new ServerPayloadHandler();

    public static ServerPayloadHandler getInstance()
    {
        return INSTANCE;
    }


    public void handleOpenNetGuiPacket(final OpenNetGuiPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    //获取玩家上下文
                    Player player = context.player();

                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if (net != null)
                    {
                        NetMenuType targetMenu = packet.target();
                        if (targetMenu == NetMenuType.NET_CRAFT_MENU)
                        {
                            player.openMenu(new SimpleMenuProvider(
                                    (containerId, playerInventory, _player) -> new DimensionsCraftMenu(DimensionsCraftMenu.Dimensions_Craft_Menu.get(), containerId, playerInventory, net.getUnifiedStorage(), null, null),
                                    Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                            ));
                        }
                        else if (targetMenu == NetMenuType.NET_MENU)
                        {
                            player.openMenu(new SimpleMenuProvider(
                                    (containerId, playerInventory, _player) -> new DimensionsNetMenu(DimensionsNetMenu.Dimensions_Net_Menu.get(), containerId, playerInventory, net.getUnifiedStorage()),
                                    Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                            ));
                        }
                        else if (targetMenu == NetMenuType.NET_CRAFT_TERMINAL)
                        {
                            ItemStack terminalStack = null;
                            if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof NetTerminalItem)
                                terminalStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                            else if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof NetTerminalItem)
                                terminalStack = player.getItemInHand(InteractionHand.OFF_HAND);
                            else
                            {
                                for (ItemStack itemStack : player.getInventory().items)
                                {
                                    if (itemStack.getItem() instanceof NetTerminalItem)
                                    {
                                        terminalStack = itemStack;
                                        break;
                                    }

                                }

                                if (terminalStack == null && BeyondDimensions.CuriosLoaded)
                                {
                                    terminalStack = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                                            .flatMap(iCuriosItemHandler ->
                                                    iCuriosItemHandler.findFirstCurio(itemStack ->
                                                            itemStack.getItem() instanceof NetTerminalItem &&
                                                                    itemStack.has(ModDataComponents.NET_ID_DATA) &&
                                                                    itemStack.get(ModDataComponents.NET_ID_DATA) >= 0
                                                    )
                                            )
                                            .map(slotResult -> slotResult.stack())
                                            .orElse(null);
                                }
                            }

                            if (terminalStack != null)
                            {
                                if (terminalStack.get(ModDataComponents.CRAFT_SLOTS) == null)
                                    terminalStack.set(ModDataComponents.CRAFT_SLOTS, new ItemStackContents(NonNullList.withSize(9, ItemStack.EMPTY)));

                                NetTerminalItem.contextMap.put(player, new NetTerminalItem.MenuTriggerContext(InteractionHand.MAIN_HAND, terminalStack));
                                player.openMenu((NetTerminalItem) terminalStack.getItem());
                            }
                        }
                    }

                }
        );

    }

    public void handleCallSeverClickPacket(final CallSeverClickPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if (player.containerMenu instanceof BDBaseMenu menu)
                    {
                        menu.customClickHandler(packet.slotIndex(), packet.clickItem(), packet.button(), packet.shiftDown());
                        menu.broadcastChanges();
                    }
                }

        );
    }

    public void handlePlayerPermissionInfoPacket(final PlayerPermissionInfoPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }

    public void handleNetControlActionPacket(final NetControlActionPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    NetControlMenu menu;
                    if (!(player.containerMenu instanceof NetControlMenu))
                    {
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    menu = (NetControlMenu) player.containerMenu;
                    menu.handlePlayerAction(packet.receiver(), packet.action());
                }

        );
    }

    public void handleRecipeFillC2SPacket(final RecipeFillC2SPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();

                    if (player.containerMenu instanceof DimensionsCraftMenu menu)
                    {
                        //服务端处理示意
                        //1.解析数组
                        //2.为每一个槽位在背包和存储中寻找资源填入
                        menu.transferRecipe(packet.keys(), packet.amount());
                    }
                }

        );
    }

    public void handleClickTransferCraftButtonPacket(final ClickTransferCraftButtonPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();

                    if (player.containerMenu instanceof DimensionsCraftMenu menu)
                    {
                        //服务端处理示意
                        //1.解析数组
                        //2.为每一个槽位在背包和存储中寻找资源填入
                        menu.cleanCraftSlots(packet.toStorage());
                    }
                }

        );
    }

    public void handleBatchTransferPacket(final BatchTransferPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    if (packet.clickStack().key() instanceof ItemStackKey clickItem)
                    {
                        Player player = context.player();

                        if (player.containerMenu instanceof BDBaseMenu menu)
                        {
                            // 批量转移到存储
                            if (packet.dirToStorage())
                            {
                                for (Slot invSlot : menu.slots)
                                {
                                    if (menu.inventoryStartIndex <= invSlot.index && invSlot.index < menu.inventoryEndIndex)
                                    {
                                        if (clickItem.equals(new ItemStackKey(invSlot.getItem())))
                                            menu.customClickHandler(invSlot.index, new KeyAmount(new ItemStackKey(invSlot.getItem()), invSlot.getItem().getCount()), 0, true);
                                    }
                                }
                            }
                            // 存储到背包
                            else if (menu instanceof DimensionsNetMenu netMenu)
                            {
                                if (!packet.clickStack().isEmpty())
                                {
                                    AbstractUnorderedStackHandler storage = netMenu.storage;

                                    // 遍历目标槽位
                                    for (int targetSlotIndex = menu.inventoryStartIndex; targetSlotIndex < menu.inventoryEndIndex && storage.hasStack(clickItem); targetSlotIndex++)
                                    {
                                        Slot slot = menu.slots.get(targetSlotIndex);

                                        KeyAmount extract = storage.extract(clickItem, Integer.MAX_VALUE, false, false); // 防止数量过多无法回插
                                        if (extract.toStack() instanceof ItemStack extractedStack)
                                        {
                                            ItemStack remaining = slot.safeInsert(extractedStack);
                                            if (!remaining.isEmpty())
                                                storage.insert(new ItemStackKey(remaining), remaining.getCount(), false);
                                        }
                                        else  // 防御操作，如果不是物品堆，整个回插
                                            storage.insert(extract.key(), extract.amount(), false);
                                    }
                                }
                            }

                            menu.broadcastChanges();
                        }
                    }
                }
        );
    }

    public void handlePickBlockFromNetPacket(final PickBlockFromNetPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if (!player.getMainHandItem().isEmpty()) return;
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if (net == null) return;
                    UnifiedStorage storage = net.getUnifiedStorage();

                    IStackKey<?> target = null;
                    for (KeyAmount stack : storage.getStorage())
                    {
                        if (stack.key() instanceof ItemStackKey itemStackKey)
                        {
                            if (itemStackKey.getSource() == packet.targetStack().getItem())
                            {
                                target = itemStackKey;
                                break;
                            }
                        }
                    }

                    if (target != null && player.getMainHandItem().isEmpty())
                    {
                        ItemStack extract = (ItemStack) storage.extract(target, target.getVanillaMaxStackSize(), false, false).toStack();
                        player.setItemInHand(InteractionHand.MAIN_HAND, extract);
                    }
                }
        );
    }

    public void handlePutHandItemToNetPacket(final PutHandItemToNetPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if (player.getMainHandItem().isEmpty()) return;
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if (net == null) return;
                    UnifiedStorage storage = net.getUnifiedStorage();
                    KeyAmount remaining = storage.insert(new ItemStackKey(player.getMainHandItem()), player.getMainHandItem().getCount(), false);
                    player.getMainHandItem().setCount((BDMath.clampLongToInt(remaining.amount())));
                }
        );
    }

    public void handleOrderedStackTypedSlotPacket(final OrderedStackTypedSlotPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }
        );
    }

    public void handleSetSlotDirectlyPacket(final SetSlotDirectlyPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if (player.containerMenu instanceof AbstractContainerMenu menu)
                    {
                        if (menu.slots.get(packet.slotId()) instanceof AbstractStackTypedSlot slot)
                        {
                            slot.setStackDirectly(packet.stack().key(), packet.stack().amount());
                        }
                    }
                }
        );
    }

    public void handleDisorderedSlotGroupSyncPacket(final DisorderedSlotGroupSyncPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }
        );
    }

    public void handleQuickDataTagPacket(final QuickDataTagPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if (player.containerMenu instanceof BDBaseMenu menu)
                    {
                        menu.readQuickDataTag(packet.tag());
                    }
                }
        );
    }

    public void handleToggleMagnetPacket(final ToggleMagnetPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();

                    for (ItemStack stack : player.getInventory().items)
                    {
                        if (stack.getItem() instanceof NetMagnetItem)
                        {
                            if (stack.has(ModDataComponents.CONTROL_MODE))
                            {
                                if (stack.get(ModDataComponents.CONTROL_MODE) == RedStoneControlMode.IGNORE)
                                {
                                    stack.set(ModDataComponents.CONTROL_MODE, RedStoneControlMode.NOT_WORKING);
                                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.magnet.close"));
                                }
                                else if (stack.get(ModDataComponents.CONTROL_MODE) == RedStoneControlMode.NOT_WORKING)
                                {
                                    stack.set(ModDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE);
                                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.magnet.open"));
                                }
                            }
                        }
                    }

                    if (BeyondDimensions.CuriosLoaded)
                    {
                        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                            List<ItemStack> curios = handler.findCurios(stack -> !stack.isEmpty())
                                    .stream()
                                    .map(SlotResult::stack)
                                    .toList();

                            for (ItemStack stack : curios)
                            {
                                if (stack.getItem() instanceof NetMagnetItem)
                                {
                                    if (stack.has(ModDataComponents.CONTROL_MODE))
                                    {
                                        if (stack.get(ModDataComponents.CONTROL_MODE) == RedStoneControlMode.IGNORE)
                                        {
                                            stack.set(ModDataComponents.CONTROL_MODE, RedStoneControlMode.NOT_WORKING);
                                            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.magnet.close"));
                                        }
                                        else if (stack.get(ModDataComponents.CONTROL_MODE) == RedStoneControlMode.NOT_WORKING)
                                        {
                                            stack.set(ModDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE);
                                            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.magnet.open"));
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
        );
    }

}
