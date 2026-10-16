package com.wintercogs.beyonddimensions.common.machine.setting;

/**
 * 吸取的物品送到哪里：只送一处，或者先送一处、放不下的再送另一处
 */
public enum PickupTarget
{
    NETWORK_ONLY,
    NETWORK_FIRST,
    INVENTORY_FIRST,
    INVENTORY_ONLY;

    /**
     * 是否先送网络
     */
    public boolean prefersNetwork()
    {
        return this == NETWORK_ONLY || this == NETWORK_FIRST;
    }

    /**
     * 网络与背包互换后的去向，“仅”与“优先”不变；磁铁的例外物品按它送
     */
    public PickupTarget opposite()
    {
        return switch (this)
        {
            case NETWORK_ONLY -> INVENTORY_ONLY;
            case NETWORK_FIRST -> INVENTORY_FIRST;
            case INVENTORY_FIRST -> NETWORK_FIRST;
            case INVENTORY_ONLY -> NETWORK_ONLY;
        };
    }
}
