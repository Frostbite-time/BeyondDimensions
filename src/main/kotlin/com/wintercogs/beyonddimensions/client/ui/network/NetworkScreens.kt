package com.wintercogs.beyonddimensions.client.ui.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet
import com.wintercogs.beyonddimensions.api.dimensionnet.NetControlAction
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel
import com.wintercogs.beyonddimensions.api.dimensionnet.PlayerPermissionInfo
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetSwitchAction
import com.wintercogs.beyonddimensions.client.gui.NetMenuType
import com.wintercogs.beyonddimensions.client.init.BDShortKeys
import com.wintercogs.beyonddimensions.client.ui.base.BdController
import com.wintercogs.beyonddimensions.client.ui.base.BdIcons
import com.wintercogs.beyonddimensions.client.ui.base.BdMenuScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdThemes
import com.wintercogs.beyonddimensions.client.ui.base.at
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.common.menu.NetControlMenu
import com.wintercogs.beyonddimensions.common.menu.PrimaryNetSwitcherMenu
import com.wintercogs.beyonddimensions.network.packet.c2s.NetControlActionPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.PrimaryNetSwitchActionPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.RenameNetPacket
import com.wintercogs.beyonddimensions.util.UIDataHelper
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.host.UiBinding
import dev.compixel.ui.ore.button.OreButton
import dev.compixel.ui.ore.button.OreButtonStyle
import dev.compixel.ui.ore.button.OreIconButton
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.input.OreTextField
import dev.compixel.ui.ore.layout.OrePanel
import dev.compixel.ui.ore.layout.OreSurface
import dev.compixel.ui.ore.overlay.OreTooltip
import dev.compixel.ui.ore.scroll.OreScrollbar
import dev.compixel.ui.ore.theme.OreTheme
import java.util.Locale
import java.util.UUID
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.phys.Vec2
import net.neoforged.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW

/* 网络控制与主网络切换：没有槽位的菜单界面，布局与操作沿用旧版 */

/** 旧版的原版风格按钮：灰底白字；放不下的文字不换行，从右侧截断。不可用时是原版的黑框深灰底 */
@Composable
private fun BdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    padding: PaddingValues = PaddingValues(horizontal = 4.dp),
) {
    val button = @Composable {
        OreButton(onClick, modifier, enabled, OreButtonStyle.Secondary, contentPadding = padding) {
            OreText(text, Modifier.clipToBounds().wrapContentWidth(Alignment.Start, unbounded = true), maxLines = 1)
        }
    }
    if (enabled) button() else OreTheme(id = BdThemes.Disabled) { button() }
}

// 网络控制

data class Member(val id: UUID, val name: String)

data class ControlState(val members: List<Member>, val selected: UUID?, val level: String, val name: String)

sealed interface ControlAction {
    data class Select(val id: UUID) : ControlAction

    data class Act(val action: NetControlAction) : ControlAction
}

/** 在游戏线程读取成员与权限；选中的成员只保存在界面这一侧，操作经旧版的数据包交给服务端校验 */
class ControlController(private val menu: NetControlMenu) :
    BdController<ControlState, ControlAction>(ControlState(emptyList(), null, "", "")) {
    private var selected: UUID? = null

    init {
        ui.update(snapshot())
    }

    override fun snapshot(): ControlState {
        val info = selected?.let { menu.playerInfo[it] }
        if (info == null) selected = null
        return ControlState(
            menu.playerInfo.entries
                .sortedWith(compareBy<Map.Entry<UUID, PlayerPermissionInfo>>({ it.value.level() }, { it.value.name() }))
                .map { Member(it.key, it.value.name()) },
            selected,
            if (info == null) tr("menu.text.beyonddimensions.permission.level.zero")
            else tr("menu.text.beyonddimensions.permission.level.prefix", info.level().name),
            tr("menu.text.beyonddimensions.name.player", info?.name() ?: ""),
        )
    }

    override fun handle(action: ControlAction) {
        when (action) {
            is ControlAction.Select -> selected = action.id
            is ControlAction.Act -> selected?.let { PacketDistributor.sendToServer(NetControlActionPacket(it, action.action)) }
        }
    }
}

class ControlLabels(val title: String, val actions: List<Pair<String, NetControlAction>>)

