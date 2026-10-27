package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.ui.storage.*
import com.wintercogs.beyonddimensions.client.ui.base.BdSlot
import com.wintercogs.beyonddimensions.client.ui.base.BdSlotGrid
import com.wintercogs.beyonddimensions.client.ui.base.SLOT_PITCH
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import com.wintercogs.beyonddimensions.common.menu.widget.AnvilWidget
import com.wintercogs.beyonddimensions.common.menu.widget.SmithingWidget
import com.wintercogs.beyonddimensions.common.menu.widget.StonecutterWidget
import com.wintercogs.beyonddimensions.config.ClientConfigRuntime
import com.wintercogs.beyonddimensions.integration.ModPresence
import com.wintercogs.beyonddimensions.integration.OtherModIds
import com.wintercogs.beyonddimensions.integration.module.polymorph.ComposeRecipeChoices
import dev.compixel.forge.drawing.NativeRefresh
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.ui.ore.display.OreText
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SmithingTemplateItem

/* BD 自带的工作台：合成台、铁砧、锻造台与切石机，在 client.init 的 BDWorkbenches 里注册 */

/** 产物槽连同强调色细框与留白的边长 */
private const val RESULT_BOX = SLOT_PITCH + 4

/** 槽位外框每边占半个像素，合起来多出的宽度 */
private const val FRAME = 1

/** 两个槽位之间的箭头连同两侧的间距 */
private const val ARROW = 7 + 7 + 7

/** 一行小字的高度 */
private const val CAPTION_HEIGHT = 10

/** 工作台的偏好由服务器保存在玩家身上，关闭界面时按它退回或保留工作台里的物品 */
internal fun sendWorkbenchPreference(menu: DimensionsNetMenu) =
    menu.commands().preference(ClientConfigRuntime.craftingReturnToStorage, ClientConfigRuntime.craftingKeepGrid)

@Composable
private fun Arrow() {
    Spacer(Modifier.width(7.dp))
    BdIcon(BdIcons.ArrowRight, color = Bd.colors[BdColors.accent])
    Spacer(Modifier.width(7.dp))
}

/** 产物槽：外面一圈强调色细框 */
@Composable
private fun ResultSlot(scope: StorageWorkbenchScope<*>, id: Int) {
    Box(Modifier.border(1.dp, Bd.colors[BdColors.accent]).padding(2.dp)) { BdSlot(scope.slots, id) }
}

// —— 合成台 ——

/** Polymorph 给出的一个候选配方 */
@Immutable
internal data class RecipeChoice(val id: String, val label: String)

internal class CraftingText {
    val recipe = tr("ui.beyonddimensions.crafting.recipe")
    val clear = WorkbenchClearText()
}

@Immutable
internal data class CraftingState(
    val grid: List<Int>,
    val result: Int,
    /** 多个配方可选时由 Polymorph 提供的候选 */
    val recipeChoices: List<RecipeChoice>,
    val text: CraftingText,
)

internal sealed interface CraftingAction {
    /** 清空合成格，物品退回存储或背包 */
    data class Clear(val toStorage: Boolean) : CraftingAction

    data class ChooseRecipe(val id: String) : CraftingAction
}

/** 合成台：合成格与产物，居中；清空与退回方向按钮在下拉框一行的右端，Polymorph 给出多个配方时配方选择也在那里 */
internal object CraftingWorkbench : StorageWorkbench<CraftingState, CraftingAction> {
    override fun title(context: StorageWorkbenchContext): Component =
        Component.translatable("ui.beyonddimensions.storage.workbench.crafting")

    // 与 9 列存储格的内容区一样宽，工作区不随可用的工作台变窄；合成格的外框每边占半个像素
    override fun size(context: StorageWorkbenchContext) =
        StorageWorkbenchSize(StorageWorkbenches.MAX_WIDTH, 3 * SLOT_PITCH + FRAME)

    private fun recipeChoices(context: StorageWorkbenchContext) =
        context.remember("recipe_choices") { if (ModPresence.isLoaded(OtherModIds.POLYMORPH)) ComposeRecipeChoices() else null }

