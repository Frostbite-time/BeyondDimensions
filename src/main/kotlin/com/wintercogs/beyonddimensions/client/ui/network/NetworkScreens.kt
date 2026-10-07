package com.wintercogs.beyonddimensions.client.ui.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet
import com.wintercogs.beyonddimensions.api.dimensionnet.NetControlAction
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetSwitchAction
import com.wintercogs.beyonddimensions.client.gui.NetMenuType
import com.wintercogs.beyonddimensions.client.ui.base.BdMenuScreen
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.init.BDItems
import com.wintercogs.beyonddimensions.common.menu.NetControlMenu
import com.wintercogs.beyonddimensions.common.menu.PrimaryNetSwitcherMenu
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.PrimaryNetSwitchActionPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.RenameNetPacket
import com.wintercogs.beyonddimensions.util.UIDataHelper
import dev.compixel.forge.item.ItemIcon
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreText
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.network.PacketDistributor
import java.util.*

private fun roleLabel(role: NetPermissionlevel) = tr("ui.beyonddimensions.network.role.${role.name.lowercase()}")

/** 列表区至少放得下五行，内容少时窗口也保持面板的比例 */
private const val LIST_MIN_HEIGHT = 5 * 20 + 4 * 2

/** 成员与网络列表的底槽：深一层的底色和细线框，没有行的部分也看得出是列表区 */
@Composable
private fun ListWell(content: @Composable BoxScope.() -> Unit) {
    val colors = Bd.colors
    Box(Modifier.fillMaxWidth().background(colors.sunken).border(1.dp, colors.line).padding(2.dp), content = content)
}

/** 角色徽章：所有者用强调色实底，管理员用浅色，成员为中性色 */
@Composable
private fun RoleChip(role: NetPermissionlevel, label: String) {
    val colors = Bd.colors
    val (ink, fill) =
        when (role) {
            NetPermissionlevel.Owner -> colors.onAccent to colors.accentDeep
            NetPermissionlevel.Manager -> colors.accentDeep to colors.accentSoft
            NetPermissionlevel.Member -> colors.muted to colors.sunken
        }
    Box(Modifier.background(fill, Bd.ChipShape).padding(horizontal = 4.dp, vertical = 1.dp)) {
        OreText(label, color = ink, style = Bd.caption, maxLines = 1)
    }
}

// ---- 网络成员与权限 ----

data class MemberView(
    val id: UUID,
    val name: String,
    val role: NetPermissionlevel,
    val actions: Set<NetControlAction>,
    val self: Boolean
)

data class NetControlState(val networkId: Int, val networkName: String, val members: List<MemberView>)

sealed interface NetControlRequest {
    data class Apply(val member: UUID, val action: NetControlAction, val role: NetPermissionlevel) : NetControlRequest

    data object Refresh : NetControlRequest
}

