package com.wintercogs.beyonddimensions.common.machine.setting;

/**
 * 吸取的物品送到哪里：只送一处，或者先送一处、放不下的再送另一处
 */
public enum PickupTarget
{
    NETWORK_ONLY,
    NETWORK_FIRST,
    INVENTORY_FIRST,
    INVENTORY_ONLY
}