    override fun snapshot(context: StorageWorkbenchContext): CraftingState {
        val crafting = context.menu.crafting()
        return CraftingState(
            grid = (crafting.gridStart() until crafting.gridEnd()).toList(),
            result = crafting.resultSlot(),
            recipeChoices = recipeChoices(context)?.snapshot(context.menu)?.map { RecipeChoice(it.id(), it.label()) }
                .orEmpty(),
            text = context.remember("text") { CraftingText() },
        )
    }

    override fun handle(context: StorageWorkbenchContext, action: CraftingAction) {
        when (action) {
            is CraftingAction.Clear -> context.clearItems(action.toStorage)
            is CraftingAction.ChooseRecipe -> recipeChoices(context)?.select(context.menu, action.id)
        }
    }

    @Composable
    override fun Actions(state: CraftingState, scope: StorageWorkbenchScope<CraftingAction>) {
        // 配方选择只在有多个配方时出现，排在按钮左边，不会挪动合成格或别的按钮
        if (state.recipeChoices.size > 1) {
            RecipeChooser(state.recipeChoices, state.text) { scope.send(CraftingAction.ChooseRecipe(it)) }
            Spacer(Modifier.width(4.dp))
        }
        WorkbenchClearButtons(state.text.clear) { scope.send(CraftingAction.Clear(it)) }
    }

    @Composable
    override fun Content(state: CraftingState, scope: StorageWorkbenchScope<CraftingAction>) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.bdSlotFrame(Bd.colors)) { BdSlotGrid(scope.slots, state.grid, 3) }
            Arrow()
            ResultSlot(scope, state.result)
        }
    }
}

