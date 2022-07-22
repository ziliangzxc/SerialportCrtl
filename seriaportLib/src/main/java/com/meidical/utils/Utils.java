package com.meidical.utils;

import android.text.TextUtils;

/**
 * @author DasonYu
 * @date 2020/5/18  16:04
 * @descprition
 */
public class Utils {
    public static String toHexString(byte[] data) {
        StringBuilder s = new StringBuilder("{");
        for (int i = 0; i < data.length; ++i) {
            s.append("0x").append(String.format("%02X", data[i])).append(",");
        }
        return s.deleteCharAt(s.length() - 1).append("}").toString();
    }

    public static String toByteHexString(byte[] data) {
        String s = "";
        for (int i = 0; i < data.length; ++i) {
            s += ("(byte) 0x" + String.format("%02X", data[i]) + ",");
        }
        return s;
    }

    /**
     * 字符串   转字节数组
     *
     * @param str
     * @return
     */
    public static byte[] getByteFromString(String str) {
        if (str != null && !str.trim().equals("")) {
            byte[] bytes = new byte[str.length() / 2];

            for (int i = 0; i < str.length() / 2; ++i) {
                bytes[i] = (byte) Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16);
            }

            return bytes;
        } else {
            return new byte[0];
        }
    }

    /**
     * 字节数组 转 字符串 输出
     *
     * @param bytes
     * @return
     */
    public static String getStringFromByte(byte[] bytes) {
        StringBuilder str = new StringBuilder();

        for (byte aByte : bytes) {
            String temp;
            if ((temp = Integer.toHexString(aByte & 255)).length() == 1) {
                temp = '0' + temp;
            }
            str.append(temp);
        }

        return str.toString();
    }

    /**
     * @return
     */
    public static boolean currentDayIsExist(int year, int month, int day) {
        int maxDay = 0;
        int minDay = 1;
        if (month == 2) {
            //判断年是不是闰年
            boolean b1 = year % 4 == 0 && year % 100 != 0;
            boolean b2 = year % 400 == 0;
            if (b1 || b2) {
                maxDay = 29;
            } else {
                maxDay = 28;
            }
        } else if (month == 4 || month == 6 || month == 9 || month == 11) {
            maxDay = 30;
        } else {
            maxDay = 31;
        }
        return day <= maxDay && day >= minDay;
    }

    /**
     * 判断输入的序列号是否合法
     *
     * @param s
     * @return
     */
    public static boolean checkSerialNumIllegal(String s) {
        if (TextUtils.isEmpty(s)) {
            return false;
        }
        String regex = "^[A-Fa-f0-9]{8}$";
        return s.matches(regex);
    }
}

