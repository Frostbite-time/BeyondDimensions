package com.wintercogs.beyonddimensions.common.menu.sync;

import com.wintercogs.beyonddimensions.api.menu.interaction.SlotClick;
import com.wintercogs.beyonddimensions.api.menu.widget.ClearableWidget;
import com.wintercogs.beyonddimensions.api.menu.widget.RecipeFillWidget;
import com.wintercogs.beyonddimensions.api.menu.workbench.PlayerWorkbenches;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.forge.sync.MinecraftSyncCodecs;
import dev.compixel.forge.sync.PacketCodec;
import dev.compixel.sync.action.ActionResult;
import dev.compixel.sync.action.ActionStatus;
import dev.compixel.sync.state.SyncCodec;
import dev.compixel.sync.state.SyncCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 资源菜单的操作请求，全部由服务端校验后执行。
 * 资源优先以本菜单内的句柄引用；外部配方查看器提供的资源可以直接携带资源键。
 */
public final class BDMenuCommands
{
    /**
     * 句柄为 -1 时使用 literal 携带的资源键
     */
    public record Key(long handle, IStackKey<?> literal)
    {
    }

    /**
     * @param modifiers 按住的修饰键与点击标志，见 {@link SlotClick#SHIFT} 等
     */
    public record Click(int slot, Key key, long amount, int button, int modifiers)
    {
    }

    /**
     * 右键菜单的"取出 x 个"
     */
    public record Take(int slot, Key key, long amount)
    {
    }

    public record Ghost(int slot, IStackKey<?> key, long amount)
    {
    }

    /**
     * 按配方查看器给出的原料填充工作台 {@code workbench}；{@code recipe} 是配方的 ID，没有时为 null
     */
    public record Recipe(ResourceLocation workbench, List<Key> keys, List<Long> amounts, boolean compress, @Nullable ResourceLocation recipe)
    {
        public Recipe
        {
            keys = List.copyOf(keys);
            amounts = List.copyOf(amounts);
        }
    }

    private static final long LITERAL = -1;
    private static final int MAX_RECIPE_KEYS = 4096;
    private static final int FEEDBACK_INTERVAL_TICKS = 20;

    /**
     * 把工作台 {@code workbench} 里的物品还给玩家，{@code toStorage} 时先退回存储
     */
    public record WorkbenchClear(ResourceLocation workbench, boolean toStorage)
    {
    }

    private static final SyncCodec<WorkbenchClear> WORKBENCH_CLEAR_CODEC = SyncCodec.of("beyonddimensions:workbench_clear/1",
            (out, value) -> {
                SyncCodecs.STRING.write(out, value.workbench().toString());
                out.writeBoolean(value.toStorage());
            },
            in -> new WorkbenchClear(readId(in), in.readBoolean()));

    public static final MenuAction<BDBaseMenu, WorkbenchClear> WORKBENCH_CLEAR = MenuAction.of("bd_workbench_clear", WORKBENCH_CLEAR_CODEC,
            (menu, player, request) -> {
                if (!(menu instanceof DimensionsNetMenu storage) || !(storage.workbenchWidget(request.workbench()) instanceof ClearableWidget widget))
                    return false;
                widget.clearItems(request.toStorage());
                return true;
            });

    /**
     * 工作台的偏好：清空时优先退回存储还是背包，关闭时是否保留。保存在玩家身上，关闭菜单时由服务器按它处理
     */
    public record WorkbenchPreference(boolean toStorage, boolean keep)
    {
    }

    private static final SyncCodec<WorkbenchPreference> WORKBENCH_PREFERENCE_CODEC = SyncCodec.of("beyonddimensions:workbench_preference/1",
            (out, value) -> {
                out.writeBoolean(value.toStorage());
                out.writeBoolean(value.keep());
            },
            in -> new WorkbenchPreference(in.readBoolean(), in.readBoolean()));

    public static final MenuAction<BDBaseMenu, WorkbenchPreference> WORKBENCH_PREFERENCE = MenuAction.of("bd_workbench_preference",
            WORKBENCH_PREFERENCE_CODEC, (menu, player, preference) -> {
                if (!(menu instanceof DimensionsNetMenu))
                    return false;
                PlayerWorkbenches.of(player).setPreference(preference.toStorage(), preference.keep());
                return true;
            });

    // 资源键的编解码依赖本菜单的注册表上下文，因此以下操作按菜单实例创建
    public final MenuAction<BDBaseMenu, Click> CLICK;
    public final MenuAction<BDBaseMenu, Take> TAKE;
    public final MenuAction<BDBaseMenu, Ghost> GHOST;
    public final MenuAction<BDBaseMenu, Recipe> RECIPE;

    private final BDBaseMenu menu;
    private WorkbenchPreference pendingPreference;
    private long lastFeedbackTick = Long.MIN_VALUE;

