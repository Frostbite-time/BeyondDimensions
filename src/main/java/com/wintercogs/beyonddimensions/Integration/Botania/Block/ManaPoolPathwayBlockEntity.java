package com.wintercogs.beyonddimensions.Integration.Botania.Block;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.ManaUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetedBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.lwjgl.opengl.GL11;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.BotaniaAPIClient;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.BotaniaForgeClientCapabilities;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.block.Wandable;
import vazkii.botania.api.internal.ManaBurst;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.item.ManaDissolvable;
import vazkii.botania.api.mana.*;
import vazkii.botania.api.mana.spark.SparkAttachable;
import vazkii.botania.api.recipe.ManaInfusionRecipe;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.client.fx.WispParticleData;
import vazkii.botania.client.gui.HUDHandler;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.block.block_entity.mana.BellowsBlockEntity;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import vazkii.botania.common.crafting.StateIngredients;
import vazkii.botania.common.handler.BotaniaSounds;
import vazkii.botania.common.handler.ManaNetworkHandler;
import vazkii.botania.common.helper.EntityHelper;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.common.item.ManaTabletItem;
import vazkii.botania.xplat.BotaniaConfig;
import vazkii.botania.xplat.XplatAbstractions;

import java.util.List;
import java.util.Optional;

// 既可以为功能花提供魔力，也可以从产能花以及魔力发射器接收魔力，并能并入火花网络的魔力池
public class ManaPoolPathwayBlockEntity extends NetedBlockEntity implements ManaCollector, ManaPool, SparkAttachable, Wandable
{
    private ManaUnifiedStorageHandler handler = null;
    private boolean isOutPutting = true;

    private final Int2ObjectMap<MutableInt> chargingParticles = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<MutableInt> drainingParticles = new Int2ObjectOpenHashMap<>();
    private static final float CHARGING_GRAVITY = 0.003f;
    // 植物魔法的三个事件id
    private static final int CRAFT_EFFECT_EVENT = 0;
    private static final int CHARGE_EFFECT_EVENT = 1;
    private static final int DRAIN_EFFECT_EVENT = 2;
    // soundTicks用于合成配方的粒子特效和声音
    private int soundTicks = 0;
    private int ticks = 0; // 用于触发充能和放能的动画，虽然速率无限，基本不可能看到动画。。

    // 成员：放在你的 BE 里
    private final Long2ObjectOpenHashMap<RecipeHolder<ManaInfusionRecipe>> recipeCache = new Long2ObjectOpenHashMap<>();
    private int recipeCacheVersionSeen = -1;
    private static volatile int GLOBAL_RECIPE_CACHE_VERSION = 0;

