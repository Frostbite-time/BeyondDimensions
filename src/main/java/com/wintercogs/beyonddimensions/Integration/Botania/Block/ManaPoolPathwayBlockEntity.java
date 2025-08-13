package com.wintercogs.beyonddimensions.Integration.Botania.Block;

import com.google.common.base.Predicates;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.ManaUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetedBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lwjgl.opengl.GL11;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.BotaniaAPIClient;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.BotaniaForgeClientCapabilities;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.block.Wandable;
import vazkii.botania.api.internal.ManaBurst;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.mana.*;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.api.mana.spark.SparkAttachable;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.client.fx.WispParticleData;
import vazkii.botania.client.gui.HUDHandler;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.block.block_entity.mana.BellowsBlockEntity;
import vazkii.botania.common.handler.ManaNetworkHandler;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.common.item.ManaTabletItem;
import vazkii.botania.xplat.XplatAbstractions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity.*;
import static vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity.PARTICLE_COLOR_BLUE;

public class ManaPoolPathwayBlockEntity extends NetedBlockEntity implements ManaCollector, ManaPool, SparkAttachable, Wandable
{
    private ManaUnifiedStorageHandler handler = null;
    private boolean isOutPutting = true;

    private final Int2ObjectMap<MutableInt> chargingParticles = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<MutableInt> drainingParticles = new Int2ObjectOpenHashMap<>();
    private static final float CHARGING_GRAVITY = 0.003f;

    public ManaPoolPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.MANA_POOL_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
        refreshHandler();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
    {
        if(cap == BotaniaForgeCapabilities.MANA_RECEIVER)
        {
            return LazyOptional.of(() -> this).cast();
        }
        if(cap == BotaniaForgeCapabilities.SPARK_ATTACHABLE)
        {
            return LazyOptional.of(() -> this).cast();
        }
        if(cap == BotaniaForgeClientCapabilities.WAND_HUD)
        {
            return LazyOptional.of(() -> new WandHud(this)).cast();
        }
        if(cap == BotaniaForgeCapabilities.WANDABLE)
        {
            return LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(cap, side);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ManaPoolPathwayBlockEntity be)
    {
        double particleChance = 1F - 0.1;
        if (Math.random() > particleChance) {
            WispParticleData data = WispParticleData.wisp((float) Math.random() / 3F,
                    PARTICLE_COLOR_RED, PARTICLE_COLOR_GREEN, PARTICLE_COLOR_BLUE, 2F);
            level.addParticle(data, pos.getX() + 0.3 + Math.random() * 0.5,
                    pos.getY() + 0.6 + Math.random() * 0.25, pos.getZ() + Math.random(),
                    0, (float) Math.random() / 25F, 0);
        }

        displayChargingParticles(level, pos, be, be.chargingParticles, true);
        displayChargingParticles(level, pos, be, be.drainingParticles, false);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaPoolPathwayBlockEntity be)
    {
        be.initManaCapAndNetwork();

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
        for (ItemEntity item : items) {
            if (!item.isAlive()) {
                continue;
            }

            ItemStack stack = item.getItem();
            ManaItem mana = XplatAbstractions.INSTANCE.findManaItem(stack);
            if (!stack.isEmpty() && mana != null) {
                if (be.isOutPutting && mana.canReceiveManaFromPool(be) || !be.isOutPutting && mana.canExportManaToPool(be)) {

                    int transfRate = Integer.MAX_VALUE;

                    if (be.isOutPutting) //输出到石板
                    {
                        if (be.getCurrentMana() > 0 && mana.getMana() < mana.getMaxMana())
                        {
                            int manaVal = Math.min(transfRate, Math.min(be.getCurrentMana(), mana.getMaxMana() - mana.getMana()));
                            mana.addMana(manaVal);
                            be.receiveMana(-manaVal);
                        }
                    }
                    else // 从石板接收
                    {
                        if (mana.getMana() > 0 && !be.isFull()) {
                            int manaVal = Math.min(transfRate, Math.min(BDMath.clampLongToInt(be.getActualMaxMana() - be.getActualCurrentMana()), mana.getMana()));
                            if (manaVal == 0 && be.level.getBlockState(pos.below()).is(BotaniaBlocks.manaVoid)) {
                                manaVal = Math.min(transfRate, mana.getMana());
                            }
                            mana.addMana(-manaVal);
                            be.receiveMana(manaVal);
                        }
                    }
                }
            }
        }
    }

    private static void displayChargingParticles(Level level, BlockPos worldPosition, ManaPoolPathwayBlockEntity be,
                                                 Int2ObjectMap<MutableInt> particles, boolean charging) {
        int bellowCount = charging ? getBellowCount(level, worldPosition, be) : 0;
        float relativeMana = (float) be.getCurrentMana() / be.getMaxMana();
        var particlesIterator = particles.int2ObjectEntrySet().iterator();
        while (particlesIterator.hasNext()) {
            var entry = particlesIterator.next();
            int ticksRemaining = entry.getValue().decrementAndGet();
            if (ticksRemaining % 2 == 0) {
                int encodedPos = entry.getIntKey();
                Vec3 itemPosRelBase = decodeRelativeItemPosition(encodedPos, relativeMana);
                if (charging) {
                    for (int i = 0; i <= bellowCount; i++) {
                        Vec3 itemPosRel = randomizeItemPos(itemPosRelBase);
                        Vec3 poolPosRel = new Vec3(0.1 + 0.8 * Math.random(), 0.1 + 0.4 * relativeMana,
                                0.1 + 0.8 * Math.random());
                        addManaFlowParticle(level, worldPosition, poolPosRel, itemPosRel);
                    }
                } else {
                    Vec3 itemPosRel = randomizeItemPos(itemPosRelBase);
                    Vec3 poolPosRel =
                            new Vec3(0.05 + 0.9 * Math.random(), 0.35 * relativeMana, 0.05 + 0.9 * Math.random());
                    addManaFlowParticle(level, worldPosition, itemPosRel, poolPosRel);
                }
            }
            if (ticksRemaining <= 0) {
                particlesIterator.remove();
            }
        }
    }

    private static int getBellowCount(Level level, BlockPos worldPosition, ManaPoolPathwayBlockEntity be) {
        int bellowCount = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockEntity tile = level.getBlockEntity(worldPosition.relative(dir));
            if (tile instanceof BellowsBlockEntity bellows && bellows.getLinkedTile() == be) {
                bellowCount++;
            }
        }
        return bellowCount;
    }

