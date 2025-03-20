package com.wintercogs.beyonddimensions.Unit;

import com.github.promeg.pinyinhelper.Pinyin;
import com.github.promeg.tinypinyin.lexicons.java.cncity.CnCityDict;
import com.wintercogs.beyonddimensions.BeyondDimensions;

import static com.github.promeg.pinyinhelper.Pinyin.isChinese;

public class TinyPinyinUtils
{

    static {
        // 初始化TinyPinyin配置，应用中国地区词典并设置拼音为小写
        Pinyin.init(Pinyin.newConfig().with(CnCityDict.getInstance()));
        BeyondDimensions.LOGGER.info(getFirstPinYin("西瓜"));
        BeyondDimensions.LOGGER.info(getAllPinyin("西瓜",false));

    }

    /**
     * 获取中文汉字的拼音首字母
     * @param hanyu 中文字符串
     * @return 拼音首字母字符串
     */
    public static String getFirstPinYin(String hanyu) {
        StringBuilder firstPinyin = new StringBuilder();
        char[] chars = hanyu.trim().toCharArray();
        for (char c : chars) {
            if (isChinese(c)) {
                String pinyin = Pinyin.toPinyin(c);
                if (!pinyin.isEmpty()) {
                    firstPinyin.append(pinyin.charAt(0));
                }
            } else {
                firstPinyin.append(c);
            }
        }
        return firstPinyin.toString();
    }

    /**
     * 将中文汉字转为全拼音
     * @param hanzi 中文字符串
     * @param addSpace 是否用空格分隔拼音
     * @return 全拼字符串
     */
    public static String getAllPinyin(String hanzi, boolean addSpace) {
        StringBuilder result = new StringBuilder();
        char[] chars = hanzi.trim().toCharArray();
        for (char c : chars) {
            if (isChinese(c)) {
                result.append(Pinyin.toPinyin(c));
            } else {
                result.append(c);
            }
            if (addSpace) {
                result.append(' ');
            }
        }
        if (addSpace && result.length() > 0) {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }
}