/** Polymorph 的配方选择：名称、候选个数与展开箭头，与工作台的下拉框一样高，点开列出候选 */
@Composable
private fun RecipeChooser(choices: List<RecipeChoice>, text: CraftingText, onChoose: (String) -> Unit) {
    val colors = Bd.colors
    var open by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val lit = hovered || open
    val tint = if (lit) colors[BdColors.accentDeep] else colors[BdColors.muted]
    Box {
        Row(
            Modifier.height(11.dp)
                .hoverable(interaction)
                .bdClickable(interaction) { open = !open }
                .background(if (lit) colors[BdColors.accentSoft] else Color.Transparent)
                .padding(horizontal = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OreText(text.recipe, color = tint, style = Bd.overline, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            OreText(choices.size.toString(), color = colors[BdColors.accentDeep], style = Bd.overline, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            BdIcon(BdIcons.ChevronDown, color = tint)
        }
        if (open) {
            BdPopover({ open = false }) {
                for (choice in choices) {
                    OptionRow(choice.label, selected = false) {
                        onChoose(choice.id)
                        open = false
                    }
                }
            }
        }
    }
}

// —— 铁砧 ——

@Immutable
internal data class AnvilState(
    val input: Int,
    val addition: Int,
    val result: Int,
    /** 当前的名称 */
    val name: String,
    /** 换了要修理的物品时加一，名称框据此重填 */
    val inputSerial: Int,
    /** 经验花费或"过于昂贵"，没有可显示的花费时为 null */
    val cost: String?,
    /** 玩家付得起这次花费 */
    val affordable: Boolean,
    val clear: WorkbenchClearText,
)

internal sealed interface AnvilAction {
    data class Rename(val name: String) : AnvilAction

    data class Clear(val toStorage: Boolean) : AnvilAction
}

/** 上一次看到的要修理的物品 */
private class AnvilInput {
    var stack: ItemStack = ItemStack.EMPTY
    var serial = 0
}

/** 铁砧：要修理的物品与材料、产物槽、名称框与经验花费，一起居中 */
internal object AnvilWorkbench : StorageWorkbench<AnvilState, AnvilAction> {
    private const val WIDTH = 130

    override fun title(context: StorageWorkbenchContext): Component = Component.translatable("block.minecraft.anvil")

    // 与合成格同高，工作区不因铁砧变高，并排时合成格与切石机的槽位和背包的行对齐
    override fun size(context: StorageWorkbenchContext) =
        StorageWorkbenchSize(WIDTH, RESULT_BOX + 3 + FIELD_HEIGHT + 2 + CAPTION_HEIGHT)

    override fun snapshot(context: StorageWorkbenchContext): AnvilState {
        val anvil = checkNotNull(context.menu.widget(AnvilWidget::class.java))
        val slots = context.menu.slots
        val input = context.remember("input") { AnvilInput() }
        val stack = slots[anvil.inputStart()].item
        if (!ItemStack.isSameItemSameComponents(stack, input.stack)) {
            input.stack = stack.copy()
            input.serial++
        }
        val result = slots[anvil.resultSlot()]
        val cost = anvil.cost()
        // 与原版铁砧界面相同：过于昂贵时总是提示，否则有产物才显示花费
        val (text, affordable) = when {
            cost <= 0 -> null to true
            cost >= 40 && !context.player.abilities.instabuild -> Component.translatable("container.repair.expensive").string to false
            !result.hasItem() -> null to true
            else -> Component.translatable("container.repair.cost", cost).string to result.mayPickup(context.player)
        }
        return AnvilState(
            input = anvil.inputStart(),
            addition = anvil.inputStart() + 1,
            result = anvil.resultSlot(),
            name = anvil.name(),
            inputSerial = input.serial,
            cost = text,
            affordable = affordable,
            clear = context.remember("clear") { WorkbenchClearText() },
        )
    }

    override fun handle(context: StorageWorkbenchContext, action: AnvilAction) {
        when (action) {
            is AnvilAction.Rename -> {
                val anvil = context.menu.widget(AnvilWidget::class.java) ?: return
                anvil.rename(action.name)
                context.request(AnvilWidget.RENAME, action.name)
            }

            is AnvilAction.Clear -> context.clearItems(action.toStorage)
        }
    }

    @Composable
    override fun Actions(state: AnvilState, scope: StorageWorkbenchScope<AnvilAction>) =
        WorkbenchClearButtons(state.clear) { scope.send(AnvilAction.Clear(it)) }

    @Composable
    override fun Content(state: AnvilState, scope: StorageWorkbenchScope<AnvilAction>) {
        val colors = Bd.colors
        var name by remember(state.inputSerial) { mutableStateOf(state.name) }
        Column(Modifier.width(WIDTH.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.bdSlotFrame(colors)) { BdSlot(scope.slots, state.input) }
                Spacer(Modifier.width(3.dp))
                BdIcon(BdIcons.Plus, color = colors[BdColors.faint])
                Spacer(Modifier.width(3.dp))
                Box(Modifier.bdSlotFrame(colors)) { BdSlot(scope.slots, state.addition) }
                Arrow()
                ResultSlot(scope, state.result)
            }
            Spacer(Modifier.height(3.dp))
            BdTextField(
                name,
                { text ->
                    if (text.length <= AnvilMenu.MAX_NAME_LENGTH) {
                        name = text
                        scope.send(AnvilAction.Rename(text))
                    }
                },
                "",
                Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            OreText(
                state.cost.orEmpty(),
                color = if (state.affordable) colors[BdColors.online] else colors[BdColors.danger],
                style = Bd.caption,
                maxLines = 1,
            )
        }
    }
}

// —— 锻造台 ——

@Immutable
internal data class SmithingState(
    val inputs: List<Int>,
    val result: Int,
    /** 各输入格空着时的底纹，格里有物品或没有可画的底纹时为 null */
    val underlays: List<ItemIcon?>,
    val clear: WorkbenchClearText,
)

/** 原版锻造台模板格的底纹，在两种模板之间轮换；基础与材料格的底纹由放进的模板决定 */
private val TEMPLATE_UNDERLAYS = listOf(
    ResourceLocation.withDefaultNamespace("item/empty_slot_smithing_template_armor_trim"),
    ResourceLocation.withDefaultNamespace("item/empty_slot_smithing_template_netherite_upgrade"),
)

/** 原版每隔这么多刻换一张底纹 */
private const val UNDERLAY_TICKS = 30

/** 方块图集里的一张图，画成空槽的底纹 */
private fun underlayIcon(sprite: ResourceLocation) =
    ItemIcon.drawn("Smithing slot underlay $sprite", { graphics ->
        graphics.blit(0, 0, 0, 16, 16, Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sprite))
    }, NativeRefresh.STATIC)

/** 锻造台：模板、基础物品与材料，箭头后是产物槽；空格里像原版一样轮换着画该放什么。唯一的动作是清空，true 时退回存储 */
internal object SmithingWorkbench : StorageWorkbench<SmithingState, Boolean> {
    override fun title(context: StorageWorkbenchContext): Component =
        Component.translatable("block.minecraft.smithing_table")

    override fun size(context: StorageWorkbenchContext) =
        StorageWorkbenchSize(3 * SLOT_PITCH + FRAME + ARROW + RESULT_BOX, RESULT_BOX)

    override fun snapshot(context: StorageWorkbenchContext): SmithingState {
        val smithing = checkNotNull(context.menu.widget(SmithingWidget::class.java))
        val slots = context.menu.slots
        val inputs = (smithing.inputStart() until smithing.resultSlot()).toList()
        val template = slots[smithing.inputStart()].item.item as? SmithingTemplateItem
        val sprites = listOf(
            TEMPLATE_UNDERLAYS,
            template?.baseSlotEmptyIcons.orEmpty(),
            template?.additionalSlotEmptyIcons.orEmpty()
        )
        // 按游戏时间轮换，所有格子同时换图；同一张图沿用同一个图标
        val step = (context.player.level().gameTime / UNDERLAY_TICKS).toInt()
        val icons = context.remember("underlays") { HashMap<ResourceLocation, ItemIcon>() }
        return SmithingState(
            inputs = inputs,
            result = smithing.resultSlot(),
            underlays = inputs.mapIndexed { index, id ->
                val choices = sprites[index]
                if (choices.isEmpty() || slots[id].hasItem()) return@mapIndexed null
                val sprite = choices[Math.floorMod(step, choices.size)]
                icons.getOrPut(sprite) { underlayIcon(sprite) }
            },
            clear = context.remember("clear") { WorkbenchClearText() },
        )
    }

    override fun handle(context: StorageWorkbenchContext, action: Boolean) {
        context.clearItems(action)
    }

    @Composable
    override fun Actions(state: SmithingState, scope: StorageWorkbenchScope<Boolean>) =
        WorkbenchClearButtons(state.clear) { scope.send(it) }

    @Composable
    override fun Content(state: SmithingState, scope: StorageWorkbenchScope<Boolean>) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.bdSlotFrame(Bd.colors)) {
                Row {
                    state.inputs.forEachIndexed { index, id ->
                        BdSlot(
                            scope.slots,
                            id,
                            underlay = state.underlays[index]
                        )
                    }
                }
            }
            Arrow()
            ResultSlot(scope, state.result)
        }
    }
}

