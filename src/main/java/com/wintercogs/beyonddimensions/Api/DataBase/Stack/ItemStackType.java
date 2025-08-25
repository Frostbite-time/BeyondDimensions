package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Api.Util.NbtEq;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

public class ItemStackType implements IStackType<ItemStack> {
    public static final ResourceLocation ID = ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/item");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    // 这四个为内部真实数据存储
    // 之所以不直接使用ItemStack，是为了防止某些模组通过事件监听对ItemStack执行修改，导致网络传输前后NBT不同步
    // 流体一般不会出现此情况，因此只修改物品
    private Item item;
    private @Nullable CompoundTag tag; // 内部nbt
    private @Nullable CompoundTag caps; // forge能力
    private long stackSize;

    private ItemStack serverCache = null;
    private ItemStack clientCache = null;
    private int vanillaStackSize = -1; // 缓存原版堆叠大小

    private int hashCodeCache = 0; // 哈希码缓存
    private boolean NeedRecalHash = true; // 指示什么时候需要重新计算哈希

    // 这里借用了AE2 api的代码用来加速能力比较
    // https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/forge/1.20.1/src/main/java/appeng/api/stacks/AEItemKey.java
    private static final MethodHandle SERIALIZE_CAPS_HANDLE;
    private static final MethodHandle DESERIALIZE_CAPS_HANDLE;
    static {
        try {
            var method = CapabilityProvider.class.getDeclaredMethod("serializeCaps");
            method.setAccessible(true);
            SERIALIZE_CAPS_HANDLE = MethodHandles.lookup().unreflect(method);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create serializeCaps method handle", exception);
        }
        try {
            var method = CapabilityProvider.class.getDeclaredMethod("deserializeCaps",CompoundTag.class);
            method.setAccessible(true);
            DESERIALIZE_CAPS_HANDLE = MethodHandles.lookup().unreflect(method);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create deserializeCaps method handle", exception);
        }
    }

    @Nullable
    public static CompoundTag serializeStackCaps(ItemStack stack) {
        try {
            var caps = (CompoundTag) SERIALIZE_CAPS_HANDLE.invokeExact((CapabilityProvider) stack);
            // Ensure stacks with no serializable cap providers are treated the same as stacks with no caps!
            return caps == null || caps.isEmpty() ? null : caps;
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to call serializeCaps", ex);
        }
    }

    @Nullable
    public static void deserializeStackCaps(ItemStack stack, CompoundTag caps) {
        try {
            if(caps != null && !caps.isEmpty())
            {
                DESERIALIZE_CAPS_HANDLE.invokeExact((CapabilityProvider) stack, caps);
            }
            return ;
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to call serializeCaps", ex);
        }
    }

    public ItemStackType()
    {
        this.item = Items.AIR;
        stackSize = 0;
    }

    public ItemStackType(ItemStack stack)
    {
        this.item = stack.getItem();
        this.tag = stack.tag == null ? null : stack.tag.copy();
        CompoundTag caps = serializeStackCaps(stack);
        this.caps = caps == null ? null : caps.copy();
        stackSize = stack.getCount();
    }

    public ItemStackType(ItemStack stack, long stackSize)
    {
        this.item = stack.getItem();
        this.tag = stack.tag == null ? null : stack.tag.copy();
        CompoundTag caps = serializeStackCaps(stack);
        this.caps = caps == null ? null : caps.copy();
        this.stackSize = stackSize;
    }

    // 用来快速复制 反序列化等
    public ItemStackType(Item item, long stackSize, CompoundTag tag, CompoundTag caps)
    {
        this.item = item;
        this.tag = tag == null ? null : tag.copy();
        this.caps = caps == null ? null : caps.copy();
        this.stackSize = stackSize;
    }

    private void refreshCachedStack() //refresh不负责数量正确，需要时自行修正数量
    {
        serverCache = new ItemStack(item, 1, caps == null ? null : caps.copy());
        serverCache.tag = tag == null ? null : tag.copy();
        clientCache = new ItemStack(item, 1, caps == null ? null : caps.copy());
        clientCache.tag = tag == null ? null : tag.copy();
    }

    @Override
    public IStackType<ItemStack> fromObject(Object key, long amount, CompoundTag dataComponentPatch)
    {
        // 先行置为ItemStack，再走统一接口
        if(key instanceof Item item)
        {
            ItemStack itemStack;
            itemStack = new ItemStack(item, 1);
            if(dataComponentPatch != null)
                itemStack.tag = dataComponentPatch;
            return new ItemStackType(itemStack,amount);
        }
        return null;
    }

    @Override
    public ItemStack getStack()
    {
        if(serverCache == null)
            refreshCachedStack();

        serverCache.setCount(BDMath.clampLongToInt(stackSize));
        return serverCache;
    }

