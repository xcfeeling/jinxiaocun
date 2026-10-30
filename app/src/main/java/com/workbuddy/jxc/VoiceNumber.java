package com.workbuddy.jxc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public final class VoiceNumber {
    public static final String PATTERN = "([0-9]+(?:\\.[0-9]{1,2})?|[零〇一二两三四五六七八九十百千万]+)";

    private VoiceNumber() {
    }

    public static BigDecimal parse(String value) {
        if (value.matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            return new BigDecimal(value);
        }
        Map<Character, Integer> digits = new HashMap<>();
        String chars = "零〇一二两三四五六七八九";
        int[] values = {0, 0, 1, 2, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = 0; i < chars.length(); i++) digits.put(chars.charAt(i), values[i]);
        long total = 0;
        long section = 0;
        int number = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (digits.containsKey(current)) {
                number = digits.get(current);
            } else if (current == '十' || current == '百' || current == '千') {
                int unit = current == '十' ? 10 : current == '百' ? 100 : 1000;
                section += (number == 0 ? 1 : number) * unit;
                number = 0;
            } else if (current == '万') {
                section += number;
                total += (section == 0 ? 1 : section) * 10000L;
                section = 0;
                number = 0;
            } else {
                throw new IllegalArgumentException("无法识别数字：" + value);
            }
        }
        return BigDecimal.valueOf(total + section + number);
    }

    public static long money(String value) {
        try {
            return parse(value).movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("价格最多保留两位小数");
        }
    }

    public static int integer(String value) {
        try {
            return parse(value).intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("数量必须是整数");
        }
    }
}
