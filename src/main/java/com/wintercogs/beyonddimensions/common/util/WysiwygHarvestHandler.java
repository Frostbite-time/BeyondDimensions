package com.wintercogs.beyonddimensions.common.util;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@net.neoforged.fml.common.EventBusSubscriber(modid = BDConstants.MODID)
public final class WysiwygHarvestHandler
{
    private static final double MIN_VIEW_DOT = Math.cos(Math.toRadians(45));
    private static final long COOLDOWN_TICKS = 20;
    private static final String LAST_USE_TAG = "beyonddimensions_wysiwyg_last_use";
    private static final ThreadLocal<CaptureContext> DROP_CAPTURE = new ThreadLocal<>();

    private WysiwygHarvestHandler()
    {
    }

    public static void harvest(ServerPlayer player)
    {
        long gameTime = player.serverLevel().getGameTime();
        long lastUse = player.getPersistentData().getLong(LAST_USE_TAG);
        if (lastUse > 0 && gameTime - lastUse < COOLDOWN_TICKS)
            return;
        player.getPersistentData().putLong(LAST_USE_TAG, gameTime);

        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null)
        {
            player.displayClientMessage(Component.translatable("msg.beyonddimensions.wysiwyg_no_network"), true);
            return;
        }

        ServerLevel level = player.serverLevel();
        UnifiedStorage storage = net.getUnifiedStorage();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = ServerConfigRuntime.wysiwygRange;
        AABB bounds = player.getBoundingBox().expandTowards(look.scale(range)).inflate(range);
        CaptureContext capture = new CaptureContext(level, bounds, storage);
        DROP_CAPTURE.set(capture);

        int containers = 0;
        int blocks = 0;
        int entities = 0;
        try
        {
            for (BlockPos pos : BlockPos.betweenClosed(
                    (int) Math.floor(bounds.minX), (int) Math.floor(bounds.minY), (int) Math.floor(bounds.minZ),
                    (int) Math.floor(bounds.maxX), (int) Math.floor(bounds.maxY), (int) Math.floor(bounds.maxZ)))
            {
                if (blocks >= ServerConfigRuntime.wysiwygMaxBlocks)
                    break;
                if (!isVisible(player, eye, look, Vec3.atCenterOf(pos), range))
                    continue;

                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(level, pos) < 0)
                    continue;

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null && CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state).isCanceled())
                    continue;
                if (blockEntity != null && drainContainer(level, pos, blockEntity, storage))
                    containers++;
                if (player.gameMode.destroyBlock(pos))
                    blocks++;
            }

            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds, entity -> !(entity instanceof Player) && entity.isAlive()))
            {
                if (!isVisible(player, eye, look, entity.getBoundingBox().getCenter(), range))
                    continue;
                entity.hurt(level.damageSources().playerAttack(player), Float.MAX_VALUE);
                if (!entity.isAlive())
                    entities++;
            }
        }
        finally
        {
            DROP_CAPTURE.remove();
        }

        player.displayClientMessage(Component.translatable("msg.beyonddimensions.wysiwyg_result", blocks, entities, containers), true);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event)
    {
        CaptureContext capture = DROP_CAPTURE.get();
        if (capture == null || event.getEntity().level() != capture.level() || !capture.bounds().intersects(event.getEntity().getBoundingBox()))
            return;

        Iterator<ItemEntity> drops = event.getDrops().iterator();
        while (drops.hasNext())
        {
            ItemEntity itemEntity = drops.next();
            ItemStack remainder = insert(capture.storage(), itemEntity.getItem());
            if (remainder.isEmpty())
                drops.remove();
            else
                itemEntity.setItem(remainder);
        }
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        captureDrop(event);
    }

    private static boolean captureDrop(EntityJoinLevelEvent event)
    {
        CaptureContext capture = DROP_CAPTURE.get();
        if (capture == null || event.getLevel() != capture.level() || !(event.getEntity() instanceof ItemEntity itemEntity))
            return false;
        if (!capture.bounds().contains(itemEntity.position()))
            return false;

        ItemStack remainder = insert(capture.storage(), itemEntity.getItem());
        if (remainder.isEmpty())
        {
            event.setCanceled(true);
            return true;
        }
        itemEntity.setItem(remainder);
        return false;
    }

    private static boolean isVisible(Player player, Vec3 eye, Vec3 look, Vec3 target, double range)
    {
        Vec3 offset = target.subtract(eye);
        double distance = offset.length();
        return distance <= range && distance > 0 && look.dot(offset.scale(1.0 / distance)) >= MIN_VIEW_DOT;
    }

    private static boolean drainContainer(ServerLevel level, BlockPos pos, BlockEntity blockEntity, UnifiedStorage storage)
    {
        boolean found = false;
        Set<IItemHandler> handlers = new HashSet<>();
        for (Direction direction : Direction.values())
        {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
            if (handler != null)
                handlers.add(handler);
        }

        for (IItemHandler handler : handlers)
        {
            found = true;
            for (int slot = 0; slot < handler.getSlots(); slot++)
            {
                ItemStack available = handler.extractItem(slot, Integer.MAX_VALUE, true);
                if (available.isEmpty())
                    continue;
                int accepted = acceptedAmount(storage, available);
                if (accepted <= 0)
                    continue;
                ItemStack extracted = handler.extractItem(slot, accepted, false);
                if (!extracted.isEmpty())
                    insert(storage, extracted);
            }
        }

        if (!found && blockEntity instanceof Container container)
        {
            found = true;
            for (int slot = 0; slot < container.getContainerSize(); slot++)
            {
                ItemStack stack = container.getItem(slot);
                ItemStack remainder = insert(storage, stack);
                container.setItem(slot, remainder);
            }
            container.setChanged();
        }
        return found;
    }

    private static int acceptedAmount(UnifiedStorage storage, ItemStack stack)
    {
        KeyAmount remaining = storage.insert(new ItemStackKey(stack), stack.getCount(), true);
        return (int) Math.max(0, stack.getCount() - remaining.amount());
    }

    private static ItemStack insert(UnifiedStorage storage, ItemStack stack)
    {
        if (stack.isEmpty())
            return ItemStack.EMPTY;
        KeyAmount remaining = storage.insert(new ItemStackKey(stack), stack.getCount(), false);
        if (remaining.isEmpty())
            return ItemStack.EMPTY;
        ItemStack result = stack.copy();
        result.setCount((int) Math.min(remaining.amount(), stack.getMaxStackSize()));
        return result;
    }

    private record CaptureContext(ServerLevel level, AABB bounds, UnifiedStorage storage)
    {
    }
}