    @Override
    public void setStack(ItemStack stack)
    {
        this.item = stack.getItem();
        this.tag = stack.tag == null ? null : stack.tag.copy();
        CompoundTag caps = serializeStackCaps(stack);
        this.caps = caps == null ? null : caps.copy();
        this.stackSize = stack.getCount();

        NeedRecalHash = true;
        refreshCachedStack();
        vanillaStackSize = -1; // 强制重新计算
    }

    @Override
    public ResourceLocation getTypeId() {
        return ID;
    }

    @Override
    public IStackType<ItemStack> getEmpty()
    {
        return new ItemStackType();
    }

    @Override
    public Class<ItemStack> getStackClass() {
        return ItemStack.class;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Item.class;
    }

    @Override
    public Object getSource()
    {
        return ItemStack.EMPTY.getItem();
    }

    @Override
    public String getModId()
    {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null ? key.getNamespace() : "unknown";
    }

    @Override
    public boolean isEmpty()
    {
        return item == Items.AIR || stackSize <= 0;
    }

    @Override
    public boolean isEmptyStack()
    {
        return item == Items.AIR;
    }

    @Override
    public ItemStack getEmptyStack()
    {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack copyStack()
    {
        ItemStack copy = new ItemStack(item, BDMath.clampLongToInt(stackSize), caps == null ? null : caps.copy());
        copy.tag = tag == null ? null : tag.copy();
        return copy;
    }

    @Override
    public ItemStack copyStackWithCount(long count)
    {
        ItemStack copy = new ItemStack(item, BDMath.clampLongToInt(count), caps == null ? null : caps.copy());
        copy.tag = tag == null ? null : tag.copy();
        return copy;
    }

    @Override
    public IStackType<ItemStack> copy()
    {
        Item copyItem = item;
        CompoundTag copyTag = tag == null ? null : tag.copy();
        CompoundTag copyCaps = caps == null ? null : caps.copy();
        long copyStackSize = stackSize;

        ItemStackType copy = new ItemStackType(copyItem,copyStackSize,copyTag,copyCaps);

        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<ItemStack> copyWithCount(long count)
    {
        Item copyItem = item;
        CompoundTag copyTag = tag == null ? null : tag.copy();
        CompoundTag copyCaps = caps == null ? null : caps.copy();
        long copyStackSize = count;

        ItemStackType copy = new ItemStackType(copyItem,copyStackSize,copyTag,copyCaps);

        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public long getStackAmount()
    {
        return stackSize;
    }

    @Override
    public void setStackAmount(long amount)
    {
        stackSize = amount;
    }

    @Override
    public void grow(long amount)
    {
        setStackAmount(getStackAmount()+amount);
    }

    @Override
    public void shrink(long amount)
    {
        grow(-amount);
    }

    @Override
    public long getVanillaMaxStackSize() {
        if(vanillaStackSize<=0)
        {
            if(serverCache == null)
                refreshCachedStack();
            vanillaStackSize = serverCache.getMaxStackSize();
        }
        return Math.min(vanillaStackSize, getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize() {
        // 可配置化的最大堆叠尺寸
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public ItemStack splitStack(long amount) {
        if (amount <= 0) return ItemStack.EMPTY;

        int splitAmount = BDMath.clampLongToInt(Math.min(amount, stackSize));
        ItemStack split = copyStack();
        split.setCount(splitAmount);
        shrink(splitAmount);
        return split;
    }

    @Override
    public IStackType<ItemStack> split(long amount)
    {
        if (amount <= 0) return new ItemStackType();

        long splitAmount = Math.min(amount, stackSize);
        ItemStackType split = (ItemStackType)copyWithCount(splitAmount);
        shrink(splitAmount);
        return split;
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        if (tagKey == null) return false;
        if (this.item == null || this.item == Items.AIR) return false;

        if (!tagKey.isFor(Registries.ITEM)) {
            return false;
        }

        TagKey<Item> itemTag = (TagKey<Item>) tagKey;
        return item.builtInRegistryHolder().is(itemTag);
    }

    @Override
    public boolean isSame(IStackType<?> other) {
        // 比较物品类型和基础NBT（如盔甲耐久等）
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return item == ((ItemStackType)other).item;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<?> other) {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        if(other instanceof ItemStackType otherStackType)
        {
            return otherStackType.item == item
                    && NbtEq.equalsRelaxed(this.tag, otherStackType.tag)
                    && NbtEq.equalsRelaxed(this.caps, otherStackType.caps);
        }
        return false;
    }

    // 网络序列化
    @Override
    public void serialize(FriendlyByteBuf buf) {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId());

        // 写入是否存在物品的标志
        boolean hasItem = item != Items.AIR;
        buf.writeBoolean(hasItem);

        if (hasItem) {
            buf.writeId(BuiltInRegistries.ITEM, item);
            buf.writeVarLong(stackSize);
            buf.writeNbt(tag);  //writeNbt会自己处理空占位，无需担心
            buf.writeNbt(caps);
        }
    }

    @Override
    public ItemStackType deserialize(FriendlyByteBuf buf,ResourceLocation typeId) {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }

        // 读取是否存在物品的标志
        boolean hasItem = buf.readBoolean();
        if (!hasItem) {
            return new ItemStackType();
        }

        Item item = (Item)buf.readById(BuiltInRegistries.ITEM);
        long count = buf.readVarLong();
        CompoundTag tag = buf.readNbt();
        CompoundTag caps = buf.readNbt();

        return new ItemStackType(item, count, tag, caps);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        tag.putLong("Amount", getStackAmount());

        CompoundTag stackTag = new CompoundTag();
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(item);
        stackTag.putString("id", resourcelocation == null ? "minecraft:air" : resourcelocation.toString());
        if (this.tag != null) {
            stackTag.put("tag", this.tag.copy());
        }
        if (this.caps != null && !this.caps.isEmpty()) { // 序列化时自动清除空能力（此操作与原版一致）
            stackTag.put("ForgeCaps", this.caps);
        }

        tag.put("Stack",stackTag);
        return tag;
    }

    @Override
    public ItemStackType deserializeNBT(CompoundTag nbt) {
        long amount = nbt.getLong("Amount"); // 数量直接从原始nbt中取

        CompoundTag stackTag = nbt.getCompound("Stack");
        CompoundTag caps = stackTag.contains("ForgeCaps") ? stackTag.getCompound("ForgeCaps") : null;
        CompoundTag tag = stackTag.contains("tag") ? stackTag.getCompound("tag") : null;
        Item rawItem = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(stackTag.getString("id")));
        Item item = ForgeRegistries.ITEMS.getDelegateOrThrow(rawItem).get();
        return new ItemStackType(item,amount,tag,caps);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui,int x, int y) {

        if(clientCache == null)
            refreshCachedStack();

        // 渲染物品图标
        var poseStack = gui.pose(); // 获取渲染的变换矩阵
        poseStack.pushPose(); // 保存矩阵状态
        clientCache.setCount(1); //设置一次数量，以防万一
        gui.renderFakeItem(clientCache, x, y);
        gui.renderItemDecorations(Minecraft.getInstance().font, clientCache, x, y, "");
        poseStack.popPose(); // 恢复矩阵状态，结束渲染

        // 渲染数量文本
        String countText = getCountText(getStackAmount());
        float scale = 0.666f; // 文本缩放因数
        var poseStackText = gui.pose();
        poseStackText.pushPose();
        poseStackText.translate(0,0,200); // 确保文本在顶层
        poseStackText.scale(scale,scale,scale); // 文本整体缩放，便于查看
        RenderSystem.disableBlend(); // 禁用混合渲染模式
        final int X = (int)(
                (x + -1 + 16.0f + 2.0f - Minecraft.getInstance().font.width(countText) * 0.666f)
                        * 1.0f / 0.666f
        );
        final int Y = (int)(
                (y + -1 + 16.0f - 5.0f * 0.666f)
                        * 1.0f / 0.666f
        );
        if(!clientCache.isEmpty())
            gui.drawString(Minecraft.getInstance().font, countText, X, Y, 0xFFFFFF);
        poseStackText.popPose();
    }

    @Override
    public String getCountText(long count) {
        if (count < 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName()
    {
        if(clientCache == null)
            refreshCachedStack();
        return clientCache.getDisplayName();
    }

    @Override
    public List<Component> getTooltipLines(@Nullable Player player, TooltipFlag tooltipFlag)
    {
        if(clientCache == null)
            refreshCachedStack();
        List<Component> tooltips = clientCache.getTooltipLines(player,tooltipFlag);
        tooltips.add(Component.translatable("istack.beyonddimensions.storage_num.item", getStackAmount()));
        return tooltips;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage()
    {
        if(clientCache == null)
            refreshCachedStack();
        return clientCache.getTooltipImage();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(net.minecraft.client.gui.GuiGraphics gui,net.minecraft.client.gui.Font font, int mouseX, int mouseY)
    {
        var minecraft = Minecraft.getInstance();
        gui.renderTooltip(minecraft.font, this.getTooltipLines(minecraft.player, minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)
                , getTooltipImage(), ItemStack.EMPTY, mouseX, mouseY);
    }

    @Override
    public boolean equals(Object other)
    {
        // 我知道我没做引用比较，等我统一整理代码时再处理
        if(other instanceof ItemStackType otherStack)
        {
            return this.isSameTypeSameComponents(otherStack);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // 基于物品类型和组件生成哈希码
        if(NeedRecalHash)
        {
            int i = 31 + item.hashCode();
            i = i * 31 + NbtEq.hashRelaxed(tag); // 内部已处理null
            i = i * 31 + NbtEq.hashRelaxed(caps);
            hashCodeCache = i;
            NeedRecalHash = false;
        }
        return hashCodeCache;
    }
}

