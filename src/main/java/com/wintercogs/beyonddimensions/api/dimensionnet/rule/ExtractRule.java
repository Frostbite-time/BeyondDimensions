package com.wintercogs.beyonddimensions.api.dimensionnet.rule;

import org.jetbrains.annotations.Nullable;

/**
 * 资源离开维度网络前的规则。只能读取，不能改动任何存储：变化写在计划里，副作用放进 {@link ExtractPlan#onCommit}。
 * 模拟与执行都会调用，同样的状态必须给出同样的计划
 */
@FunctionalInterface
public interface ExtractRule
{
    /**
     * @return 计划；null 表示不处理，交给后面的规则
     */
    @Nullable ExtractPlan plan(StorageRequest request);
}
