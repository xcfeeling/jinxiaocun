package com.workbuddy.jxc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AnalysisReportGenerator {
    private AnalysisReportGenerator() {
    }

    public static AnalysisSnapshot generate(InventoryDatabase database, int windowDays) {
        long now = System.currentTimeMillis();
        long start = now - windowDays * 86400000L;
        List<Product> products = database.searchProducts("");
        List<BusinessRecord> sales = database.getRecords(false, 1000000);
        List<BusinessRecord> purchases = database.getRecords(true, 1000000);
        Map<Long, long[]> salesByProduct = new HashMap<>();
        Map<Long, Integer> purchasesByProduct = new HashMap<>();
        Map<Long, Long> lastSales = new HashMap<>();
        Map<String, long[]> priceBands = new LinkedHashMap<>();
        for (BusinessRecord sale : sales) {
            if (sale.businessDate >= start && sale.businessDate <= now) {
                long[] values = salesByProduct.containsKey(sale.productId) ? salesByProduct.get(sale.productId) : new long[2];
                values[0] += sale.quantity;
                values[1] += sale.totalCents();
                salesByProduct.put(sale.productId, values);
                Product product = findProduct(products, sale.productId);
                if (product != null) {
                    String band = priceBand(product.salePriceCents);
                    long[] bandValues = priceBands.containsKey(band) ? priceBands.get(band) : new long[2];
                    bandValues[0] += sale.quantity;
                    bandValues[1] += sale.totalCents();
                    priceBands.put(band, bandValues);
                }
            }
            lastSales.merge(sale.productId, sale.businessDate, Math::max);
        }
        for (BusinessRecord purchase : purchases) {
            if (purchase.businessDate >= start && purchase.businessDate <= now) {
                purchasesByProduct.merge(purchase.productId, purchase.quantity, Integer::sum);
            }
        }
        List<ProductPerformance> performance = new ArrayList<>();
        for (Product product : products) {
            long[] sold = salesByProduct.containsKey(product.id) ? salesByProduct.get(product.id) : new long[2];
            int quantity = (int) sold[0];
            long amount = sold[1];
            int purchased = purchasesByProduct.containsKey(product.id) ? purchasesByProduct.get(product.id) : 0;
            long lastSold = lastSales.containsKey(product.id) ? lastSales.get(product.id) : 0;
            long since = lastSold > 0 ? lastSold : product.createdAt;
            int daysWithoutSales = (int) Math.max(0, (now - since) / 86400000L);
            double daily = quantity / (double) windowDays;
            double coverage = daily > 0 ? product.stock / daily : (product.stock > 0 ? 9999 : 0);
            int fastThreshold = Math.max(5, windowDays / 15);
            boolean fast = quantity >= fastThreshold;
            boolean slow = product.stock > 0 && daysWithoutSales >= 30;
            int suggested = suggestedQuantity(product, quantity, windowDays, coverage, slow, now);
            performance.add(new ProductPerformance(product, quantity, amount, purchased, lastSold,
                    daysWithoutSales, coverage, suggested, fast, slow));
        }
        List<CategoryStats> bands = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : priceBands.entrySet()) {
            long[] value = entry.getValue();
            bands.add(new CategoryStats(entry.getKey(), (int) value[0], value[1], 0, 0));
        }
        return new AnalysisSnapshot(windowDays, now, database.getPeriodStats(start, now + 1),
                database.getSummary(), performance,
                database.getCategoryStats(true, start, now + 1),
                database.getCategoryStats(false, start, now + 1), bands);
    }

    private static int suggestedQuantity(Product product, int quantity, int windowDays,
                                         double coverage, boolean slow, long now) {
        if (quantity <= 0 || slow) return 0;
        double daily = quantity / (double) windowDays;
        int targetDays = quantity >= Math.max(5, windowDays / 15) ? 30 : 45;
        double factor = seasonFactor(product.season, now);
        double needed = daily * targetDays * factor - product.stock;
        return (int) Math.min(9999, Math.max(0, Math.ceil(needed)));
    }

    private static double seasonFactor(String season, long now) {
        if ("ALL".equals(season)) return 1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        int month = calendar.get(Calendar.MONTH) + 1;
        String current;
        if (month >= 3 && month <= 5) current = "SPRING";
        else if (month >= 6 && month <= 8) current = "SUMMER";
        else if (month >= 9 && month <= 11) current = "AUTUMN";
        else current = "WINTER";
        return season.equals(current) ? 1.15 : 0.85;
    }

    private static Product findProduct(List<Product> products, long id) {
        for (Product product : products) if (product.id == id) return product;
        return null;
    }

    private static String priceBand(long price) {
        if (price < 10000) return "100元以下";
        if (price < 20000) return "100-199元";
        if (price < 30000) return "200-299元";
        return "300元及以上";
    }
}