/** 网络控制：左侧是成员列表，右侧是选中成员的权限、名称与四个操作按钮 */
class ControlScreen
private constructor(menu: NetControlMenu, title: Component, controller: ControlController, labels: ControlLabels) :
    BdMenuScreen<NetControlMenu>(menu, title, controller, { ControlView(controller.ui, labels) }) {
    constructor(
        menu: NetControlMenu,
        @Suppress("UNUSED_PARAMETER") inventory: Inventory,
        title: Component,
    ) : this(
        menu,
        title,
        ControlController(menu),
        ControlLabels(
            title.string,
            listOf(
                tr("menu.button.beyonddimensions.setowner") to NetControlAction.SetOwner,
                tr("menu.button.beyonddimensions.setmanager") to NetControlAction.SetManager,
                tr("menu.button.beyonddimensions.removemanager") to NetControlAction.RemoveManager,
                tr("menu.button.beyonddimensions.removemember") to NetControlAction.RemovePlayer,
            ),
        ),
    )
}

@Composable
private fun ControlView(binding: UiBinding<ControlState, ControlAction>, labels: ControlLabels) {
    val state = binding.value
    Box(Modifier.fillMaxSize().background(OreTheme.colors.backdrop), contentAlignment = Alignment.Center) {
        // 旧版贴图实际绘制 256×233
        OrePanel("", Modifier.size(256.dp, 233.dp), showTitleBar = false, contentPadding = PaddingValues(0.dp)) {
            Box(Modifier.fillMaxSize()) {
                OreText(labels.title, Modifier.at(11, 6), maxLines = 1)
                // 成员列表：旧版每行一个 10 像素高的按钮，按权限再按名称排序；列表框的内容从边框内 1 像素开始
                OreTheme(id = BdThemes.List) {
                    OreSurface(Modifier.at(9, 16).size(88.dp, 210.dp)) {
                        LazyColumn(Modifier.fillMaxSize().padding(1.dp)) {
                            items(state.members, key = { it.id }) { member ->
                                // 行内按钮恢复界面配色；选中的成员如同旧版获得焦点的按钮
                                OreTheme(id = if (member.id == state.selected) BdThemes.Selected else BdThemes.Screen) {
                                    BdButton(
                                        member.name,
                                        { binding.send(ControlAction.Select(member.id)) },
                                        Modifier.fillMaxWidth().height(10.dp),
                                        padding = PaddingValues(horizontal = 1.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                OreText(state.level, Modifier.at(110, 10), maxLines = 1)
                OreText(state.name, Modifier.at(110, 25), maxLines = 1)
                for ((index, action) in labels.actions.withIndex())
                    BdButton(
                        action.first,
                        { binding.send(ControlAction.Act(action.second)) },
                        Modifier.at(110, 60 + index * 25).size(100.dp, 20.dp),
                    )
            }
        }
    }
}

// 主网络切换

/** 一个可选网络：[searchable] 是小写的名称、编号与权限，供搜索匹配 */
data class NetOption(
    val id: Int,
    val label: String,
    val tooltip: String,
    val searchable: String,
    val renamable: Boolean,
    val current: Boolean,
    val customName: String,
    val defaultName: String,
)

data class SwitcherState(
    val current: String,
    val hasPrimary: Boolean,
    val options: List<NetOption>,
    /** 最近用过且仍可选的网络：编号与提示 */
    val recent: List<Pair<Int, String>>,
)

sealed interface SwitcherAction {
    data class SetPrimary(val id: Int) : SwitcherAction

    data object ClearPrimary : SwitcherAction

    data object OpenNet : SwitcherAction

    data class Rename(val id: Int, val name: String) : SwitcherAction
}

/** 在游戏线程整理可选网络与最近使用的网络；切换、清除与重命名经旧版的数据包交给服务端 */
class SwitcherController(private val menu: PrimaryNetSwitcherMenu) :
    BdController<SwitcherState, SwitcherAction>(SwitcherState("", false, emptyList(), emptyList())) {
    private var observedPrimary = menu.currentPrimaryNetId

    init {
        // 从维度网络界面切换过来时，与旧版一样把鼠标放回原处
        if (UIDataHelper.isTransfer) {
            UIDataHelper.lastMousePos?.let { GLFW.glfwSetCursorPos(window(), it.x.toDouble(), it.y.toDouble()) }
            UIDataHelper.isTransfer = false
        }
        ui.update(snapshot())
    }

    override fun snapshot(): SwitcherState {
        val current = menu.currentPrimaryNetId
        if (current != observedPrimary) {
            observedPrimary = current
            remember(current)
        }
        val ids = menu.options.map { it.netId() }.toSet()
        return SwitcherState(
            tr(
                "menu.text.beyonddimensions.primary_net_switcher.current",
                if (current == DimensionsNet.NO_PRIMARY_NET_ID) tr("menu.text.beyonddimensions.primary_net_switcher.none")
                else "#$current",
            ),
            current != DimensionsNet.NO_PRIMARY_NET_ID,
            menu.options.map { option ->
                val permission =
                    tr("menu.text.beyonddimensions.primary_net_switcher.permission." + option.permission().name.lowercase(Locale.ROOT))
                val text = "${option.networkName.string} (#${option.netId()}) $permission"
                val isCurrent = option.netId() == current
                NetOption(
                    option.netId(),
                    if (isCurrent) text + tr("menu.text.beyonddimensions.primary_net_switcher.current_suffix") else text,
                    tr("tooltip.button.beyonddimensions.primary_net_switcher.option", option.netId(), permission),
                    text.lowercase(Locale.ROOT),
                    option.permission() == NetPermissionlevel.Owner || option.permission() == NetPermissionlevel.Manager,
                    isCurrent,
                    option.customName(),
                    DimensionsNet.getNetworkName(option.netId(), "").string,
                )
            },
            RECENT.filter { it in ids }.take(3).map { it to tr("tooltip.button.beyonddimensions.primary_net_switcher.recent", it) },
        )
    }

    override fun handle(action: SwitcherAction) {
        when (action) {
            is SwitcherAction.SetPrimary ->
                PacketDistributor.sendToServer(PrimaryNetSwitchActionPacket(PrimaryNetSwitchAction.SET_EXPLICIT, action.id))
            SwitcherAction.ClearPrimary ->
                PacketDistributor.sendToServer(
                    PrimaryNetSwitchActionPacket(PrimaryNetSwitchAction.CLEAR_PRIMARY, DimensionsNet.NO_PRIMARY_NET_ID)
                )
            SwitcherAction.OpenNet ->
                if (menu.currentPrimaryNetId != DimensionsNet.NO_PRIMARY_NET_ID) {
                    val x = DoubleArray(1)
                    val y = DoubleArray(1)
                    GLFW.glfwGetCursorPos(window(), x, y)
                    UIDataHelper.lastMousePos = Vec2(x[0].toFloat(), y[0].toFloat())
                    UIDataHelper.isTransfer = true
                    PacketDistributor.sendToServer(OpenNetGuiPacket(menu.player.stringUUID, NetMenuType.NET_CRAFT_MENU))
                }
            is SwitcherAction.Rename -> PacketDistributor.sendToServer(RenameNetPacket(action.id, action.name))
        }
    }

    private fun window() = Minecraft.getInstance().window.window

    private companion object {
        /** 与旧版一样在本次游戏中记住最近成为主网络的网络，最多 8 个 */
        val RECENT = ArrayDeque<Int>()

        fun remember(id: Int) {
            if (id < 0) return
            RECENT.remove(id)
            RECENT.addFirst(id)
            while (RECENT.size > 8) RECENT.removeLast()
        }
    }
}

class SwitcherLabels(
    val title: String,
    val search: String,
    val searchTooltip: String,
    val clear: String,
    val clearTooltip: String,
    val recent: String,
    val networks: String,
    val openNet: String,
    val openNetIcon: ItemIcon,
)

/**
 * 主网络切换：搜索框、当前主网络、清除按钮、最近使用的网络与全部网络列表。
 * 右键列表中自己管理的网络可以重命名，回车确认、Esc 取消；右上角的按钮打开主网络的维度网络界面。
 */
class PrimaryScreen
private constructor(menu: PrimaryNetSwitcherMenu, title: Component, controller: SwitcherController, labels: SwitcherLabels) :
    BdMenuScreen<PrimaryNetSwitcherMenu>(menu, title, controller, { SwitcherView(controller.ui, labels) }) {
    constructor(
        menu: PrimaryNetSwitcherMenu,
        @Suppress("UNUSED_PARAMETER") inventory: Inventory,
        title: Component,
    ) : this(
        menu,
        title,
        SwitcherController(menu),
        SwitcherLabels(
            title.string,
            tr("menu.label.beyonddimensions.primary_net_switcher.search"),
            tr("tooltip.editbox.beyonddimensions.primary_net_switcher.search"),
            tr("menu.button.beyonddimensions.primary_net_switcher.clear"),
            tr("tooltip.button.beyonddimensions.primary_net_switcher.clear"),
            tr("menu.label.beyonddimensions.primary_net_switcher.recent"),
            tr("menu.label.beyonddimensions.primary_net_switcher.all_networks"),
            tr("tooltip.button.beyonddimensions.open_dimensions_net_menu"),
            BdIcons.sprite("opposite_arrow"),
        ),
    )

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // 与旧版相同：不在输入文字时，再按一次打开切换界面的按键就关闭界面
        if (!hasTextInputFocus && BDShortKeys.OPEN_PRIMARY_NET_SWITCHER_KEY.matches(keyCode, scanCode)) {
            onClose()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SwitcherView(binding: UiBinding<SwitcherState, SwitcherAction>, labels: SwitcherLabels) {
    val state = binding.value
    var search by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<Int?>(null) }
    val query = search.trim().lowercase(Locale.ROOT)
    val shown = if (query.isEmpty()) state.options else state.options.filter { query in it.searchable }
    val list = rememberLazyListState()
    Box(Modifier.fillMaxSize().background(OreTheme.colors.backdrop), contentAlignment = Alignment.Center) {
        // Ore 输入框比原版高 6 像素，搜索框以下的内容与面板一起下移 6
        OrePanel("", Modifier.size(176.dp, (239 + SHIFT).dp), showTitleBar = false, contentPadding = PaddingValues(0.dp)) {
            Box(Modifier.fillMaxSize()) {
                OreText(labels.title, Modifier.at(8, 8), maxLines = 1)
                OreTheme(id = BdThemes.Sidebar) {
                    // 图标四周本身留有透明边，按旧版 16×16 的大小盖住整个按钮
                    OreIconButton(
                        labels.openNet,
                        { binding.send(SwitcherAction.OpenNet) },
                        Modifier.at(152, 4).size(16.dp),
                        state.hasPrimary,
                    ) {
                        MinecraftItemIcon(labels.openNetIcon, Modifier.requiredSize(16.dp))
                    }
                }
                OreTheme(id = BdThemes.Field) {
                    OreTooltip(labels.searchTooltip, Modifier.at(8, 20)) {
                        Box(
                            Modifier.width(160.dp).onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) {
                                // 右键清空搜索框，与旧版相同
                                if (it.buttons.isSecondaryPressed) search = ""
                            }
                        ) {
                            OreTextField(search, { search = it }, placeholder = labels.search)
                        }
                    }
                }
                OreText(state.current, Modifier.at(8, 37 + SHIFT), maxLines = 1)
                OreTooltip(labels.clearTooltip, Modifier.at(8, 47 + SHIFT)) {
                    BdButton(labels.clear, { binding.send(SwitcherAction.ClearPrimary) }, Modifier.size(160.dp, 20.dp))
                }
                OreText(labels.recent, Modifier.at(8, 69 + SHIFT), maxLines = 1)
                for ((index, recent) in state.recent.withIndex())
                    OreTooltip(recent.second, Modifier.at(8 + index * 54, 78 + SHIFT)) {
                        BdButton("#${recent.first}", { binding.send(SwitcherAction.SetPrimary(recent.first)) }, Modifier.size(50.dp, 16.dp))
                    }
                OreText(labels.networks, Modifier.at(8, 96 + SHIFT), maxLines = 1)
                // 网络列表一次显示 6 行；当前主网络不可再次选择，右键自己管理的网络可以重命名
                LazyColumn(Modifier.at(8, 107 + SHIFT).size(140.dp, 120.dp), state = list) {
                    items(shown, key = { it.id }) { option ->
                        if (renaming == option.id)
                            RenameField(
                                option,
                                { name ->
                                    binding.send(SwitcherAction.Rename(option.id, name))
                                    renaming = null
                                },
                                { renaming = null },
                            )
                        else
                            OreTooltip(option.tooltip) {
                                BdButton(
                                    option.label,
                                    { binding.send(SwitcherAction.SetPrimary(option.id)) },
                                    Modifier.size(140.dp, 20.dp).onPointerEvent(PointerEventType.Press) {
                                        if (it.buttons.isSecondaryPressed && option.renamable) renaming = option.id
                                    },
                                    enabled = !option.current,
                                )
                            }
                    }
                }
                // 与存储界面相同的 Ore 滚动条，放在旧版滑块所在的一列（轨道内 12 宽、120 高）
                OreTheme(id = BdThemes.Scroller) { OreScrollbar(list, Modifier.at(160, 108 + SHIFT).size(12.dp, 120.dp)) }
            }
        }
    }
}

private const val SHIFT = 6

/** 列表行内的重命名输入框：回车提交，Esc 取消；留空时显示网络的默认名称 */
@Composable
private fun RenameField(option: NetOption, onDone: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember(option.id) { mutableStateOf(option.customName) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    OreTheme(id = BdThemes.Field) {
        OreTextField(
            text,
            { if (it.length <= DimensionsNet.MAX_NETWORK_NAME_LENGTH) text = it },
            Modifier.size(140.dp, 20.dp).focusRequester(focus).onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false
                else
                    when (event.key) {
                        Key.Enter,
                        Key.NumPadEnter -> {
                            onDone(text)
                            true
                        }
                        Key.Escape -> {
                            onCancel()
                            true
                        }
                        else -> false
                    }
            },
            placeholder = option.defaultName,
        )
    }
}
