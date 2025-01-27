package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;

public record SearchAndButtonGuiPacket(String searchText, HashMap<ButtonName, ButtonState> buttonStateMap) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<SearchAndButtonGuiPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "search_and_button_gui_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    public static final StreamCodec<ByteBuf,ButtonName> BUTTONNAME_BUF = buttonNameMap_buf(32767);
    public static final StreamCodec<ByteBuf, ButtonState> BUTTONSTATE_BUF = buttonStateMap_buf(32767);

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<ByteBuf, SearchAndButtonGuiPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    SearchAndButtonGuiPacket::searchText,
                    ByteBufCodecs.map(
                            HashMap::new,
                            BUTTONNAME_BUF,
                            BUTTONSTATE_BUF
                    ),
                    SearchAndButtonGuiPacket::buttonStateMap,
                    SearchAndButtonGuiPacket::new
            );

    static StreamCodec<ByteBuf, ButtonName> buttonNameMap_buf(final int maxLength)
    {
        return new StreamCodec<ByteBuf, ButtonName>() {
            public ButtonName decode(ByteBuf buf) {
                // 从 ByteBuf 中读取 UTF-8 编码的字符串
                String name = Utf8String.read(buf, maxLength);
                // 将字符串转换为 ButtonName 枚举
                return ButtonName.valueOf(name);
            }

            public void encode(ByteBuf buf, ButtonName buttonName) {
                // 将 ButtonName 枚举的 name() 转换为字符串并写入 ByteBuf
                Utf8String.write(buf, buttonName.name(), maxLength);
            }
        };
    }


    static StreamCodec<ByteBuf, ButtonState> buttonStateMap_buf(final int maxLength)
    {
        return new StreamCodec<ByteBuf, ButtonState>() {
            public ButtonState decode(ByteBuf buf) {
                // 从 ByteBuf 中读取 UTF-8 编码的字符串
                String name = Utf8String.read(buf, maxLength);
                // 将字符串转换为 ButtonState 枚举
                return ButtonState.valueOf(name);
            }

            public void encode(ByteBuf buf, ButtonState buttonState) {
                // 将 ButtonState 枚举的 name() 转换为字符串并写入 ByteBuf
                Utf8String.write(buf, buttonState.name(), maxLength);
            }
        };
    }

    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
