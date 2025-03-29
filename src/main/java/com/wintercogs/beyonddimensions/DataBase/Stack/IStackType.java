package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

// 用于定义不同stack的行为 物品 流体 以及其他模组中行为逻辑stack相似的资源
// 实现还需重写hashcode以及equals方法，使其检测忽略数量以用于其他位置的代码
public interface IStackType<T> {

    IStackType<T> fromObject(Object key, long amount, int meta,NBTTagCompound dataComponentPatch);

    // 类型的唯一标识符 如(beyonddimension:stack_type/item) beyonddimension为本modID，提供对原版Item的支持，Item为要支持的Stack类型
    ResourceLocation getTypeId();

    IStackType<T> getEmpty();

    // 返回直接引用，以降低性能开销 如果可能修改stack 使用copyStack方法
    T getStack();

    void setStack(T stack); // 用于适应工厂方式来新建实例

    // 获取类型
    Class<T> getStackClass();

    // 获取根类型 如ItemStack 返回Item的class
    Class<?> getSourceClass();

    // 获取根 如如ItemStack 直接返回Item 用于检查继承链
    Object getSource();

    // 新增方法：判断堆栈是否为空（如ItemStack.isEmpty()）
    boolean isEmpty();

    // 空实例（例如：ItemStack.EMPTY）
    T getEmptyStack();

    // 新增方法：复制堆栈
    T copyStack();

    // 按数量复制堆叠
    T copyStackWithCount(long count);

    IStackType<T> copy();

    IStackType<T> copyWithCount(long count);


    // 获取stack当前的数量
    long getStackAmount();

    void setStackAmount(long amount);

    void grow(long amount);

    void shrink(long amount);

    // 单个堆叠的最大容量
    long getVanillaMaxStackSize();

    // 获取期望的单个堆叠最大容量
    long getCustomMaxStackSize();

    T splitStack(long amount);

    IStackType<T>  split(long amount); // 分割出指定数量的堆叠

    boolean isSame(IStackType<T> other); // 检查两个堆叠除数量和组件外的一切是否相同，例如如果是物品 就只检查物品类型是否相同

    boolean isSameTypeSameComponents(IStackType<T> other); // 检查两个堆叠除数量外的一切是否相同，例如如果是物品 就检查物品类型和所有组件是否相同

    // 序列化/反序列化（用于网络和NBT）
    void serialize(PacketBuffer buf);
    IStackType<T> deserialize(PacketBuffer buf, ResourceLocation typeId);

    // 传入一个buf，将会自动遍历所有可能的实现，并调用其接口，返回不为null的结果
    static IStackType deserializeCommon(PacketBuffer buf)
    {
        ResourceLocation typeId = buf.readResourceLocation();
        for(IStackType stacktype : StackTypeRegistry.getAllTypes())
        {
            IStackType stack = stacktype.deserialize(buf,typeId);
            if(stack!=null)
            {
                return stack;
            }
        }

        return null;
    }

    // 新增方法：NBT序列化（用于磁盘存储）
    NBTTagCompound serializeNBT();
    IStackType<T> deserializeNBT(NBTTagCompound nbt);


    // UI渲染（在指定位置绘制图标和数量）
    @SideOnly(Side.CLIENT)
    void render(int x, int y);

    String getCountText(long count);

    String getDisplayName();

    List<String> getTooltipLines(@Nullable EntityPlayer player, ITooltipFlag advanced);

    // 1.12.2不支持 取消此接口
    //Optional<TooltipComponent> getTooltipImage();

    @SideOnly(Side.CLIENT)
    void renderTooltip(int mouseX, int mouseY);

}