/** 网络控制器：列出网络成员，按各自的权限提供提升、降级与移除 */
class NetControlScreen(menu: NetControlMenu, inventory: Inventory, title: Component) :
    BdMenuScreen<NetControlMenu, NetControlState, NetControlRequest>(menu, title) {
    private val text = NetControlText(title.string)

    // 成员列表没有变化时沿用上次排好序的视图
    private var members = emptyList<NetControlMenu.Member>()
    private var views = emptyList<MemberView>()

    override fun snapshot(): NetControlState {
        val current = container.members()
        if (current !== members) {
            members = current
            val self = Minecraft.getInstance().player?.uuid
            views =
                current
                    .map { member ->
                        val actions = NetControlAction.entries.filter(member::allows).toSet()
                        MemberView(member.id(), member.name(), member.role(), actions, member.id() == self)
                    }
                    .sortedWith(compareByDescending<MemberView> { it.role.ordinal }.thenBy { it.name.lowercase() })
        }
        return NetControlState(
            container.networkId(),
            DimensionsNet.getNetworkName(container.networkId(), container.networkName()).string,
            views,
        )
    }

    override fun handle(action: NetControlRequest) {
        when (action) {
            is NetControlRequest.Apply -> container.request(
                NetControlMenu.Request(
                    action.member,
                    action.action,
                    action.role
                )
            )

            NetControlRequest.Refresh -> container.refresh()
        }
    }

    @Composable
    override fun Content(state: NetControlState) {
        val colors = Bd.colors
        var chosen by remember { mutableStateOf<UUID?>(null) }
        BdScreenFrame {
            // 网络里至少有玩家自己，成员为空说明数据还没到：这时只有背景淡入，窗口等数据到了再加入进场动画
            if (state.members.isNotEmpty()) {
                // 默认选中自己；选中的成员离开网络后也回到自己
                val selected =
                    state.members.find { it.id == chosen } ?: state.members.find { it.self } ?: state.members.first()
                BdTabbedWindow(
                    Modifier.width((SIDE_RAIL_WIDTH + 250).dp),
                    header = {
                        BdHeader(
                            text.icon,
                            state.networkName,
                            ::requestClose,
                            tag = "#%04d".format(state.networkId)
                        )
                    },
                    rail = { BdRailTab(text.title, BdGlyphs.Main, selected = true) {} },
                ) {
                    BdMainPage(true) {
                        Row {
                            Column(Modifier.weight(1f)) {
                                BdSectionLabel(text.members) {
                                    OreText(state.members.size.toString(), color = colors.faint, style = Bd.caption)
                                    Spacer(Modifier.width(3.dp))
                                    BdGlyphButton(
                                        OreGlyph.CycleArrows,
                                        text.refresh,
                                        { send(NetControlRequest.Refresh) },
                                        size = 11.dp,
                                        glyphSize = 7.dp
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                ListWell {
                                    BdScrollColumn(
                                        Modifier.heightIn(min = LIST_MIN_HEIGHT.dp, max = 200.dp),
                                        Arrangement.spacedBy(2.dp)
                                    ) {
                                        for (member in state.members) {
                                            MemberRow(member, member == selected, text) { chosen = member.id }
                                        }
                                        // 只有自己时说明怎样让别人加入，免得只看到一行自己的名字
                                        if (state.members.size == 1) {
                                            OreText(
                                                text.alone,
                                                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
                                                color = colors.faint,
                                                style = Bd.caption,
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            MemberPanel(selected, text, Modifier.width(PANEL_WIDTH.dp)) { send(it) }
                        }
                    }
                }
            }
        }
    }
}

/** 右侧成员面板的宽度 */
private const val PANEL_WIDTH = 92

private class NetControlText(val title: String) {
    val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_CONTROL.get()))
    val members = tr("ui.beyonddimensions.network.members")
    val manage = tr("ui.beyonddimensions.network.manage")
    val refresh = tr("ui.beyonddimensions.network.refresh")
    val roles = NetPermissionlevel.entries.associateWith(::roleLabel)
    val actions =
        NetControlAction.entries.associateWith { tr("ui.beyonddimensions.network.action.${it.name.lowercase()}") }
    val actionHints =
        NetControlAction.entries.associateWith { tr("ui.beyonddimensions.network.action.${it.name.lowercase()}.hint") }
    val leave = tr("ui.beyonddimensions.network.action.leave")
    val leaveHint = tr("ui.beyonddimensions.network.action.leave.hint")
    val you = tr("ui.beyonddimensions.network.you")
    val alone =
        tr(
            "ui.beyonddimensions.network.alone",
            tr(BDItems.NET_MEMBER_INVITER.get().descriptionId),
            tr(BDItems.NET_MANAGER_INVITER.get().descriptionId),
        )
}

/** 成员列表的一行：点选后在右侧面板管理；选中的一行用强调色细框标出 */
@Composable
private fun MemberRow(member: MemberView, selected: Boolean, text: NetControlText, onSelect: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth()
            .height(20.dp)
            .hoverable(interaction)
            .bdClickable(interaction, enabled = !selected, onClick = onSelect)
            .background(if (selected || hovered) colors.accentSoft else colors.surface)
            .border(1.dp, if (selected) colors.accent else colors.line)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoleChip(member.role, text.roles.getValue(member.role))
        Spacer(Modifier.width(5.dp))
        OreText(member.name, Modifier.weight(1f, fill = false), color = colors.text, maxLines = 1)
        if (member.self) {
            Spacer(Modifier.width(3.dp))
            OreText(text.you, color = colors.faint, style = Bd.caption, maxLines = 1)
        }
    }
}

/**
 * 选中成员的名字、角色与四个操作。操作总是列出，当前玩家不能对这名成员执行的显示为停用；
 * 悬停时说明操作的作用。对自己执行“移除”就是退出网络。
 */
@Composable
private fun MemberPanel(
    member: MemberView,
    text: NetControlText,
    modifier: Modifier,
    send: (NetControlRequest) -> Unit,
) {
    val colors = Bd.colors
    Column(modifier) {
        BdSectionLabel(text.manage)
        Spacer(Modifier.height(5.dp))
        Column(Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.line).padding(5.dp)) {
            OreText(member.name, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoleChip(member.role, text.roles.getValue(member.role))
                if (member.self) {
                    Spacer(Modifier.width(3.dp))
                    OreText(text.you, color = colors.faint, style = Bd.caption, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (action in NetControlAction.entries) {
                val leaving = member.self && action == NetControlAction.RemovePlayer
                ActionButton(
                    if (leaving) text.leave else text.actions.getValue(action),
                    if (leaving) text.leaveHint else text.actionHints.getValue(action),
                    danger = action == NetControlAction.RemovePlayer,
                    enabled = action in member.actions,
                ) {
                    send(NetControlRequest.Apply(member.id, action, member.role))
                }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, hint: String, danger: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val ink = if (danger) colors.danger else colors.accentDeep
    val lit = hovered && enabled
    BdTooltip(hint) {
        Box(
            Modifier.fillMaxWidth()
                .height(15.dp)
                .hoverable(interaction, enabled)
                .bdClickable(interaction, enabled = enabled, onClick = onClick)
                .background(if (lit) ink else colors.surface, Bd.ChipShape)
                .border(
                    1.dp,
                    when {
                        lit -> ink
                        enabled -> ink.copy(alpha = 0.45f)
                        else -> colors.line
                    },
                    Bd.ChipShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            OreText(
                label,
                color =
                    when {
                        lit -> colors.onAccent
                        enabled -> ink
                        else -> colors.faint
                    },
                style = Bd.caption,
                maxLines = 1,
            )
        }
    }
}

// ---- 主网络切换 ----

data class NetOptionView(val id: Int, val name: String, val role: NetPermissionlevel, val renamable: Boolean)

data class PrimaryNetState(val primary: Int, val options: List<NetOptionView>)

sealed interface PrimaryNetRequest {
    data class Select(val id: Int) : PrimaryNetRequest

    data class Rename(val id: Int, val name: String) : PrimaryNetRequest

    data object Clear : PrimaryNetRequest

    data object OpenTerminal : PrimaryNetRequest
}

/** 主网络切换：搜索、选择与重命名可用的网络，并打开主网络的终端 */
class PrimaryNetScreen(menu: PrimaryNetSwitcherMenu, inventory: Inventory, title: Component) :
    BdMenuScreen<PrimaryNetSwitcherMenu, PrimaryNetState, PrimaryNetRequest>(menu, title) {
    private val text = PrimaryNetText(title.string)

    override fun snapshot() =
        PrimaryNetState(
            container.currentPrimaryNetId,
            container.options.map {
                NetOptionView(
                    it.netId(),
                    DimensionsNet.getNetworkName(it.netId(), it.customName()).string,
                    it.permission(),
                    it.permission() != NetPermissionlevel.Member,
                )
            },
        )

    override fun handle(action: PrimaryNetRequest) {
        when (action) {
            // 成员资格、管理权限与名称长度都由服务器检查
            is PrimaryNetRequest.Select ->
                PacketDistributor.sendToServer(
                    PrimaryNetSwitchActionPacket(
                        PrimaryNetSwitchAction.SET_EXPLICIT,
                        action.id
                    )
                )

            is PrimaryNetRequest.Rename -> PacketDistributor.sendToServer(RenameNetPacket(action.id, action.name))
            PrimaryNetRequest.Clear ->
                PacketDistributor.sendToServer(
                    PrimaryNetSwitchActionPacket(
                        PrimaryNetSwitchAction.CLEAR_PRIMARY,
                        DimensionsNet.NO_PRIMARY_NET_ID
                    )
                )

            PrimaryNetRequest.OpenTerminal -> {
                // 终端打开时回到上次的滚动位置
                UIDataHelper.isTransfer = true
                PacketDistributor.sendToServer(
                    OpenNetGuiPacket(
                        container.player.stringUUID,
                        NetMenuType.NET_CRAFT_MENU
                    )
                )
            }
        }
    }

    @Composable
    override fun Content(state: PrimaryNetState) {
        val colors = Bd.colors
        var query by remember { mutableStateOf("") }
        var renaming by remember { mutableStateOf<Int?>(null) }
        BdScreenFrame {
            val current = state.options.find { it.id == state.primary }
            BdTabbedWindow(
                Modifier.width((SIDE_RAIL_WIDTH + 230).dp),
                header = {
                    BdHeader(
                        text.icon,
                        text.title,
                        ::requestClose,
                        tag = current?.let { "#%04d".format(it.id) },
                    )
                },
                rail = { BdRailTab(text.title, BdGlyphs.Main, selected = true) {} },
            ) {
                BdMainPage(true) {
                    BdSearchField(query, { query = it }, text.search, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    BdSectionLabel(text.networks) {
                        OreText(
                            state.options.size.toString(),
                            color = colors.faint,
                            style = Bd.caption
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val shown =
                        state.options.filter {
                            query.isBlank() || it.name.contains(query, ignoreCase = true) || it.id.toString()
                                .contains(query.trim().removePrefix("#"))
                        }
                    ListWell {
                        if (shown.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().height(LIST_MIN_HEIGHT.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OreText(text.empty, color = colors.faint, style = Bd.caption)
                            }
                        } else {
                            BdScrollColumn(
                                Modifier.heightIn(min = LIST_MIN_HEIGHT.dp, max = 170.dp),
                                Arrangement.spacedBy(2.dp)
                            ) {
                                for (option in shown) {
                                    NetworkRow(
                                        option,
                                        option.id == state.primary,
                                        renaming == option.id,
                                        text,
                                        onSelect = { send(PrimaryNetRequest.Select(option.id)) },
                                        onRename = { renaming = option.id },
                                        onCommit = { name ->
                                            renaming = null
                                            if (name != null) send(PrimaryNetRequest.Rename(option.id, name))
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OreText(text.primary, color = colors.faint, style = Bd.caption, maxLines = 1)
                        Spacer(Modifier.width(3.dp))
                        OreText(
                            current?.name ?: text.none,
                            Modifier.weight(1f),
                            color = colors.text,
                            style = Bd.caption,
                            maxLines = 1
                        )
                        BdChip({ send(PrimaryNetRequest.Clear) }, enabled = current != null) {
                            OreText(text.clear, color = colors.muted, style = Bd.caption, maxLines = 1)
                        }
                        Spacer(Modifier.width(4.dp))
                        BdChip(
                            { send(PrimaryNetRequest.OpenTerminal) },
                            enabled = current != null,
                            active = current != null
                        ) {
                            OreText(text.open, color = colors.accentDeep, style = Bd.caption, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private class PrimaryNetText(val title: String) {
    val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_CONTROL.get()))
    val networks = tr("ui.beyonddimensions.primary.networks")
    val search = tr("ui.beyonddimensions.primary.search")
    val primary = tr("ui.beyonddimensions.primary.current")
    val none = tr("ui.beyonddimensions.primary.none")
    val empty = tr("ui.beyonddimensions.primary.empty")
    val rename = tr("ui.beyonddimensions.primary.rename")
    val clear = tr("ui.beyonddimensions.primary.clear")
    val open = tr("ui.beyonddimensions.primary.open")
    val roles = NetPermissionlevel.entries.associateWith(::roleLabel)
}

@Composable
private fun NetworkRow(
    option: NetOptionView,
    selected: Boolean,
    renaming: Boolean,
    text: PrimaryNetText,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onCommit: (String?) -> Unit,
) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth()
            .height(20.dp)
            .hoverable(interaction)
            .bdClickable(interaction, enabled = !selected && !renaming, onClick = onSelect)
            .background(if (selected || hovered) colors.accentSoft else colors.surface)
            .border(1.dp, if (selected) colors.accent else colors.line)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(4.dp).background(if (selected) colors.accent else colors.line))
        Spacer(Modifier.width(5.dp))
        if (renaming) {
            RenameField(option.name, onCommit)
        } else {
            OreText(option.name, Modifier.weight(1f), color = colors.text, maxLines = 1)
            OreText("#%04d".format(option.id), color = colors.faint, style = Bd.caption, maxLines = 1)
            Spacer(Modifier.width(4.dp))
            RoleChip(option.role, text.roles.getValue(option.role))
            if (option.renamable) {
                Spacer(Modifier.width(2.dp))
                BdGlyphButton(OreGlyph.Pencil, text.rename, onRename, size = 13.dp, glyphSize = 7.dp)
            }
        }
    }
}

@Composable
private fun RowScope.RenameField(initial: String, onCommit: (String?) -> Unit) {
    val colors = Bd.colors
    var value by remember { mutableStateOf(initial) }
    BasicTextField(
        value,
        { value = it.take(DimensionsNet.MAX_NETWORK_NAME_LENGTH) },
        Modifier.weight(1f).height(14.dp),
        singleLine = true,
        textStyle = Bd.body.copy(color = colors.text),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onCommit(value) }),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxSize().background(colors.surface).border(1.dp, colors.accent)
                    .padding(horizontal = 3.dp), contentAlignment = Alignment.CenterStart
            ) {
                inner()
            }
        },
    )
    Spacer(Modifier.width(3.dp))
    BdGlyphButton(OreGlyph.Checkmark, null, { onCommit(value) }, size = 13.dp, glyphSize = 7.dp)
    BdGlyphButton(OreGlyph.Cross, null, { onCommit(null) }, size = 13.dp, glyphSize = 7.dp)
}
