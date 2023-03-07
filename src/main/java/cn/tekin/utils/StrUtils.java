package cn.tekin.utils;

/**
 * @author tekintian@gmail.com
 * @version v0.0.1
 * @since v0.0.1 2023-03-07 14:59
 */
public class StrUtils {

    /**
     * 首字母大小写转换
     * 通过 ASCII 码判断字母大小写，ASCII在 65-90 之间是大写，97-122 是小写
     * @param str
     * @return
     */
    public static String lowerFirst(String str) {
        char[] chars=str.toCharArray();

        // 如果第一个char为大写则 通过加 32转换为小写
        if (chars[0]>=65 && chars[0]<= 90) {
            chars[0]+=32;
        }

        return String.valueOf(chars);
    }
}
