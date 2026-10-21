package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSettings;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.machine.SettingEditor;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import com.wintercogs.beyonddimensions.common.component.MachineConfigCardContents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 机器配置卡：从一台机器复制它导出的配置，粘贴到同一类机器上。
 * <p>
 * 方块机器：手持卡右键复制，Shift 右键粘贴，Ctrl 右键粘贴到与它相连的全部同类机器。
 * 物品机器：卡在副手、机器在主手，右键复制，Shift 右键粘贴
 */
public class MachineConfigCardItem extends Item
{
    /**
     * 一次粘贴到相连机器时最多处理的台数
     */
    public static final int CONNECTED_LIMIT = 256;

    /**
     * 卡上配置的大小上限，按 NBT 的内存估算计。超过上限时整张不复制：
     * 过大的卡在网络上读不出来，持卡玩家与周围的玩家都会断线
     */
    public static final int MAX_CONFIG_BYTES = 256 * 1024;

    // 物品 NBT 中保存卡上配置的键
    private static final String CONTENTS = "machine_config_card";

    public MachineConfigCardItem(Properties properties)
    {
        super(properties);
    }

    public static @Nullable MachineConfigCardContents contents(ItemStack card)
    {
        CompoundTag tag = card.getTag();
        if (tag == null || !tag.contains(CONTENTS, Tag.TAG_COMPOUND))
            return null;
        return MachineConfigCardContents.fromTag(tag.getCompound(CONTENTS));
    }

    /**
     * 卡上的配置能否粘贴到这一类机器
     */
    public static boolean fits(ItemStack card, MachineType type)
    {
        MachineConfigCardContents contents = contents(card);
        return contents != null && contents.type() == type;
    }

    public static void copy(Player player, Machine machine, ItemStack card)
    {
        ResourceLocation id = BDRegistries.MACHINE_TYPES.get().getKey(machine.machineType());
        if (id == null)
            return;
        CompoundTag config = new CompoundTag();
        machine.exportConfig(config);
        int size = config.sizeInBytes();
        if (size > MAX_CONFIG_BYTES)
        {
            feedback(player, Component.translatable("msg.beyonddimensions.machine_config_card.too_large", size / 1024, MAX_CONFIG_BYTES / 1024));
            return;
        }
        card.getOrCreateTag().put(CONTENTS, new MachineConfigCardContents(id, config).toTag());
        feedback(player, Component.translatable("msg.beyonddimensions.machine_config_card.copied", machine.machineType().displayName()));
    }

    /**
     * 把卡上的配置粘贴到 machines；卡是空的或属于另一类机器时提示玩家，返回 false
     */
    public static boolean paste(Player player, List<? extends Machine> machines, ItemStack card)
    {
        if (machines.isEmpty())
            return false;
        MachineType type = machines.get(0).machineType();
        MachineConfigCardContents contents = contents(card);
        if (contents == null)
        {
            feedback(player, Component.translatable("msg.beyonddimensions.machine_config_card.empty"));
            return false;
        }
        if (contents.type() != type)
        {
            feedback(player, Component.translatable("msg.beyonddimensions.machine_config_card.mismatch", cardMachineName(contents), type.displayName()));
            return false;
        }
        CompoundTag config = contents.config();
        for (Machine machine : machines)
            machine.applyConfig(config.copy());
        feedback(player, machines.size() == 1
                ? Component.translatable("msg.beyonddimensions.machine_config_card.pasted", type.displayName())
                : Component.translatable("msg.beyonddimensions.machine_config_card.pasted_connected", machines.size(), type.displayName()));
        return true;
    }

    /**
     * 从 start 出发，经六个面相邻、与它同属一类的方块机器，包括 start 自己，最多 {@link #CONNECTED_LIMIT} 台
     */
    public static List<BlockPos> connected(Level level, BlockPos start)
    {
        if (!(level.getBlockEntity(start) instanceof Machine first))
            return List.of();
        MachineType type = first.machineType();
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && found.size() < CONNECTED_LIMIT)
        {
            BlockPos pos = queue.poll();
            found.add(pos);
            for (Direction direction : Direction.values())
            {
                BlockPos next = pos.relative(direction);
                if (seen.add(next) && level.isLoaded(next)
                        && level.getBlockEntity(next) instanceof Machine machine && machine.machineType() == type)
                    queue.add(next);
            }
        }
        return found;
    }

    private static Component cardMachineName(MachineConfigCardContents contents)
    {
        MachineType type = contents.type();
        return type != null ? type.displayName() : Component.literal(contents.machineType().toString());
    }

    private static void feedback(Player player, Component message)
    {
        player.displayClientMessage(message, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3F, 1.0F);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tooltip, flag);
        MachineConfigCardContents contents = contents(stack);
        if (contents == null)
        {
            tooltip.add(Component.translatable("tooltip.beyonddimensions.machine_config_card.empty").withStyle(ChatFormatting.GRAY));
        }
        else
        {
            tooltip.add(Component.translatable("tooltip.beyonddimensions.machine_config_card.machine", cardMachineName(contents)).withStyle(ChatFormatting.AQUA));
            MachineType type = contents.type();
            CompoundTag config = contents.config();
            if (type != null && config.contains(Machine.SETTINGS, Tag.TAG_COMPOUND))
            {
                MachineSettings settings = MachineSettings.fromTag(config.getCompound(Machine.SETTINGS));
                for (MachineType.Entry<?> entry : type.settings())
                    tooltip.add(settingLine(entry, settings));
            }
        }
        tooltip.add(Component.translatable("tooltip.beyonddimensions.machine_config_card.block").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.beyonddimensions.machine_config_card.connected").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.beyonddimensions.machine_config_card.item").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static <T> Component settingLine(MachineType.Entry<T> entry, MachineSettings settings)
    {
        T value = entry.valueIn(settings);
        Component shown;
        if (entry.editor() instanceof SettingEditor.Choice<T> choice)
            shown = Component.translatable(choice.optionLabel().apply(value));
        else if (entry.editor() instanceof SettingEditor.OnOff<T> onOff)
            shown = Component.translatable(value.equals(onOff.on())
                    ? "tooltip.beyonddimensions.machine_config_card.on"
                    : "tooltip.beyonddimensions.machine_config_card.off");
        else if (entry.editor() instanceof SettingEditor.NumberField<T> number)
            shown = Component.literal(Long.toString(number.toLong().applyAsLong(value)));
        else
            throw new IllegalStateException("Unknown setting editor: " + entry.editor());
        return Component.translatable("tooltip.beyonddimensions.machine_config_card.setting", Component.translatable(entry.labelKey()), shown)
                .withStyle(ChatFormatting.GRAY);
    }
}