    public ManaPoolPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.MANA_POOL_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
        refreshHandler();
    }

    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                BotaniaForgeCapabilities.MANA_RECEIVER,
                ModBlockEntities.MANA_POOL_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if(be instanceof ManaPoolPathwayBlockEntity pool)
                        return pool;
                    else
                        return null;
                }
        );

        event.registerBlockEntity(
                BotaniaForgeCapabilities.SPARK_ATTACHABLE,
                ModBlockEntities.MANA_POOL_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if(be instanceof ManaPoolPathwayBlockEntity pool)
                        return pool;
                    else
                        return null;
                }
        );

        event.registerBlockEntity(
                BotaniaForgeClientCapabilities.BLOCK_WAND_HUD,
                ModBlockEntities.MANA_POOL_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if(be instanceof ManaPoolPathwayBlockEntity pool)
                        return new WandHud(pool);
                    return null;
                }
        );

        event.registerBlockEntity(
                BotaniaForgeCapabilities.WANDABLE,
                ModBlockEntities.MANA_POOL_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if(be instanceof ManaPoolPathwayBlockEntity pool)
                        return pool;
                    else
                        return null;
                }
        );

    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ManaPoolPathwayBlockEntity be)
    {
        double particleChance = 1F - 0.1;
        if (Math.random() > particleChance) {
            WispParticleData data = WispParticleData.wisp((float) Math.random() / 3F,
                    ManaPoolBlockEntity.PARTICLE_COLOR_RED, ManaPoolBlockEntity.PARTICLE_COLOR_GREEN, ManaPoolBlockEntity.PARTICLE_COLOR_BLUE, 2F);
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

        if (be.soundTicks > 0) {
            be.soundTicks--;
        }

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
        for (ItemEntity item : items) {
            if (!item.isAlive()) {
                continue;
            }

            ItemStack stack = item.getItem();
            ManaItem mana = XplatAbstractions.INSTANCE.findManaItem(stack);
            if (!stack.isEmpty() && mana != null) {
                if (be.isOutPutting && mana.canReceiveManaFromPool(be) || !be.isOutPutting && mana.canExportManaToPool(be))
                {
                    boolean didSomething = false;
                    int transfRate = Integer.MAX_VALUE;

                    if (be.isOutPutting) //输出到石板
                    {
                        if (be.getCurrentMana() > 0 && mana.getMana() < mana.getMaxMana())
                        {
                            didSomething = true;
                            int manaVal = Math.min(transfRate, Math.min(be.getCurrentMana(), mana.getMaxMana() - mana.getMana()));
                            mana.addMana(manaVal);
                            be.receiveMana(-manaVal);
                        }
                    }
                    else // 从石板接收
                    {
                        if (mana.getMana() > 0 && !be.isFull()) {
                            didSomething = true;
                            int manaVal = Math.min(transfRate, Math.min(BDMath.clampLongToInt(be.getActualMaxMana() - be.getActualCurrentMana()), mana.getMana()));
                            if (manaVal == 0 && be.level.getBlockState(pos.below()).is(BotaniaBlocks.manaVoid)) {
                                manaVal = Math.min(transfRate, mana.getMana());
                            }
                            mana.addMana(-manaVal);
                            be.receiveMana(manaVal);
                        }
                    }

                    if (didSomething)
                    {
                        if (BotaniaConfig.common().chargingAnimationEnabled() && be.ticks % 10 == 0) {
                            level.blockEvent(pos, state.getBlock(),
                                    be.isOutPutting ? CHARGE_EFFECT_EVENT : DRAIN_EFFECT_EVENT,
                                    encodeRelativeItemPosition(pos, item));
                        }
                        EntityHelper.syncItem(item);
                    }
                }
            }
        }
        be.ticks++;
    }

    private static int encodeRelativeItemPosition(BlockPos worldPosition, ItemEntity item) {
        double relX = Mth.clamp(item.position().x() - worldPosition.getX(), 0, 1);
        double relY = Mth.clamp(0.125 + 0.875 * (item.position().y() - worldPosition.getY()), 0.125, 0.9);
        double relZ = Mth.clamp(item.position().z() - worldPosition.getZ(), 0, 1);

        int compressedX = (int) Math.round(7.0 * relX);
        int compressedY = 4 - Mth.ceillog2(14 - (int) (14.0 * relY));
        int compressedZ = (int) Math.round(7.0 * relZ);

        return compressedX | compressedY << 3 | compressedZ << 5;
    }

    // 供 配方重载 调用
    public static void onRecipesReloaded() {
        GLOBAL_RECIPE_CACHE_VERSION++;
    }

    @Nullable
    public RecipeHolder<ManaInfusionRecipe> getMatchingRecipe(ItemStack stack, BlockState below) {
        if (level == null || stack.isEmpty()) return null;

        // 版本变化 → 清缓存
        if (recipeCacheVersionSeen != GLOBAL_RECIPE_CACHE_VERSION) {
            recipeCache.clear();
            recipeCacheVersionSeen = GLOBAL_RECIPE_CACHE_VERSION;
        }

        final int itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(stack.getItem());
        final int blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(below.getBlock());
        final long key = ((long) itemId << 32) | (blockId & 0xFFFFFFFFL);

        var cached = recipeCache.get(key);
        if (cached != null) {
            var r = cached.value();
            // 二次校验，防止属性型催化剂不匹配
            if (r.matches(stack) && (r.getRecipeCatalyst() == StateIngredients.NONE || r.getRecipeCatalyst().test(below))) {
                return cached;
            } else {
                recipeCache.remove(key); // 失效
            }
        }

        // 没命中 → 走“档 A”逻辑
        RecipeHolder<ManaInfusionRecipe> best = null, firstNonCat = null;
        for (var rh : BotaniaRecipeTypes.getRecipes(level, BotaniaRecipeTypes.MANA_INFUSION_TYPE)) {
            var r = rh.value();
            if (!r.matches(stack)) continue;

            if (r.getRecipeCatalyst() != StateIngredients.NONE) {
                if (r.getRecipeCatalyst().test(below)) {
                    best = rh; // 最高优先级，直接确定
                    break;
                }
            } else if (firstNonCat == null) {
                firstNonCat = rh;
            }
        }
        if (best == null) best = firstNonCat;

        if (best != null) {
            if (recipeCache.size() > 256) recipeCache.clear(); // 简易限流
            recipeCache.put(key, best);
        }
        return best;
    }

    // 用于配方合成
    public boolean collideEntityItem(ItemEntity item) {
        if (level.isClientSide || !item.isAlive() || item.getItem().isEmpty()) {
            return false;
        }

        ItemStack stack = item.getItem();

        if (stack.getItem() instanceof ManaDissolvable dissolvable) {
            dissolvable.onDissolveTick(this, item);
        }

        if (XplatAbstractions.INSTANCE.itemFlagsComponent(item).manaInfusionSpawned) {
            return false;
        }

        RecipeHolder<ManaInfusionRecipe> recipe = getMatchingRecipe(stack, level.getBlockState(worldPosition.below()));

        if (recipe != null) {
            int mana = recipe.value().getManaToConsume();
            if (getCurrentMana() >= mana) {
                receiveMana(-mana);

                ItemStack output = recipe.value().getRecipeOutput(level.registryAccess(), stack);
                EntityHelper.shrinkItem(item);
                item.setOnGround(false); //Force entity collision update to run every tick if crafting is in progress

                ItemEntity outputItem = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, output);
                XplatAbstractions.INSTANCE.itemFlagsComponent(outputItem).manaInfusionSpawned = true;
                if (item.getOwner() instanceof Player player) {
                    player.triggerRecipeCrafted(recipe, List.of(output));
                    output.onCraftedBy(level, player, output.getCount());
                } else {
                    output.onCraftedBySystem(level);
                }
                level.addFreshEntity(outputItem);

                craftingEffect(true);
                return true;
            }
        }

        return false;
    }

    // 播放配方合成后的粒子特效和声音
    public void craftingEffect(boolean playSound) {
        if (playSound && soundTicks == 0) {
            level.playSound(null, worldPosition, BotaniaSounds.manaPoolCraft, SoundSource.BLOCKS, 1F, 1F);
            soundTicks = 6;
        }

        level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, getBlockPos());
        level.blockEvent(getBlockPos(), getBlockState().getBlock(), CRAFT_EFFECT_EVENT, 0);
    }


    @Override
    public boolean triggerEvent(int event, int param) {
        return switch (event)
        {
            case 0 ->
            {/* 表示CRAFT_EFFECT_EVENT */
                if (level.isClientSide)
                {
                    for (int i = 0; i < 25; i++)
                    {
                        float red = (float) Math.random();
                        float green = (float) Math.random();
                        float blue = (float) Math.random();
                        SparkleParticleData data = SparkleParticleData.sparkle((float) Math.random(), red, green, blue, 10);
                        level.addParticle(data, worldPosition.getX() + 0.5 + Math.random() * 0.4 - 0.2, worldPosition.getY() + 0.75, worldPosition.getZ() + 0.5 + Math.random() * 0.4 - 0.2, 0, 0, 0);
                    }
                }

                yield true;
            }
            case CHARGE_EFFECT_EVENT ->
            {
                if (level.isClientSide && BotaniaConfig.common().chargingAnimationEnabled())
                {
                    chargingParticles.computeIfAbsent(param, i -> new MutableInt(15)).setValue(15);
                }
                yield true;
            }
            case DRAIN_EFFECT_EVENT ->
            {
                if (level.isClientSide && BotaniaConfig.common().chargingAnimationEnabled())
                {
                    drainingParticles.computeIfAbsent(param, i -> new MutableInt(15)).setValue(15);
                }
                yield true;
            }
            default -> super.triggerEvent(event, param);
        };
    }


    private static void displayChargingParticles(Level level, BlockPos worldPosition, ManaPoolPathwayBlockEntity be,
                                                 Int2ObjectMap<MutableInt> particles, boolean charging) {
        int bellowCount = charging ? getBellowCount(level, worldPosition, be) : 0;
        int max = be.getMaxMana();
        float relativeMana = (max > 0) ? (float) be.getCurrentMana() / max : 0f;
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
        if (horizontalDistance < 1.0e-6) {
            // 退化处理：轻微随机偏移，避免除零
            horizontalDiff = new Vec3((Math.random() - 0.5) * 0.01, 0, (Math.random() - 0.5) * 0.01);
            horizontalDistance = horizontalDiff.horizontalDistance();
        }
        Vec3 horizontalDir = horizontalDiff.scale(1 / horizontalDistance);
        double startHeight = startPos.y - endPos.y;
        double vY0Squared = 2 * CHARGING_GRAVITY * (maxHeight - startHeight);
        double vY0 = Math.sqrt(vY0Squared);
        double lifetime = (vY0 + Math.sqrt(vY0Squared + 2 * CHARGING_GRAVITY * startHeight)) / CHARGING_GRAVITY;
        double vX0 = horizontalDistance / lifetime;
        Vec3 v0 = horizontalDir.scale(vX0).with(Direction.Axis.Y, vY0);

        WispParticleData data = WispParticleData.wisp(0.1f, ManaPoolBlockEntity.PARTICLE_COLOR_RED, ManaPoolBlockEntity.PARTICLE_COLOR_GREEN,
                ManaPoolBlockEntity.PARTICLE_COLOR_BLUE, (float) (0.025 * lifetime), CHARGING_GRAVITY).withNoClip(true);
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
    public @UnknownNullability Level getManaReceiverLevel()
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
    public boolean areIncomingTransfersDone()
    {
        if(refreshHandler())
            return handler.areIncomingTransfersDone();
        return false;
    }

    @Override
    public boolean onUsedByWand(@Nullable Player player, ItemStack stack, Direction side)
    {
        if ((player == null || player.isShiftKeyDown()) && !level.isClientSide()) {
            isOutPutting = !isOutPutting;
            setChanged();
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putBoolean("out_putting", isOutPutting);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        isOutPutting = tag.getBoolean("out_putting");
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        BotaniaAPI.instance().getManaNetworkInstance().fireManaNetworkEvent(this, ManaBlockType.POOL, ManaNetworkAction.REMOVE);
    }
}
