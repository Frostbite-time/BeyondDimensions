package com.wintercogs.beyonddimensions.integration.module.create.contraption;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

public class NetInterfaceMovementBehaviour implements MovementBehaviour
{
    @Override
    public void tick(MovementContext context)
    {
        if (context.world.isClientSide())
        {
            return;
        }

        MountedItemStorage storage = context.getItemStorage();
        if (storage instanceof NetInterfaceMountedStorage netInterface)
        {
            netInterface.tickCoreTransfer();
        }
    }
}
