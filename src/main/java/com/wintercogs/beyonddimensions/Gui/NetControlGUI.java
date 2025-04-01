package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.api.widget.IGuiAction;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.keys.StringKey;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.NetControlAction;
import com.wintercogs.beyonddimensions.DataBase.NetPermissionlevel;
import com.wintercogs.beyonddimensions.DataBase.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Gui.Factory.PosGuiFactory;
import com.wintercogs.beyonddimensions.Gui.Sync.PlayerPermissionsSync;
import com.wintercogs.beyonddimensions.Gui.Widgets.ClickAbleButton;
import com.wintercogs.beyonddimensions.Gui.Widgets.PermissionsButton;
import com.wintercogs.beyonddimensions.Network.Packet.toServer.NetControlActionPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NetControlGUI extends BDOrderedContainerGUI
{
    public static PosGuiFactory factory =  new PosGuiFactory("net_control_gui",() ->{
        return new NetControlGUI();
    });


    public List<PlayerPermissionInfo> permissions = new ArrayList<>(); // 每个按钮对应一个
    private PlayerPermissionInfo currentPermission; // 页面当前显示的详细信息

    private List<IWidget> needUpdateWidgets = new ArrayList<>();


    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        ModularPanel panel = super.buildUI(guiData, guiSyncManager);
        stackTypedHandler = null;
        viewerStackTypedHandler = null;

        ListWidget listWidget = new ListWidget(
                (permission) ->{
                    if(permission instanceof PlayerPermissionInfo playerPermissionInfo)
                    {
                        PermissionsButton button = new PermissionsButton(playerPermissionInfo){
                            @Override
                            public Result onMousePressed(int mouseButton)
                            {
                                Result result = super.onMousePressed(mouseButton);
                                rebuildCurrentInfo(panel,playerPermissionInfo);
                                return result;
                            }
                        };
                        button.width(30);
                        button.overlay(new StringKey(playerPermissionInfo.getName()));
                        return button;
                    }
                    return null;
                },
                (widget) ->{
                    if(widget instanceof PermissionsButton permissionsButton)
                        return permissionsButton.permission;

                    return null;
                }

        );



        if(!guiData.isClient())
        {
            DimensionsNet net = DimensionsNet.getNetFromPlayer(guiData.getPlayer());
            if(net != null)
            {
                this.permissions = new ArrayList<>(net.getPlayerPermissionInfoMap(guiData.getPlayer().world).values()) ;
            }
        }

        PlayerPermissionsSync sync = new PlayerPermissionsSync(permissions);
        sync.setChangeListener(
                () -> {
                    this.permissions = sync.getValue();

                    int size = listWidget.getValues().size();
                    if(size > 0)
                    {
                        for(int i = size-1; i >=0 ; i--)
                        {
                            listWidget.remove(i);
                        }
                    }


                    int newSize = permissions.size();
                    for(int i = 0; i < newSize; i++)
                    {
                        listWidget.add(permissions.get(i),i);
                    }

                    if(currentPermission != null)
                    {
                        UUID nowPlayer = currentPermission.getPlayerId();
                        boolean find = false;

                        for(PlayerPermissionInfo permission : permissions)
                        {
                            if(permission.getPlayerId().equals(nowPlayer))
                            {
                                currentPermission = permission;
                                find = true;
                            }
                        }

                        if(!find)
                        {
                            currentPermission = null;
                        }

                        rebuildCurrentInfo(panel,currentPermission);
                    }
                }
        );

        if(!NetworkUtils.isClient())
        {
            DimensionsNet net = DimensionsNet.getNetFromPlayer(guiData.getPlayer());
            if(net != null)
                sync.setNet(net);
        }

        guiSyncManager.syncValue("permissions_sync",sync);

        TextWidget nameText = new TextWidget(new StringKey("名称:") );
        TextWidget levelText = new TextWidget(new StringKey("权限:"));
        needUpdateWidgets.add(nameText);
        needUpdateWidgets.add(levelText);

        nameText.leftRel(0.5f).topRel(0.1f);
        levelText.leftRel(0.5f).topRel(0.2f);


        ClickAbleButton ownerButton = new ClickAbleButton()
        {
            @Override
            public Result onMousePressed(int mouseButton)
            {
                Result result = super.onMousePressed(mouseButton);
                if(currentPermission != null)
                    PacketRegister.INSTANCE.sendToServer(new NetControlActionPacket(currentPermission.getPlayerId(),NetControlAction.SetOwner));

                return result;
            }
        };
        ClickAbleButton managerButton = new ClickAbleButton(){
            @Override
            public Result onMousePressed(int mouseButton)
            {
                Result result = super.onMousePressed(mouseButton);
                if(currentPermission != null)
                    PacketRegister.INSTANCE.sendToServer(new NetControlActionPacket(currentPermission.getPlayerId(),NetControlAction.SetManager));

                return result;
            }
        };
        ClickAbleButton removeManagerButton = new ClickAbleButton(){
            @Override
            public Result onMousePressed(int mouseButton)
            {
                Result result = super.onMousePressed(mouseButton);
                if(currentPermission != null)
                    PacketRegister.INSTANCE.sendToServer(new NetControlActionPacket(currentPermission.getPlayerId(),NetControlAction.RemoveManager));

                return result;
            }
        };
        ClickAbleButton removeMemberButton = new ClickAbleButton(){
            @Override
            public Result onMousePressed(int mouseButton)
            {
                Result result = super.onMousePressed(mouseButton);
                if(currentPermission != null)
                    PacketRegister.INSTANCE.sendToServer(new NetControlActionPacket(currentPermission.getPlayerId(),NetControlAction.RemovePlayer));

                return result;
            }
        };

        ownerButton.leftRel(0.5f).topRel(0.3f);
        managerButton.leftRel(0.5f).topRel(0.45f);
        removeManagerButton.leftRel(0.65f).topRel(0.3f);
        removeMemberButton.leftRel(0.65f).topRel(0.45f);

        ownerButton.overlay(new StringKey("移交所有权"));
        managerButton.overlay(new StringKey("设为管理"));
        removeManagerButton.overlay(new StringKey("取消管理"));
        removeMemberButton.overlay(new StringKey("移除成员"));

        listWidget.size(32,143).top(4).left(7);


        return panel.child(listWidget).child(nameText).child(levelText)
                .child(ownerButton).child(managerButton).child(removeManagerButton).child(removeMemberButton);
    }


    public void rebuildCurrentInfo(ModularPanel panel,PlayerPermissionInfo info)
    {
        this.currentPermission = info;

        if(this.currentPermission != null)
        {
            TextWidget nameText = new TextWidget(new StringKey("名称:"+ currentPermission.getName()) );
            TextWidget levelText = new TextWidget(new StringKey("权限:"+ currentPermission.getLevel().toString()));

            for(IWidget widget : needUpdateWidgets)
            {
                panel.remove(widget);
            }

            needUpdateWidgets.clear();
            needUpdateWidgets.add(nameText);
            needUpdateWidgets.add(levelText);

            nameText.leftRel(0.5f).topRel(0.1f);
            levelText.leftRel(0.5f).topRel(0.2f);

            panel.child(nameText);
            panel.child(levelText);
            WidgetTree.resize(panel);
        }
        else
        {
            TextWidget nameText = new TextWidget(new StringKey("名称:") );
            TextWidget levelText = new TextWidget(new StringKey("权限:"));

            for(IWidget widget : needUpdateWidgets)
            {
                panel.remove(widget);
            }

            needUpdateWidgets.clear();
            needUpdateWidgets.add(nameText);
            needUpdateWidgets.add(levelText);

            nameText.leftRel(0.5f).topRel(0.1f);
            levelText.leftRel(0.5f).topRel(0.2f);

            panel.child(nameText);
            panel.child(levelText);
            WidgetTree.resize(panel);
        }


    }


    @Override
    public SlotGroupWidget buildStackTypedSlots(IStackTypedHandler stackTypedHandler)
    {
        return null;
    }

    @Override
    protected boolean SeachTextMatch(IStackType stack)
    {
        return false;
    }

    @Override
    protected List<Integer> SortIndexList(List<IStackType> stacksSource, List<Integer> indicesSource)
    {
        return null;
    }


    public static void handlePlayerAction(UUID receiver, NetControlAction action, DimensionsNet net, EntityPlayerMP player)
    {
        if(action == NetControlAction.SetOwner)
        {
            // 执行者是所有者，且接收者不为玩家，则可以设置新所有者
            if(net.isOwner(player)&&!player.getUniqueID().equals(receiver))
            {
                net.setOwner(receiver);
            }
        }
        else if(action == NetControlAction.SetManager)
        {
            // 执行者是所有者，且接收者不为管理员，则可以被添加为管理员
            if(net.isOwner(player)&&!net.isManager(receiver))
            {
                net.addManager(receiver);
            }
        }
        else if(action == NetControlAction.RemoveManager)
        {
            // 执行者是所有者，且接收者为管理员，且接收者并非所有者，则可以被移除管理员权限
            if(net.isOwner(player)&&net.isManager(receiver)&&!net.isOwner(receiver))
            {
                net.removeManager(receiver);
            }
        }
        else if(action == NetControlAction.RemovePlayer)
        {
            // 管理员可以移除任何非管理员
            if(net.isManager(player)&&!net.isManager(receiver))
            {
                net.removePlayer(receiver);
            }
            else if(player.getUniqueID().equals(receiver)&&!net.isOwner(receiver)) // 任何人都可以直接移除自己，除非是所有者
            {
                net.removePlayer(receiver);
            }
            else if(net.isOwner(player)&&!player.getUniqueID().equals(receiver)) // 所有者可以移除自己之外的任何人
            {
                net.removePlayer(receiver);
            }
        }

    }
}
