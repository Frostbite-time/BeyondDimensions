package com.wintercogs.beyonddimensions.api.dimensionnet.rule;

/**
 * 操作真正提交后执行的效果；模拟时不会执行。可以在这里改动任何存储，那会作为新的操作照常经过规则
 */
@FunctionalInterface
public interface StorageEffect
{
    /**
     * @param amount 这一步实际接收或交出的数量
     */
    void run(StorageRequest request, long amount);
}
