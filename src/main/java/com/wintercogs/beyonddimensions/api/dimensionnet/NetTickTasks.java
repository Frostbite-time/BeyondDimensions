package com.wintercogs.beyonddimensions.api.dimensionnet;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 维度网络每刻的任务：服务端每刻开始时，对每个已载入的网络按注册顺序各调用一次；已删除与临时网络不会调用。
 * 附属在模组构造期间注册，不必自己订阅刻事件或遍历网络：
 * <pre>{@code
 * NetTickTasks.register(new ResourceLocation("myaddon", "upkeep"), net -> {
 *     Upkeep upkeep = net.getOrCreateData(UPKEEP.get());
 *     upkeep.tick(net);
 * });
 * }</pre>
 * 任务在服务器线程运行，抛出的异常与刻事件里的一样不会被拦下
 */
public final class NetTickTasks
{
    @FunctionalInterface
    public interface Task
    {
        void tick(DimensionsNet net);
    }

    private static final Map<ResourceLocation, Task> TASKS = new LinkedHashMap<>();

    // 服务器线程每刻读取的副本，注册与移除时替换
    private static volatile Task[] running = new Task[0];

    private NetTickTasks()
    {
    }

    /**
     * @throws IllegalStateException id 已被注册
     */
    public static synchronized void register(ResourceLocation id, Task task)
    {
        Objects.requireNonNull(id);
        Objects.requireNonNull(task);
        if (TASKS.containsKey(id))
            throw new IllegalStateException("Network tick task " + id + " is already registered");
        TASKS.put(id, task);
        running = TASKS.values().toArray(Task[]::new);
    }

    /**
     * @return 是否有这个任务
     */
    public static synchronized boolean remove(ResourceLocation id)
    {
        if (TASKS.remove(id) == null)
            return false;
        running = TASKS.values().toArray(Task[]::new);
        return true;
    }

    /**
     * 已注册的任务，按执行顺序
     */
    public static synchronized List<ResourceLocation> ids()
    {
        return List.copyOf(TASKS.keySet());
    }

    static void run(DimensionsNet net)
    {
        for (Task task : running)
            task.tick(net);
    }
}
