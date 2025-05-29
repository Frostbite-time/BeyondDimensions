package com.wintercogs.beyonddimensions.GUI.SharedWidget;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class StatusButton extends IconButton
{
    protected ArrayList<ButtonState> states = new ArrayList<>();;
    protected Map<ButtonState,ResourceLocation> iconMap = new HashMap<>();
    protected Map<ButtonState, Tooltip> tooltipMap = new HashMap<>(); // 需要添加可变工具提示则添加 需要固定工具提示则直接setTooltip，此处留空
    public ButtonState currentState;


    protected StatusButton(int x, int y, int width, int height, ButtonName name, OnPress onPress)
    {
        // 给予一个默认图片用于构造父类
        super(x, y, width, height, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/unkonw_thing.png"), name, onPress);
        initButton();
        setIcon(iconMap.get(currentState));
    }

    // 用于子类初始化状态、状态图片映射表、当前状态
    protected abstract void initButton();

    // 用于快速切换状态的便捷方法
    public void toggleState()
    {
        // 确保状态列表不为空
        if (states == null || states.isEmpty()) {
            throw new IllegalStateException("Button states are not initialized.");
        }

        // 找到当前状态的索引
        int currentIndex = states.indexOf(currentState);

        // 如果当前状态不在列表中，抛出异常
        if (currentIndex == -1) {
            throw new IllegalStateException("Current state is not in the states list.");
        }

        // 计算下一个状态的索引（循环切换）
        int nextIndex = (currentIndex + 1) % states.size();

        // 切换到下一个状态
        currentState = states.get(nextIndex);

        // 更新按钮图标为当前状态对应的图标
        setIcon(iconMap.get(currentState));

        // 更新工具提示
        if(!(tooltipMap == null))
        {
            if(tooltipMap.containsKey(currentState) && tooltipMap.get(currentState)!=null)
            {
                setTooltip(tooltipMap.get(currentState));
            }
        }
    }

    // 用于手动设置当前状态
    public void setState(ButtonState state)
    {
        currentState = state;
        setIcon(iconMap.get(currentState));

        // 更新工具提示
        if(!(tooltipMap == null))
        {
            if(tooltipMap.containsKey(currentState) && tooltipMap.get(currentState)!=null)
            {
                setTooltip(tooltipMap.get(currentState));
            }
        }
    }
}