    private static Vec3 decodeRelativeItemPosition(int param, float relativeMana) {
        int compressedX = param & 0x7;
        int compressedY = param >> 3 & 0x3;
        int compressedZ = param >> 5 & 0x7;

        double relX = compressedX / 7.0;
        double relY = 1.0 - (14.0 / 16.0) / (1 << compressedY);
        double relZ = compressedZ / 7.0;

        return new Vec3(relX, Math.max(relY, 0.5 * relativeMana), relZ);
    }

    private static Vec3 randomizeItemPos(Vec3 itemPosRelBase) {
        return itemPosRelBase.add(0.1 * Math.random() - 0.05, 0.1 * Math.random() + 0.25, 0.1 * Math.random() - 0.05);
    }

    private static void addManaFlowParticle(Level level, BlockPos worldPosition, Vec3 startPos, Vec3 endPos) {
        double maxHeight = Math.max(startPos.y, endPos.y) - endPos.y + 0.05 * Math.random();
        Vec3 horizontalDiff = new Vec3(endPos.x - startPos.x, 0, endPos.z - startPos.z);
        double horizontalDistance = horizontalDiff.horizontalDistance();
        Vec3 horizontalDir = horizontalDiff.scale(1 / horizontalDistance);
        double startHeight = startPos.y - endPos.y;
        double vY0Squared = 2 * CHARGING_GRAVITY * (maxHeight - startHeight);
        double vY0 = Math.sqrt(vY0Squared);
        double lifetime = (vY0 + Math.sqrt(vY0Squared + 2 * CHARGING_GRAVITY * startHeight)) / CHARGING_GRAVITY;
        double vX0 = horizontalDistance / lifetime;
        Vec3 v0 = horizontalDir.scale(vX0).with(Direction.Axis.Y, vY0);

        WispParticleData data = WispParticleData.wisp(0.1f, PARTICLE_COLOR_RED, PARTICLE_COLOR_GREEN,
                PARTICLE_COLOR_BLUE, (float) (0.025 * lifetime), CHARGING_GRAVITY).withNoClip(true);
        level.addParticle(data, worldPosition.getX() + startPos.x, worldPosition.getY() + startPos.y,
                worldPosition.getZ() + startPos.z, v0.x, v0.y, v0.z);
    }


    private void initManaCapAndNetwork() {
        if (!ManaNetworkHandler.instance.isPoolIn(level, this) && !isRemoved()) {
            BotaniaAPI.instance().getManaNetworkInstance().fireManaNetworkEvent(this, ManaBlockType.POOL, ManaNetworkAction.ADD);
        }
    }