    public BDMenuCommands(BDBaseMenu menu)
    {
        this.menu = menu;
        var nativeKey = MinecraftSyncCodecs.buffer("beyonddimensions:stack_key/1",
                PacketCodec.<IStackKey<?>>of(IStackKey::serializeCommon, IStackKey::deserializeCommon));

        var keyCodec = SyncCodec.<Key>of("beyonddimensions:command_key/1:" + nativeKey.id(),
                (out, value) -> {
                    out.writeLong(value.handle());
                    if (value.handle() == LITERAL)
                        nativeKey.write(out, Objects.requireNonNull(value.literal()));
                },
                in -> {
                    long handle = in.readLong();
                    return new Key(handle, handle == LITERAL ? nativeKey.read(in) : null);
                });

        var clickCodec = SyncCodec.<Click>of("beyonddimensions:click/2:" + keyCodec.id(),
                (out, value) -> {
                    out.writeInt(value.slot());
                    keyCodec.write(out, value.key());
                    out.writeLong(value.amount());
                    out.writeInt(value.button());
                    out.writeByte(value.modifiers());
                },
                in -> new Click(in.readInt(), keyCodec.read(in), in.readLong(), in.readInt(), in.readUnsignedByte()));
        CLICK = MenuAction.of("bd_click", clickCodec, BDMenuResources.MAX_ACTION_BYTES, (m, player, click) -> {
            IStackKey<?> key = m.commands().resolve(click.key());
            if (key == null || click.slot() < 0 || click.slot() >= m.slots.size()
                    || click.button() < 0 || click.button() > 2 || (click.modifiers() & ~SlotClick.MODIFIERS) != 0
                    || click.amount() < 0 || click.amount() > key.getVanillaMaxStackSize())
                return false;
            m.customClickHandler(click.slot(), new KeyAmount(key, click.amount()), click.button(), click.modifiers());
            m.broadcastChanges();
            return true;
        });

        var takeCodec = SyncCodec.<Take>of("beyonddimensions:take/1:" + keyCodec.id(),
                (out, value) -> {
                    out.writeInt(value.slot());
                    keyCodec.write(out, value.key());
                    out.writeLong(value.amount());
                },
                in -> new Take(in.readInt(), keyCodec.read(in), in.readLong()));
        TAKE = MenuAction.of("bd_take", takeCodec, BDMenuResources.MAX_ACTION_BYTES, (m, player, take) -> {
            IStackKey<?> key = m.commands().resolve(take.key());
            if (key == null || take.slot() < 0 || take.slot() >= m.slots.size()
                    || take.amount() < 1 || take.amount() > key.getVanillaMaxStackSize())
                return false;
            m.customClickHandler(take.slot(), new KeyAmount(key, take.amount()), 0, 0, take.amount());
            m.broadcastChanges();
            return true;
        });

        var ghostCodec = SyncCodec.<Ghost>of("beyonddimensions:ghost/1:" + nativeKey.id(),
                (out, value) -> {
                    out.writeInt(value.slot());
                    nativeKey.write(out, value.key());
                    out.writeLong(value.amount());
                },
                in -> new Ghost(in.readInt(), nativeKey.read(in), in.readLong()));
        GHOST = MenuAction.of("bd_ghost", ghostCodec, BDMenuResources.MAX_ACTION_BYTES, (m, player, ghost) -> {
            // 标记槽只接受 0 或 1 个资源作为模板
            if (ghost.slot() < 0 || ghost.slot() >= m.slots.size() || ghost.amount() < 0 || ghost.amount() > 1
                    || !(m.slots.get(ghost.slot()) instanceof AbstractStackTypedSlot slot) || !slot.isFake())
                return false;
            if (!ghost.key().isEmpty() && ghost.amount() > 0 && !slot.getStorage().isStackValid(slot.getSlotIndex(), ghost.key()))
                return false;
            slot.setStackDirectly(ghost.key(), ghost.amount());
            m.broadcastChanges();
            return true;
        });

        var recipeCodec = SyncCodec.<Recipe>of("beyonddimensions:recipe/2:" + keyCodec.id(),
                (out, value) -> {
                    if (value.keys().size() > MAX_RECIPE_KEYS || value.keys().size() != value.amounts().size())
                        throw new IOException("Invalid recipe size");
                    SyncCodecs.STRING.write(out, value.workbench().toString());
                    out.writeInt(value.keys().size());
                    for (int i = 0; i < value.keys().size(); i++)
                    {
                        keyCodec.write(out, value.keys().get(i));
                        out.writeLong(value.amounts().get(i));
                    }
                    out.writeBoolean(value.compress());
                    SyncCodecs.STRING.write(out, value.recipe() == null ? "" : value.recipe().toString());
                },
                in -> {
                    ResourceLocation workbench = readId(in);
                    int count = in.readInt();
                    if (count < 0 || count > MAX_RECIPE_KEYS)
                        throw new IOException("Invalid recipe size");
                    List<Key> keys = new ArrayList<>(count);
                    List<Long> amounts = new ArrayList<>(count);
                    for (int i = 0; i < count; i++)
                    {
                        keys.add(keyCodec.read(in));
                        amounts.add(in.readLong());
                    }
                    boolean compress = SyncCodecs.BOOLEAN.read(in);
                    String recipe = SyncCodecs.STRING.read(in);
                    return new Recipe(workbench, keys, amounts, compress, recipe.isEmpty() ? null : parseId(recipe));
                });
        RECIPE = MenuAction.of("bd_recipe", recipeCodec, BDMenuResources.MAX_ACTION_BYTES, (m, player, recipe) -> {
            if (!(m instanceof DimensionsNetMenu storage) || !(storage.workbenchWidget(recipe.workbench()) instanceof RecipeFillWidget widget))
                return false;
            List<IStackKey<?>> keys = new ArrayList<>(recipe.keys().size());
            for (Key reference : recipe.keys())
            {
                IStackKey<?> key = m.commands().resolve(reference);
                if (key == null)
                    return false;
                keys.add(key);
            }
            return widget.fillRecipe(keys, recipe.amounts(), recipe.compress(), recipe.recipe());
        });
    }