// —— 切石机 ——

@Immutable
internal data class RecipeOption(val icon: ItemIcon, val name: String)

@Immutable
internal data class StonecutterState(
    val input: Int,
    val result: Int,
    /** 从当前输入能切出的产物 */
    val recipes: List<RecipeOption>,
    /** 选中的配方，没有时为 -1 */
    val selected: Int,
    val clear: WorkbenchClearText,
)

internal sealed interface StonecutterAction {
    data class Select(val index: Int) : StonecutterAction

    data class Clear(val toStorage: Boolean) : StonecutterAction
}

/** 切石机：输入槽、可选的产物与它们的滚动条、产物槽 */
internal object StonecutterWorkbench : StorageWorkbench<StonecutterState, StonecutterAction> {
    private const val COLUMNS = 4
    private const val ROWS = 3

    override fun title(context: StorageWorkbenchContext): Component =
        Component.translatable("block.minecraft.stonecutter")

    override fun size(context: StorageWorkbenchContext) =
        StorageWorkbenchSize(
            SLOT_PITCH + FRAME + 5 + COLUMNS * SLOT_PITCH + FRAME + 2 + SCROLLBAR_WIDTH + ARROW + RESULT_BOX,
            ROWS * SLOT_PITCH + FRAME,
        )

    override fun snapshot(context: StorageWorkbenchContext): StonecutterState {
        val stonecutter = checkNotNull(context.menu.widget(StonecutterWidget::class.java))
        val registries = context.player.level().registryAccess()
        // 同一个配方的产物沿用上次的图标
        val options = context.remember("options") { HashMap<ResourceLocation, RecipeOption>() }
        return StonecutterState(
            input = stonecutter.inputStart(),
            result = stonecutter.resultSlot(),
            recipes = stonecutter.recipes().map { recipe ->
                options.getOrPut(recipe.id()) {
                    val product = recipe.value().getResultItem(registries)
                    RecipeOption(ItemIcon.snapshot(product), product.hoverName.string)
                }
            },
            selected = stonecutter.selected(),
            clear = context.remember("clear") { WorkbenchClearText() },
        )
    }

