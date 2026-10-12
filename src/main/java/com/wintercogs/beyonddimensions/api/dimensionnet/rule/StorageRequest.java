package com.wintercogs.beyonddimensions.api.dimensionnet.rule;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;

/**
 * 交给规则的一步请求
 */
public interface StorageRequest
{
    /**
     * 这一步处理的资源
     */
    IStackKey<?> key();

    /**
     * 这一步请求的数量
     */
    long amount();

    /**
     * 所在的维度网络
     */
    DimensionsNet net();

    /**
     * 网络里现有的数量，包含本次操作中尚未提交的变化
     */
    long stored(IStackKey<?> key);
}