    /**
     * 注册槽位点击与标记，并在客户端提示失败的请求
     */
    public <M extends BDBaseMenu> MenuSync<M> inventory(MenuSync<M> sync)
    {
        return sync.action(CLICK).action(TAKE).action(GHOST).onActionResult(this::reportFailure);
    }

    /**
     * 注册工作区的配方填充、清空与偏好，由存储菜单调用
     */
    public <M extends BDBaseMenu> MenuSync<M> workbenches(MenuSync<M> sync)
    {
        return sync.action(RECIPE).action(WORKBENCH_CLEAR).action(WORKBENCH_PREFERENCE);
    }

    private static ResourceLocation readId(DataInput in) throws IOException
    {
        return parseId(SyncCodecs.STRING.read(in));
    }

    private static ResourceLocation parseId(String text) throws IOException
    {
        ResourceLocation id = ResourceLocation.tryParse(text);
        if (id == null)
            throw new IOException("Invalid id " + text);
        return id;
    }

    private void reportFailure(ActionResult result)
    {
        if (!menu.player.level().isClientSide() || result.status() == ActionStatus.APPLIED)
            return;
        long tick = menu.player.level().getGameTime();
        if (lastFeedbackTick != Long.MIN_VALUE && tick - lastFeedbackTick < FEEDBACK_INTERVAL_TICKS)
            return;
        lastFeedbackTick = tick;
        String reason = switch (result.failure())
        {
            case BODY_BYTES -> "large";
            case NOT_READY -> "waiting";
            case THROTTLED -> "rate";
            case CODEC, MALFORMED -> "data";
            default -> "rejected";
        };
        menu.player.displayClientMessage(Component.translatable("message.beyonddimensions.menu_action." + reason), false);
    }

    private IStackKey<?> resolve(Key key)
    {
        if (key.handle() == LITERAL)
            return key.literal();
        return key.handle() < 0 ? null : menu.resources().resolve(key.handle());
    }

    /**
     * 已有句柄的资源以句柄引用，否则直接携带资源键
     */
    public Key reference(IStackKey<?> key)
    {
        long id = menu.resources().idOf(key);
        if (id == 0 && !key.isEmpty())
            return new Key(LITERAL, key);
        return new Key(id, null);
    }

    /**
     * @param modifiers 按住的修饰键，见 {@link SlotClick#SHIFT} 等
     */
    public boolean click(int slot, KeyAmount key, int button, int modifiers)
    {
        return menu.menuSync().request(CLICK, new Click(slot, reference(key.key()), key.amount(), button, modifiers)).queued();
    }

    public boolean take(int slot, IStackKey<?> key, long amount)
    {
        return menu.menuSync().request(TAKE, new Take(slot, reference(key), amount)).queued();
    }

    public boolean ghost(int slot, KeyAmount value)
    {
        return menu.menuSync().request(GHOST, new Ghost(slot, value.key(), value.amount())).queued();
    }

    /**
     * 请服务器按原料填充工作台 {@code workbench}，见 {@link RecipeFillWidget}；{@code recipe} 是配方的 ID，没有时为 null
     */
    public boolean recipe(ResourceLocation workbench, List<IStackKey<?>> keys, List<Long> amounts, boolean compress,
                          @Nullable ResourceLocation recipe)
    {
        if (keys.size() != amounts.size() || keys.size() > MAX_RECIPE_KEYS)
            return false;
        List<Key> references = keys.stream().map(this::reference).toList();
        return menu.menuSync().request(RECIPE, new Recipe(workbench, references, amounts, compress, recipe)).queued();
    }

    /**
     * 请服务器把工作台 {@code workbench} 里的物品还给玩家，{@code toStorage} 时先退回存储
     */
    public boolean clearWorkbench(ResourceLocation workbench, boolean toStorage)
    {
        return menu.menuSync().request(WORKBENCH_CLEAR, new WorkbenchClear(workbench, toStorage)).queued();
    }

    /**
     * 工作台的偏好在首个快照到达后才能发送，之前的设置会暂存
     */
    public void preference(boolean toStorage, boolean keep)
    {
        pendingPreference = new WorkbenchPreference(toStorage, keep);
        flushPreference();
    }

    public void flushPreference()
    {
        if (pendingPreference != null && menu.menuSync().hasSnapshot()
                && menu.menuSync().request(WORKBENCH_PREFERENCE, pendingPreference).queued())
            pendingPreference = null;
    }
}
