package com.workbuddy.jxc;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalBusinessParser {
    private LocalBusinessParser() {
    }

    public static BusinessDraft parse(String command, String preferredIntent) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("请先说出或输入业务内容");
        }
        String text = normalize(command);
        String intent = detectIntent(text, preferredIntent);
        if (BusinessDraft.ADD_PRODUCT.equals(intent)) {
            return new BusinessDraft(LocalCommandParser.parse(text));
        }
        boolean purchase = BusinessDraft.PURCHASE.equals(intent);
        Pattern quantityPattern = Pattern.compile("(?:数量|进货|采购|补货|入库|销售|卖出|卖了|出库|进了|到货)\\s*" +
                "(?:是|为|了|[:：])?\\s*" + VoiceNumber.PATTERN + "\\s*(?:件|条|个|套|双)?");
        String quantityText = value(quantityPattern, text, false);
        if (quantityText == null) {
            quantityText = value(Pattern.compile(VoiceNumber.PATTERN + "\\s*(?:件|条|个|套|双)"), text, false);
        }
        if (quantityText == null) {
            throw new IllegalArgumentException("没有识别到数量，请说例如“裙子销售3件”");
        }
        Pattern pricePattern = Pattern.compile(purchase
                ? "(?:进货价|进价|采购价|单价)\\s*(?:是|为|[:：])?\\s*" + VoiceNumber.PATTERN + "\\s*(?:元|块)?"
                : "(?:销售价|售价|卖价|单价)\\s*(?:是|为|[:：])?\\s*" + VoiceNumber.PATTERN + "\\s*(?:元|块)?");
        String priceText = value(pricePattern, text, false);
        if (priceText == null) {
            throw new IllegalArgumentException("没有识别到单价，请补充" + (purchase ? "进货价" : "销售价"));
        }
        String partner = partner(text, purchase);
        String name = productName(text, quantityPattern, pricePattern);
        return new BusinessDraft(intent, name, VoiceNumber.integer(quantityText),
                VoiceNumber.money(priceText), partner, businessDate(text));
    }

    private static String detectIntent(String text, String preferred) {
        if (text.matches(".*(新增|添加|货号自增长).*")) return BusinessDraft.ADD_PRODUCT;
        if (text.matches(".*(销售|卖出|卖了|售给|开单|出库).*")) return BusinessDraft.SALE;
        if (text.matches(".*(进货|采购|补货|入库|进了|到货).*")) return BusinessDraft.PURCHASE;
        if (text.contains("商品")) return BusinessDraft.ADD_PRODUCT;
        if (BusinessDraft.PURCHASE.equals(preferred) || BusinessDraft.SALE.equals(preferred)) return preferred;
        return BusinessDraft.ADD_PRODUCT;
    }

    private static String productName(String text, Pattern quantity, Pattern price) {
        String name = text;
        name = name.replaceAll("^(?:请|麻烦)?(?:帮我|给我)?(?:记一笔|做一笔|添加|新增|录入)?", "");
        name = name.replaceAll("^(?:进货|采购|补货|入库|销售|卖出|卖了|出库|开单)", "");
        name = name.replaceAll("^(?:给|为)", "");
        name = name.replaceAll("(?:供应商|供货商|客户|买家).*$", "");
        name = name.replaceAll("(?:进货|采购|补货|入库|销售|卖出|卖了|出库|进了|到货).*$", "");
        name = quantity.matcher(name).replaceAll("");
        name = price.matcher(name).replaceAll("");
        name = name.replaceAll(VoiceNumber.PATTERN + "\\s*(?:件|条|个|套|双)", "");
        name = name.replaceAll("(?:今天|昨天|前天|\\d{4}-\\d{1,2}-\\d{1,2})", "");
        name = name.replaceAll("^[\\s，,。.!！；;：:]+|[\\s，,。.!！；;：:]+$", "");
        if (name.isEmpty()) throw new IllegalArgumentException("没有识别到商品名称");
        return name;
    }

    private static String partner(String text, boolean purchase) {
        Pattern pattern = Pattern.compile(purchase
                ? "(?:供应商|供货商)\\s*(?:是|为|[:：])?\\s*([^，,。.!！]+)"
                : "(?:客户|买家|卖给|售给)\\s*(?:是|为|[:：])?\\s*([^，,。.!！]+)");
        String value = value(pattern, text, false);
        return value == null ? "" : value.trim();
    }

    private static long businessDate(String text) {
        Calendar calendar = Calendar.getInstance();
        if (text.contains("前天")) calendar.add(Calendar.DAY_OF_MONTH, -2);
        else if (text.contains("昨天")) calendar.add(Calendar.DAY_OF_MONTH, -1);
        Matcher matcher = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})").matcher(text);
        if (matcher.find()) {
            calendar.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)) - 1,
                    Integer.parseInt(matcher.group(3)));
        }
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static String value(Pattern pattern, String text, boolean required) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            if (required) throw new IllegalArgumentException("缺少必要信息");
            return null;
        }
        return matcher.group(1);
    }

    private static String normalize(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            result.append(c >= '０' && c <= '９' ? (char) (c - '０' + '0') : c);
        }
        return result.toString().trim();
    }
}