    override fun handle(context: StorageWorkbenchContext, action: StonecutterAction) {
        when (action) {
            is StonecutterAction.Select -> {
                val stonecutter = context.menu.widget(StonecutterWidget::class.java) ?: return
                stonecutter.select(context.player, action.index)
                context.request(StonecutterWidget.SELECT, action.index)
            }

            is StonecutterAction.Clear -> context.clearItems(action.toStorage)
        }
    }

    @Composable
    override fun Actions(state: StonecutterState, scope: StorageWorkbenchScope<StonecutterAction>) =
        WorkbenchClearButtons(state.clear) { scope.send(StonecutterAction.Clear(it)) }

    @Composable
    override fun Content(state: StonecutterState, scope: StorageWorkbenchScope<StonecutterAction>) {
        val colors = Bd.colors
        val height = (ROWS * SLOT_PITCH + FRAME).dp
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.bdSlotFrame(colors)) { BdSlot(scope.slots, state.input) }
            Spacer(Modifier.width(5.dp))
            // 产物多于三行时在框内滚动；换了输入时回到最上面。滚轮一格滚动一行，像原版切石机，不翻动存储格
            val scroll = remember(state.recipes) { ScrollState(0) }
            Box(
                Modifier.size((COLUMNS * SLOT_PITCH + FRAME).dp, height)
                    .bdSlotFrame(colors)
                    .bdWheelStep(scroll, SLOT_PITCH.dp)
            ) {
                Column(Modifier.verticalScroll(scroll)) {
                    for (row in state.recipes.indices.chunked(COLUMNS)) {
                        Row {
                            for (index in row) {
                                RecipeCell(
                                    state.recipes[index],
                                    index == state.selected
                                ) { scope.send(StonecutterAction.Select(index)) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(2.dp))
            BdScrollbar(scroll, Modifier.width(SCROLLBAR_WIDTH.dp).height(height))
            Arrow()
            ResultSlot(scope, state.result)
        }
    }
}

/** 切石机的一个可选产物：选中的有强调色底色与细框 */
@Composable
private fun RecipeCell(option: RecipeOption, selected: Boolean, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    BdTooltip(option.name) {
        Box(
            Modifier.size(SLOT_PITCH.dp)
                .hoverable(interaction)
                .bdClickable(interaction, enabled = !selected, onClick = onClick)
                .then(
                    when {
                        selected -> Modifier.background(colors[BdColors.accent].copy(alpha = 0.3f))
                            .border(1.dp, colors[BdColors.accent])

                        hovered -> Modifier.background(colors[BdColors.surface])
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            MinecraftItemIcon(option.icon, Modifier.size(16.dp))
        }
    }
}
