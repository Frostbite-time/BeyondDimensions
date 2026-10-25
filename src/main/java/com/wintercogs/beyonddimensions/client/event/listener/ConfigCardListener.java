package com.wintercogs.beyonddimensions.client.event.listener;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineItem;
import com.wintercogs.beyonddimensions.common.init.BDPackets;
import com.wintercogs.beyonddimensions.common.item.MachineConfigCardItem;
import com.wintercogs.beyonddimensions.network.packet.c2s.ConfigCardPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 配置卡的按键与提示：按住 Ctrl 指着方块机器时框出粘贴会作用到的全部机器
 */
@Mod.EventBusSubscriber(modid = BDConstants.MODID, value = Dist.CLIENT)
public class ConfigCardListener
{
    private static @Nullable BlockPos outlinedFrom;
    private static long outlinedTick;
    private static List<BlockPos> outlined = List.of();

    @SubscribeEvent
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event)
    {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND)
            return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator())
            return;
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof MachineConfigCardItem)
        {
            BlockPos pos = targetMachine(minecraft);
            if (pos == null)
                return;
            ConfigCardPacket.Operation operation = Screen.hasControlDown() ? ConfigCardPacket.Operation.PASTE_CONNECTED
                    : player.isShiftKeyDown() ? ConfigCardPacket.Operation.PASTE
                    : ConfigCardPacket.Operation.COPY;
            BDPackets.INSTANCE.sendToServer(ConfigCardPacket.block(operation, pos));
        }
        else if (main.getItem() instanceof MachineItem && player.getOffhandItem().getItem() instanceof MachineConfigCardItem)
        {
            BDPackets.INSTANCE.sendToServer(ConfigCardPacket.item(player.isShiftKeyDown()));
        }
        else
        {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(true);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !Screen.hasControlDown())
            return;
        ItemStack card = player.getMainHandItem();
        if (!(card.getItem() instanceof MachineConfigCardItem))
            return;
        BlockPos target = targetMachine(minecraft);
        if (target == null || !(player.level().getBlockEntity(target) instanceof Machine machine))
            return;

        List<BlockPos> machines = connected(player.level(), target);
        // 卡上的配置能粘贴到这类机器时为绿色，否则为红色
        boolean fits = MachineConfigCardItem.fits(card, machine.machineType());
        float red = fits ? 0.35F : 1.0F;
        float green = fits ? 1.0F : 0.35F;

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (BlockPos pos : machines)
            LevelRenderer.renderLineBox(pose, lines, new AABB(pos).inflate(0.004), red, green, 0.35F, 1.0F);
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static @Nullable BlockPos targetMachine(Minecraft minecraft)
    {
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
            return null;
        return minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof Machine ? hit.getBlockPos() : null;
    }

    // 相连的机器每刻最多找一次
    private static List<BlockPos> connected(Level level, BlockPos from)
    {
        long tick = level.getGameTime();
        if (!from.equals(outlinedFrom) || tick != outlinedTick)
        {
            outlined = MachineConfigCardItem.connected(level, from);
            outlinedFrom = from;
            outlinedTick = tick;
        }
        return outlined;
    }
}
