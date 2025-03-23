package com.wintercogs.beyonddimensions.Packet;

import net.minecraft.network.protocol.Packet;

public interface CustomPacket<T>
{
    void handle();

    void encode();

    T decode();

}
