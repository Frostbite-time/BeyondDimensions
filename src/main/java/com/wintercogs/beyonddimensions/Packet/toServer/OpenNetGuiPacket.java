package com.wintercogs.beyonddimensions.Packet.toServer;


import com.wintercogs.beyonddimensions.Packet.CustomPacket;

public record OpenNetGuiPacket(String uuid) implements CustomPacket<OpenNetGuiPacket>
{

    @Override
    public void handle()
    {

    }

    @Override
    public void encode()
    {

    }

    @Override
    public OpenNetGuiPacket decode()
    {
        return null;
    }
}
