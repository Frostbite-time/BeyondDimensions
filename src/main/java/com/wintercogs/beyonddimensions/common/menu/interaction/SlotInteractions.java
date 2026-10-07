package com.wintercogs.beyonddimensions.common.menu.interaction;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 槽位交互的注册表。一次点击按表中顺序逐条尝试：断言成立且执行后报告"已处理"的第一条生效，其余不再执行。
 * 表中的顺序就是优先级，默认的存入与取出排在最后。
 * <p>
 * 联动模组用 {@link #register} 加入自己的交互，并相对某条已有交互指定位置。
 * 位置不成立（引用的 id 不存在、id 重复）时立刻抛出异常并指出原因，游戏随之启动失败。
 * BD 的内置交互在本类首次使用时注册，因此联动注册时总能引用它们。
 */
public final class SlotInteractions
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 一种槽位交互
     */
    public interface Interaction
    {
        /**
         * 是否处理这次点击。只能读取上下文，不能改动任何东西
         */
        boolean applies(SlotClick click);

        /**
         * 执行交互；返回 false 表示没有做任何事，交给后面的交互
         */
        boolean perform(SlotClick click);
    }

    /**
     * 新交互在表中的位置
     */
    public sealed interface Placement
    {
    }

    private record First() implements Placement
    {
    }

    private record Last() implements Placement
    {
    }

    private record Before(ResourceLocation id) implements Placement
    {
    }

    private record After(ResourceLocation id) implements Placement
    {
    }

    public static Placement first()
    {
        return new First();
    }

    public static Placement last()
    {
        return new Last();
    }

    public static Placement before(ResourceLocation id)
    {
        return new Before(Objects.requireNonNull(id));
    }

    public static Placement after(ResourceLocation id)
    {
        return new After(Objects.requireNonNull(id));
    }

    private record Entry(ResourceLocation id, Interaction interaction)
    {
    }

    // 点击在服务端线程读取，注册可能来自模组加载线程：每次注册换一份新表
    private static volatile List<Entry> entries = List.of();

    static
    {
        BuiltinSlotInteractions.register();
    }

    private SlotInteractions()
    {
    }

    /**
     * 注册一种交互。
     *
     * @throws IllegalStateException id 已被注册，或 {@code placement} 引用的交互不存在
     */
    public static synchronized void register(ResourceLocation id, Interaction interaction, Placement placement)
    {
        Objects.requireNonNull(id);
        Objects.requireNonNull(interaction);
        List<Entry> next = new ArrayList<>(entries);
        if (indexOf(next, id) >= 0)
            throw new IllegalStateException("Slot interaction " + id + " is already registered");
        int index = switch (placement)
        {
            case First first -> 0;
            case Last last -> next.size();
            case Before before -> require(next, id, before.id(), "before");
            case After after -> require(next, id, after.id(), "after") + 1;
        };
        next.add(index, new Entry(id, interaction));
        entries = List.copyOf(next);
    }

    private static int require(List<Entry> list, ResourceLocation id, ResourceLocation anchor, String relation)
    {
        int index = indexOf(list, anchor);
        if (index < 0)
            throw new IllegalStateException("Cannot register slot interaction " + id + " " + relation + " " + anchor
                    + ": no interaction " + anchor + " is registered. Registered, in order: " + ids());
        return index;
    }

    private static int indexOf(List<Entry> list, ResourceLocation id)
    {
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).id().equals(id)) return i;
        return -1;
    }

    /**
     * 当前全部交互的 id，按尝试顺序
     */
    public static List<ResourceLocation> ids()
    {
        return entries.stream().map(Entry::id).toList();
    }

    /**
     * 按顺序处理一次点击，返回是否有交互处理了它
     */
    public static boolean dispatch(SlotClick click)
    {
        for (Entry entry : entries)
        {
            if (entry.interaction().applies(click) && entry.interaction().perform(click))
            {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Slot click {} {} button {}{} handled by {}", click.kind(), click.slot().index,
                            click.button(), click.shift() ? " with shift" : "", entry.id());
                return true;
            }
        }
        return false;
    }
}
