package com.wintercogs.beyonddimensions.client.ui.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet
import com.wintercogs.beyonddimensions.api.dimensionnet.NetControlAction
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetSwitchAction
import com.wintercogs.beyonddimensions.client.gui.NetMenuType
import com.wintercogs.beyonddimensions.client.ui.base.BdController
import com.wintercogs.beyonddimensions.client.ui.base.BdMenuScreen
import com.wintercogs.beyonddimensions.client.ui.base.LocalBdScreen
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.BdChip
import com.wintercogs.beyonddimensions.client.ui.kit.BdGlyphButton
import com.wintercogs.beyonddimensions.client.ui.kit.BdHeader
import com.wintercogs.beyonddimensions.client.ui.kit.BdSearchField
import com.wintercogs.beyonddimensions.client.ui.kit.BdSectionLabel
import com.wintercogs.beyonddimensions.client.ui.kit.BdStatus
import com.wintercogs.beyonddimensions.client.ui.kit.BdTone
import com.wintercogs.beyonddimensions.client.ui.kit.BdWindow
import com.wintercogs.beyonddimensions.client.ui.kit.bdClickable
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.menu.NetControlMenu
import com.wintercogs.beyonddimensions.common.menu.PrimaryNetSwitcherMenu
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.PrimaryNetSwitchActionPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.RenameNetPacket
import com.wintercogs.beyonddimensions.util.UIDataHelper
import dev.compixel.forge.item.ItemIcon
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.overlay.OreTooltip
import dev.compixel.ui.ore.overlay.OreTooltipMode
import dev.compixel.ui.ore.scroll.OreScrollbar
import java.util.UUID
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec2
import net.neoforged.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW

private fun roleLabel(role: NetPermissionlevel) = tr("ui.beyonddimensions.network.role.${role.name.lowercase()}")

/** 角色徽章：所有者用强调色，管理员用浅色，成员为中性色 */
@Composable
private fun RoleChip(role: NetPermissionlevel, label: String) {
    val colors = Bd.colors
    val (ink, fill) =
        when (role) {
            NetPermissionlevel.Owner -> colors.surface to colors.accentDeep
            NetPermissionlevel.Manager -> colors.accentDeep to colors.accentSoft
            NetPermissionlevel.Member -> colors.muted to colors.sunken
        }
    Box(Modifier.background(fill, Bd.ChipShape).padding(horizontal = 4.dp, vertical = 1.dp)) {
        OreText(label, color = ink, style = Bd.caption, maxLines = 1)
    }
}

/** 可滚动的列表区域；内容超出时右侧出现滚动条 */
@Composable
private fun ScrollList(maxHeight: Int, content: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxWidth().heightIn(max = maxHeight.dp)) {
        Column(Modifier.padding(end = if (scroll.maxValue > 0) 8.dp else 0.dp).verticalScroll(scroll)) { content() }
        if (scroll.maxValue > 0) OreScrollbar(scroll, Modifier.align(Alignment.TopEnd).width(5.dp).fillMaxHeight())
    }
}

// ---- 网络成员与权限 ----

data class MemberView(val id: UUID, val name: String, val role: NetPermissionlevel, val actions: Set<NetControlAction>, val self: Boolean)

data class NetControlState(val ready: Boolean = false, val networkId: Int = -1, val networkName: String = "", val members: List<MemberView> = emptyList())

sealed interface NetControlRequest {
    data class Apply(val member: UUID, val action: NetControlAction, val role: NetPermissionlevel) : NetControlRequest

    data object Refresh : NetControlRequest
}

class NetControlController(private val menu: NetControlMenu) : BdController<NetControlState, NetControlRequest> {
    private var members = emptyList<NetControlMenu.Member>()
    private var views = emptyList<MemberView>()

    override fun snapshot(): NetControlState {
        val current = menu.members()
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
            menu.ready(),
            menu.networkId(),
            DimensionsNet.getNetworkName(menu.networkId(), menu.networkName()).string,
            views,
        )
    }

    override fun handle(action: NetControlRequest) {
        when (action) {
            is NetControlRequest.Apply -> menu.request(NetControlMenu.Request(action.member, action.action, action.role))
            NetControlRequest.Refresh -> menu.refresh()
        }
    }
}

class NetControlScreen(menu: NetControlMenu, inventory: Inventory, title: Component) :
    BdMenuScreen<NetControlMenu, NetControlState, NetControlRequest>(menu, title) {
    private val controller = NetControlController(menu)
    private val text = NetControlText(title.string)

    override fun snapshot() = controller.snapshot()

    override fun handle(action: NetControlRequest) = controller.handle(action)

    @Composable override fun Page(state: NetControlState) = NetControlView(state, ::send, text)
}

