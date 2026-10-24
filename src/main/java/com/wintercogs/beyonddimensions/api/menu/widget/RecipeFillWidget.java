package com.wintercogs.beyonddimensions.api.menu.widget;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 能按配方查看器给出的原料填充的部件。BD 的 JEI 与 EMI 插件用它填充合成台、铁砧、锻造台与切石机；
 * 附属的插件可以用 {@code BDMenuCommands#recipe} 请服务器填充自己的工作台
 */
public interface RecipeFillWidget
{
    /**
     * 要填充的输入槽，按原料的顺序；配方查看器据此核算原料，这些槽里已有的物品也算作可用的原料
     */
    List<Slot> inputSlots();

    /**
     * 在服务端按原料填充：已有的物品先按退回方向清出去，每格的原料先从背包取，再从存储取。
     * 请求来自客户端，实现要先校验。
     *
     * @param keys     每个输入槽的原料，按槽位顺序
     * @param amounts  每个输入槽要放的数量
     * @param compress 原料多于格数时是否打包进物质压缩球；不支持时忽略
     * @param recipe   配方的 ID，例如切石机据此选中配方；没有时为 null
     */
    boolean fillRecipe(List<IStackKey<?>> keys, List<Long> amounts, boolean compress, @Nullable ResourceLocation recipe);
}
