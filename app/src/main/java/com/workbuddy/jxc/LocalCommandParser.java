package com.workbuddy.jxc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalCommandParser {
    private static final String NUMBER = "([0-9]+(?:\\.[0-9]{1,2})?|[零〇一二两三四五六七八九十百千万]+)";
    private static final Pattern PURCHASE = Pattern.compile(
            "(?:进货价|进价|成本价|采购价)\\s*(?:是|为|[:：])?\\s*" + NUMBER + "\\s*(?:元|块)?");
    private static final Pattern SALE = Pattern.compile(
            "(?:销售价|售价|卖价|批发价)\\s*(?:是|为|[:：])?\\s*" + NUMBER + "\\s*(?:元|块)?");
    private static final Pattern STOCK = Pattern.compile(
            "(?:库存|数量|存货)\\s*(?:是|为|[:：])?\\s*" + NUMBER + "\\s*(?:件|条|个|套|双)?");

    private LocalCommandParser() {
    }

    public static ProductDraft parse(String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("请先说出或输入商品信息");
        }
        String text = normalize(command);
        String purchase = findValue(PURCHASE, text, "进货价");
        String sale = findValue(SALE, text, "销售价");
        String stock = findValue(STOCK, text, "库存");

        String name = text.replaceAll("^(?:请|麻烦)?(?:帮我|给我)?(?:添加|新增|录入)(?:一个|一件|一款)?商品", "");
        name = name.replaceAll("货号(?:自增长|自动增长|自动递增|递增|自动生成)", "");
        name = PURCHASE.matcher(name).replaceAll("");
        name = SALE.matcher(name).replaceAll("");
        name = STOCK.matcher(name).replaceAll("");
        name = name.replaceAll("^[\\s，,。.!！；;：:]+|[\\s，,。.!！；;：:]+$", "");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("没有识别到商品名称，请补充后重试");
        }

        long purchaseCents = parseMoney(purchase);
        long saleCents = parseMoney(sale);
        long stockValue = parseChineseNumber(stock).longValueExact();
        if (stockValue > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("库存数量过大");
        }
        return new ProductDraft(name, purchaseCents, saleCents, (int) stockValue);
    }

    private static String findValue(Pattern pattern, String text, String label) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("没有识别到" + label + "，请补充后重试");
        }
        return matcher.group(1);
    }

    private static long parseMoney(String value) {
        try {
            return parseChineseNumber(value).movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("价格最多保留两位小数");
        }
    }

    private static BigDecimal parseChineseNumber(String value) {
        if (value.matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            return new BigDecimal(value);
        }
        Map<Character, Integer> digits = new HashMap<>();
        String chars = "零〇一二两三四五六七八九";
        int[] values = {0, 0, 1, 2, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = 0; i < chars.length(); i++) {
            digits.put(chars.charAt(i), values[i]);
        }
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

    private static String normalize(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            result.append(c >= '０' && c <= '９' ? (char) (c - '０' + '0') : c);
        }
        return result.toString().trim();
    }
}
