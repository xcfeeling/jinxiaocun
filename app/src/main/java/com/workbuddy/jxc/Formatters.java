package com.workbuddy.jxc;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Formatters {
    private Formatters() {
    }

    public static String money(long cents) {
        return "¥" + BigDecimal.valueOf(cents, 2).toPlainString();
    }

    public static String editableMoney(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    public static long parseMoney(String value) {
        try {
            return new BigDecimal(value.trim()).movePointRight(2).longValueExact();
        } catch (Exception e) {
            throw new IllegalArgumentException("价格格式不正确，最多保留两位小数");
        }
    }

    public static String dateTime(long timestamp) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(timestamp));
    }
}