class NetControlText(val title: String) {
    val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_CONTROL.get()))
    val brand = "BEYOND DIMENSIONS"
    val syncing = tr("ui.beyonddimensions.status.syncing")
    val members = tr("ui.beyonddimensions.network.members")
    val refresh = tr("ui.beyonddimensions.network.refresh")
    val empty = tr("ui.beyonddimensions.network.empty")
    val roles = NetPermissionlevel.entries.associateWith(::roleLabel)
    val actions = NetControlAction.entries.associateWith { tr("ui.beyonddimensions.network.action.${it.name.lowercase()}") }
    val actionHints = NetControlAction.entries.associateWith { tr("ui.beyonddimensions.network.action.${it.name.lowercase()}.hint") }
    val leave = tr("ui.beyonddimensions.network.action.leave")
    val leaveHint = tr("ui.beyonddimensions.network.action.leave.hint")
    val you = tr("ui.beyonddimensions.network.you")
}

@Composable
private fun NetControlView(state: NetControlState, send: (NetControlRequest) -> Unit, text: NetControlText) {
    val colors = Bd.colors
    val screen = LocalBdScreen.current
    Box(Modifier.fillMaxSize().background(Color(0x400A1423)), contentAlignment = Alignment.Center) {
        BdWindow(Modifier.width(250.dp)) {
            BdHeader(
                text.icon,
                text.brand,
                if (state.networkId >= 0) state.networkName else text.title,
                if (state.networkId >= 0) "#%04d".format(state.networkId) else null,
                if (state.ready) null else BdStatus(text.syncing, BdTone.Warning),
                screen::close,
            )
            Column(Modifier.padding(8.dp)) {
                BdSectionLabel(text.members) {
                    OreText(state.members.size.toString(), color = colors.faint, style = Bd.caption)
                    Spacer(Modifier.width(3.dp))
                    BdGlyphButton(OreGlyph.CycleArrows, text.refresh, { send(NetControlRequest.Refresh) }, size = 11.dp, glyphSize = 7.dp)
                }
                Spacer(Modifier.height(5.dp))
                if (state.members.isEmpty()) {
                    OreText(text.empty, color = colors.faint, style = Bd.caption)
                } else {
                    ScrollList(200) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (member in state.members) MemberRow(member, text) { send(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: MemberView, text: NetControlText, send: (NetControlRequest) -> Unit) {
    val colors = Bd.colors
    Row(
        Modifier.fillMaxWidth().height(20.dp).background(colors.surface).border(1.dp, colors.line).padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoleChip(member.role, text.roles.getValue(member.role))
        Spacer(Modifier.width(5.dp))
        OreText(member.name, Modifier.weight(1f, fill = false), color = colors.text, maxLines = 1)
        if (member.self) {
            Spacer(Modifier.width(3.dp))
            OreText(text.you, color = colors.faint, style = Bd.caption, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        for (action in NetControlAction.entries) {
            if (action !in member.actions) continue
            Spacer(Modifier.width(3.dp))
            // 对自己执行“移除”就是退出网络
            val leaving = member.self && action == NetControlAction.RemovePlayer
            val label = if (leaving) text.leave else text.actions.getValue(action)
            val hint = if (leaving) text.leaveHint else text.actionHints.getValue(action)
            ActionButton(label, hint, danger = action == NetControlAction.RemovePlayer) {
                send(NetControlRequest.Apply(member.id, action, member.role))
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, hint: String, danger: Boolean, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val ink = if (danger) colors.danger else colors.accentDeep
    OreTooltip(hint, mode = OreTooltipMode.Immediate) {
        Box(
            Modifier.height(13.dp)
                .hoverable(interaction)
                .bdClickable(interaction, onClick = onClick)
                .background(if (hovered) ink else Color.Transparent, Bd.ChipShape)
                .border(1.dp, ink.copy(alpha = if (hovered) 1f else 0.45f), Bd.ChipShape)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            OreText(label, color = if (hovered) colors.surface else ink, style = Bd.caption, maxLines = 1)
        }
    }
}

// ---- 主网络切换 ----

data class NetOptionView(val id: Int, val name: String, val role: NetPermissionlevel, val renamable: Boolean)

data class PrimaryNetState(val ready: Boolean = false, val primary: Int = DimensionsNet.NO_PRIMARY_NET_ID, val options: List<NetOptionView> = emptyList())

sealed interface PrimaryNetRequest {
    data class Select(val id: Int) : PrimaryNetRequest

    data class Rename(val id: Int, val name: String) : PrimaryNetRequest

    data object Clear : PrimaryNetRequest

    data object OpenTerminal : PrimaryNetRequest
}

class PrimaryNetController(private val menu: PrimaryNetSwitcherMenu) : BdController<PrimaryNetState, PrimaryNetRequest> {
    override fun snapshot() =
        PrimaryNetState(
            menu.menuSync().hasSnapshot(),
            menu.currentPrimaryNetId,
            menu.options.map {
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
            is PrimaryNetRequest.Select ->
                if (menu.options.any { it.netId() == action.id })
                    PacketDistributor.sendToServer(PrimaryNetSwitchActionPacket(PrimaryNetSwitchAction.SET_EXPLICIT, action.id))
            is PrimaryNetRequest.Rename -> {
                val option = menu.options.find { it.netId() == action.id } ?: return
                if (option.permission() == NetPermissionlevel.Member) return
                val name = action.name.codePoints().limit(DimensionsNet.MAX_NETWORK_NAME_LENGTH.toLong()).toArray().let { String(it, 0, it.size) }
                PacketDistributor.sendToServer(RenameNetPacket(action.id, name))
            }
            PrimaryNetRequest.Clear ->
                PacketDistributor.sendToServer(PrimaryNetSwitchActionPacket(PrimaryNetSwitchAction.CLEAR_PRIMARY, DimensionsNet.NO_PRIMARY_NET_ID))
            PrimaryNetRequest.OpenTerminal -> {
                if (menu.currentPrimaryNetId == DimensionsNet.NO_PRIMARY_NET_ID) return
                val x = DoubleArray(1)
                val y = DoubleArray(1)
                GLFW.glfwGetCursorPos(Minecraft.getInstance().window.window, x, y)
                UIDataHelper.lastMousePos = Vec2(x[0].toFloat(), y[0].toFloat())
                UIDataHelper.isTransfer = true
                PacketDistributor.sendToServer(OpenNetGuiPacket(menu.player.stringUUID, NetMenuType.NET_CRAFT_MENU))
            }
        }
    }
}

class PrimaryNetScreen(menu: PrimaryNetSwitcherMenu, inventory: Inventory, title: Component) :
    BdMenuScreen<PrimaryNetSwitcherMenu, PrimaryNetState, PrimaryNetRequest>(menu, title) {
    private val controller = PrimaryNetController(menu)
    private val text = PrimaryNetText(title.string)

    override fun snapshot() = controller.snapshot()

    override fun handle(action: PrimaryNetRequest) = controller.handle(action)

    @Composable override fun Page(state: PrimaryNetState) = PrimaryNetView(state, ::send, text)
}

class PrimaryNetText(val title: String) {
    val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_CONTROL.get()))
    val brand = "BEYOND DIMENSIONS"
    val syncing = tr("ui.beyonddimensions.status.syncing")
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
private fun PrimaryNetView(state: PrimaryNetState, send: (PrimaryNetRequest) -> Unit, text: PrimaryNetText) {
    val colors = Bd.colors
    val screen = LocalBdScreen.current
    var query by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<Int?>(null) }
    Box(Modifier.fillMaxSize().background(Color(0x400A1423)), contentAlignment = Alignment.Center) {
        BdWindow(Modifier.width(230.dp)) {
            val current = state.options.find { it.id == state.primary }
            BdHeader(
                text.icon,
                text.brand,
                text.title,
                current?.let { "#%04d".format(it.id) },
                if (state.ready) null else BdStatus(text.syncing, BdTone.Warning),
                screen::close,
            )
            Column(Modifier.padding(8.dp)) {
                BdSearchField(query, { query = it }, text.search, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                BdSectionLabel(text.networks) { OreText(state.options.size.toString(), color = colors.faint, style = Bd.caption) }
                Spacer(Modifier.height(4.dp))
                val shown =
                    state.options.filter {
                        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.id.toString().contains(query.trim().removePrefix("#"))
                    }
                if (shown.isEmpty()) {
                    OreText(text.empty, color = colors.faint, style = Bd.caption)
                } else {
                    ScrollList(170) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    OreText(current?.name ?: text.none, Modifier.weight(1f), color = colors.text, style = Bd.caption, maxLines = 1)
                    BdChip({ send(PrimaryNetRequest.Clear) }, enabled = current != null) {
                        OreText(text.clear, color = colors.muted, style = Bd.caption, maxLines = 1)
                    }
                    Spacer(Modifier.width(4.dp))
                    BdChip({ send(PrimaryNetRequest.OpenTerminal) }, enabled = current != null, active = current != null) {
                        OreText(text.open, color = colors.accentDeep, style = Bd.caption, maxLines = 1)
                    }
                }
            }
        }
    }
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
private fun androidx.compose.foundation.layout.RowScope.RenameField(initial: String, onCommit: (String?) -> Unit) {
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
            Box(Modifier.fillMaxSize().background(colors.surface).border(1.dp, colors.accent).padding(horizontal = 3.dp), contentAlignment = Alignment.CenterStart) {
                inner()
            }
        },
    )
    Spacer(Modifier.width(3.dp))
    BdGlyphButton(OreGlyph.Checkmark, null, { onCommit(value) }, size = 13.dp, glyphSize = 7.dp)
    BdGlyphButton(OreGlyph.Cross, null, { onCommit(null) }, size = 13.dp, glyphSize = 7.dp)
}
