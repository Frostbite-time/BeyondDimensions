package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import dev.composemc.forge.sync.MenuAction;
import dev.composemc.forge.sync.MenuSync;
import dev.composemc.forge.sync.MinecraftSyncCodecs;
import dev.composemc.sync.state.SyncCodec;
import dev.composemc.sync.state.SyncCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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
    /** 句柄为 -1 时使用 literal 携带的资源键 */
    public record Key(long handle, IStackKey<?> literal) {}

    public record Click(int slot, Key key, long amount, int button, boolean shift) {}

    public record Ghost(int slot, IStackKey<?> key, long amount) {}

    public record Batch(Key key, boolean toStorage) {}

    public record Mode(int setting, int value) {}

    public record Recipe(List<Key> keys, List<Long> amounts, boolean compress)
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

    public static final MenuAction<BDBaseMenu, Boolean> CRAFT_RETURN = MenuAction.of("bd.craft_return", SyncCodecs.BOOLEAN,
            (menu, player, toStorage) -> {
                if (!(menu instanceof DimensionsCraftMenu craft))
                    return false;
                craft.cleanCraftSlots(toStorage);
                return true;
            });

    public static final MenuAction<BDBaseMenu, Boolean> CRAFT_PREFERENCE = MenuAction.of("bd.craft_preference", SyncCodecs.BOOLEAN,
            (menu, player, toStorage) -> {
                if (!(menu instanceof DimensionsCraftMenu craft))
                    return false;
                craft.firstCraftReturnDir = toStorage;
                return true;
            });

    private static final SyncCodec<Mode> MODE_CODEC = SyncCodec.of("beyonddimensions:interface_mode/1",
            (out, value) -> {
                out.writeInt(value.setting());
                out.writeInt(value.value());
            },
            in -> new Mode(in.readInt(), in.readInt()));

    public static final MenuAction<BDBaseMenu, Mode> MODE = MenuAction.of("bd.interface_mode", MODE_CODEC,
            (menu, player, mode) -> menu instanceof NetInterfaceBaseMenu face && face.applyMode(mode.setting(), mode.value()));

    // 资源键的编解码依赖本菜单的注册表上下文，因此以下操作按菜单实例创建
    public final MenuAction<BDBaseMenu, Click> CLICK;
    public final MenuAction<BDBaseMenu, Ghost> GHOST;
    public final MenuAction<BDBaseMenu, Batch> BATCH;
    public final MenuAction<BDBaseMenu, Recipe> RECIPE;

    private final BDBaseMenu menu;
    private Boolean pendingPreference;
    private long lastFeedbackTick = Long.MIN_VALUE;

    public BDMenuCommands(BDBaseMenu menu)
    {
        this.menu = menu;
        var nativeKey = MinecraftSyncCodecs.registry("beyonddimensions:stack_key/1", BDMenuResources.MAX_NATIVE_BYTES, IStackKey.STREAM_CODEC);

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

        var clickCodec = SyncCodec.<Click>of("beyonddimensions:click/1:" + keyCodec.id(),
                (out, value) -> {
                    out.writeInt(value.slot());
                    keyCodec.write(out, value.key());
                    out.writeLong(value.amount());
                    out.writeInt(value.button());
                    out.writeBoolean(value.shift());
                },
                in -> new Click(in.readInt(), keyCodec.read(in), in.readLong(), in.readInt(), SyncCodecs.BOOLEAN.read(in)));
        CLICK = MenuAction.of("bd.click", clickCodec, BDMenuResources.MAX_ACTION_BYTES, (m, player, click) -> {
            IStackKey<?> key = m.commands().resolve(click.key());
            if (key == null || click.slot() < 0 || click.slot() >= m.slots.size()
                    || click.button() < 0 || click.button() > 2
                    || click.amount() < 0 || click.amount() > key.getVanillaMaxStackSize())
                return false;
            m.customClickHandler(click.slot(), new KeyAmount(key, click.amount()), click.button(), click.shift());
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
        GHOST = MenuAction.of("bd.ghost", ghostCodec, BDMenuResources.MAX_ACTION_BYTES, (m, player, ghost) -> {
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

        var batchCodec = SyncCodec.<Batch>of("beyonddimensions:batch/1:" + keyCodec.id(),
                (out, value) -> {
                    keyCodec.write(out, value.key());
                    out.writeBoolean(value.toStorage());
                },
                in -> new Batch(keyCodec.read(in), SyncCodecs.BOOLEAN.read(in)));
        BATCH = MenuAction.of("bd.batch", batchCodec, BDMenuResources.MAX_ACTION_BYTES,
                (m, player, batch) -> m.commands().applyBatch(batch));

        var recipeCodec = SyncCodec.<Recipe>of("beyonddimensions:recipe/1:" + keyCodec.id(),
                (out, value) -> {
                    if (value.keys().size() > MAX_RECIPE_KEYS || value.keys().size() != value.amounts().size())
                        throw new IOException("Invalid recipe size");
                    out.writeInt(value.keys().size());
                    for (int i = 0; i < value.keys().size(); i++)
                    {
                        keyCodec.write(out, value.keys().get(i));
                        out.writeLong(value.amounts().get(i));
                    }
                    out.writeBoolean(value.compress());
                },
                in -> {
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
                    return new Recipe(keys, amounts, SyncCodecs.BOOLEAN.read(in));
                });
        RECIPE = MenuAction.of("bd.recipe", recipeCodec, BDMenuResources.MAX_ACTION_BYTES, (m, player, recipe) -> {
            if (!(m instanceof DimensionsCraftMenu craft))
                return false;
            List<IStackKey<?>> keys = new ArrayList<>(recipe.keys().size());
            for (Key reference : recipe.keys())
            {
                IStackKey<?> key = m.commands().resolve(reference);
                if (key == null)
                    return false;
                keys.add(key);
            }
            return craft.transferRecipe(keys, recipe.amounts(), recipe.compress());
        });
    }

    /** 注册槽位点击、标记与批量转移，并在客户端提示失败的请求 */
    public <M extends BDBaseMenu> MenuSync<M> inventory(MenuSync<M> sync)
    {
        return sync.action(CLICK).action(GHOST).action(BATCH).onActionResult(this::reportFailure);
    }

    /** 注册合成页面的配方填充与合成槽清理 */
    public <M extends BDBaseMenu> MenuSync<M> crafting(MenuSync<M> sync)
    {
        return sync.action(RECIPE).action(CRAFT_RETURN).action(CRAFT_PREFERENCE);
    }

    private void reportFailure(MenuSync.ActionResult result)
    {
        if (!menu.player.level().isClientSide() || result.status() == MenuSync.ActionStatus.APPLIED)
            return;
        long tick = menu.player.level().getGameTime();
        if (lastFeedbackTick != Long.MIN_VALUE && tick - lastFeedbackTick < FEEDBACK_INTERVAL_TICKS)
            return;
        lastFeedbackTick = tick;
        String reason = switch (result.failure())
        {
            case QUEUE_BYTES, PENDING_ACTIONS -> "busy";
            case BODY_BYTES -> "large";
            case NOT_READY -> "waiting";
            case QUEUE_TIMEOUT, SEND_TIMEOUT, REPLY_TIMEOUT, ASSEMBLY_TIMEOUT -> "timeout";
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

    /** 已有句柄的资源以句柄引用，否则直接携带资源键 */
    public Key reference(IStackKey<?> key)
    {
        long id = menu.resources().idOf(key);
        if (id == 0 && !key.isEmpty())
            return new Key(LITERAL, key);
        return new Key(id, null);
    }

    public boolean click(int slot, KeyAmount key, int button, boolean shift)
    {
        return menu.menuSync().request(CLICK, new Click(slot, reference(key.key()), key.amount(), button, shift)).queued();
    }

    public boolean ghost(int slot, KeyAmount value)
    {
        return menu.menuSync().request(GHOST, new Ghost(slot, value.key(), value.amount())).queued();
    }

    public boolean batch(KeyAmount key, boolean toStorage)
    {
        return menu.menuSync().request(BATCH, new Batch(reference(key.key()), toStorage)).queued();
    }

    public boolean recipe(List<IStackKey<?>> keys, List<Long> amounts, boolean compress)
    {
        if (keys.size() != amounts.size() || keys.size() > MAX_RECIPE_KEYS)
            return false;
        List<Key> references = keys.stream().map(this::reference).toList();
        return menu.menuSync().request(RECIPE, new Recipe(references, amounts, compress)).queued();
    }

    public boolean mode(int setting, int value)
    {
        return menu.menuSync().request(MODE, new Mode(setting, value)).queued();
    }

    public boolean returnCrafting(boolean toStorage)
    {
        return menu.menuSync().request(CRAFT_RETURN, toStorage).queued();
    }

    /** 合成返还方向在首个快照到达后才能发送，之前的设置会暂存 */
    public void preference(boolean toStorage)
    {
        pendingPreference = toStorage;
        flushPreference();
    }

    public void flushPreference()
    {
        if (pendingPreference != null && menu.menuSync().hasSnapshot()
                && menu.menuSync().request(CRAFT_PREFERENCE, pendingPreference).queued())
            pendingPreference = null;
    }

    /** 批量转移：把背包中所有同种物品存入网络，或把网络中的该物品尽量取到背包 */
    private boolean applyBatch(Batch request)
    {
        IStackKey<?> key = resolve(request.key());
        if (!(key instanceof ItemStackKey item) || menu.inventoryStartIndex < 0 || menu.inventoryEndIndex > menu.slots.size())
            return false;
        if (request.toStorage())
        {
            for (int i = menu.inventoryStartIndex; i < menu.inventoryEndIndex; i++)
            {
                ItemStack stack = menu.slots.get(i).getItem();
                if (item.equals(new ItemStackKey(stack)))
                    menu.customClickHandler(i, new KeyAmount(new ItemStackKey(stack), stack.getCount()), 0, true);
            }
        }
        else if (menu instanceof DimensionsNetMenu net)
        {
            for (int i = menu.inventoryStartIndex; i < menu.inventoryEndIndex && net.storage.hasStack(item); i++)
            {
                var extracted = net.storage.extract(item, Integer.MAX_VALUE, false, false);
                if (extracted.toStack() instanceof ItemStack stack)
                {
                    Slot slot = menu.slots.get(i);
                    ItemStack remaining = slot.safeInsert(stack);
                    if (!remaining.isEmpty())
                        net.storage.insert(new ItemStackKey(remaining), remaining.getCount(), false);
                }
                else
                {
                    net.storage.insert(extracted.key(), extracted.amount(), false);
                }
            }
        }
        else
        {
            return false;
        }
        menu.broadcastChanges();
        return true;
    }
}