    public boolean refreshHandler()
    {
        // net存在、handler存在、且存储的引用一致，跳过刷新，返回真
        if(getNet() != null && handler != null && getNet().getUnifiedStorage() == handler.getStorage())
        {
            return true;
        }
        else if(getNet() != null) // 上述任一不存在，则刷新
        {
            handler = new ManaUnifiedStorageHandler(getNet().getUnifiedStorage(), new CapCtx(level,getBlockPos(),null,this));
            return true;
        }
        return false; // net不存在则false
    }

    public long getActualCurrentMana()
    {
        if(refreshHandler())
            return handler.getActualCurrentMana();
        else
            return 0;
    }

    public long getActualMaxMana()
    {
        if(refreshHandler())
            return handler.getActualMaxMana();
        else
            return 0;
    }


    @Override
    public void onClientDisplayTick()
    {

    }

    @Override
    public float getManaYieldMultiplier(ManaBurst burst)
    {
        return 1f;
    }

    @Override
    public boolean isOutputtingPower()
    {
        return isOutPutting;
    }

    @Override
    public int getMaxMana()
    {
        if(refreshHandler())
            return handler.getMaxMana();
        else
            return 0;
    }

    @Override
    public Optional<DyeColor> getColor()
    {
        return Optional.empty();
    }

    @Override
    public void setColor(Optional<DyeColor> color)
    {

    }

    @Override
    public Level getManaReceiverLevel()
    {
        return level;
    }

    @Override
    public BlockPos getManaReceiverPos()
    {
        return getBlockPos();
    }

    @Override
    public int getCurrentMana()
    {
        if(refreshHandler())
            return handler.getCurrentMana();
        else
            return 0;
    }

    @Override
    public boolean isFull()
    {
        if(refreshHandler())
            return handler.isFull();
        else
            return true; // 如果handler不存在，返回true，防止插入
    }

    @Override
    public void receiveMana(int mana)
    {
        if(refreshHandler())
            handler.receiveMana(mana);
    }

    @Override
    public boolean canReceiveManaFromBursts()
    {
        return true;
    }


    @Override
    public boolean canAttachSpark(ItemStack stack)
    {
        if(refreshHandler())
            return handler.canAttachSpark(stack);
        return false;
    }

    @Override
    public int getAvailableSpaceForMana()
    {
        if(refreshHandler())
            return handler.getAvailableSpaceForMana();
        return 0;
    }

    @Override
    public ManaSpark getAttachedSpark()
    {
        List<Entity> sparks = level.getEntitiesOfClass(Entity.class, new AABB(getBlockPos().above()), Predicates.instanceOf(ManaSpark.class));
        if (sparks.size() == 1) {
            Entity e = sparks.get(0);
            return (ManaSpark) e;
        }

        return null;
    }

    @Override
    public boolean areIncomingTranfersDone()
    {
        if(refreshHandler())
            return handler.areIncomingTranfersDone();
        return false;
    }

    @Override
    public boolean onUsedByWand(@Nullable Player player, ItemStack stack, Direction side)
    {
        if (player == null || player.isShiftKeyDown() && !level.isClientSide()) {
            isOutPutting = !isOutPutting;
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        }
        return true;
    }

    public static class WandHud implements WandHUD
    {
        private final ManaPoolPathwayBlockEntity pool;

        public WandHud(ManaPoolPathwayBlockEntity pool) {
            this.pool = pool;
        }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void renderHUD(net.minecraft.client.gui.GuiGraphics gui, Minecraft mc) {
            ItemStack poolStack = new ItemStack(pool.getBlockState().getBlock());
            String name = poolStack.getHoverName().getString();

            int centerX = mc.getWindow().getGuiScaledWidth() / 2;
            int centerY = mc.getWindow().getGuiScaledHeight() / 2;

            int width = Math.max(102, mc.font.width(name)) + 4;

            RenderHelper.renderHUDBox(gui, centerX - width / 2, centerY + 8, centerX + width / 2, centerY + 48);

            // 这种动态数据不适合每tick同步到客户端，因此默认为满魔力渲染即可
            BotaniaAPIClient.instance().drawSimpleManaHUD(gui, 0x0095FF, 1, 1, name);

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            int arrowU = pool.isOutPutting ? 22 : 0;
            int arrowV = 38;
            RenderHelper.drawTexturedModalRect(gui, HUDHandler.manaBar, centerX - 11, centerY + 30, arrowU, arrowV, 22, 15);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            ItemStack tablet = new ItemStack(BotaniaItems.manaTablet);
            ManaTabletItem.setStackCreative(tablet);

            gui.renderItem(tablet, centerX - 31, centerY + 30);
            gui.renderItem(poolStack, centerX + 15, centerY + 30);

            RenderSystem.disableBlend();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putBoolean("out_putting", isOutPutting);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        isOutPutting = tag.getBoolean("out_putting");
    }
}
