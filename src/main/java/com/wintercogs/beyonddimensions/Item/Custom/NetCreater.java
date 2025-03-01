package com.wintercogs.beyonddimensions.Item.Custom;


import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class NetCreater extends Item
{

    public NetCreater(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if(usedHand != InteractionHand.MAIN_HAND)
        {
            return InteractionResultHolder.fail(itemstack);
        }

        if(!level.isClientSide())
        {
            DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
            if (net != null)
            {
                return InteractionResultHolder.fail(itemstack);
            }
            String netId = DimensionsNet.buildNewNetName(player);
            String numId = netId.replace("BDNet_", "");
            DimensionsNet newNet = player.getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), netId);
            newNet.setId(Integer.parseInt(numId));
            newNet.setOwner(player.getUUID());
            newNet.addManager(player.getUUID());
            newNet.addPlayer(player.getUUID());
            newNet.setDirty();
            itemstack.consume(1,player);
        }

        return InteractionResultHolder.sidedSuccess(itemstack,level.isClientSide());
    }

}
