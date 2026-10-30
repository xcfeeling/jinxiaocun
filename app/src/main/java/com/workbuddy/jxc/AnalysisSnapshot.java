package com.workbuddy.jxc;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnalysisSnapshot {
    public final int windowDays;
    public final long generatedAt;
    public final PeriodStats periodStats;
    public final InventorySummary inventorySummary;
    public final List<ProductPerformance> products;
    public final List<CategoryStats> seasons;
    public final List<CategoryStats> styles;
    public final List<CategoryStats> priceBands;

    public AnalysisSnapshot(int windowDays, long generatedAt, PeriodStats periodStats,
                            InventorySummary inventorySummary, List<ProductPerformance> products,
                            List<CategoryStats> seasons, List<CategoryStats> styles,
                            List<CategoryStats> priceBands) {
        this.windowDays = windowDays;
        this.generatedAt = generatedAt;
        this.periodStats = periodStats;
        this.inventorySummary = inventorySummary;
        this.products = products;
        this.seasons = seasons;
        this.styles = styles;
        this.priceBands = priceBands;
    }

    public JSONObject toJson() throws Exception {
        JSONObject root = new JSONObject();
        root.put("windowDays", windowDays);
        root.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                .format(new Date(generatedAt)));
        JSONObject totals = new JSONObject();
        totals.put("salesAmount", money(periodStats.salesCents));
        totals.put("grossProfit", money(periodStats.grossProfitCents));
        totals.put("salesQuantity", periodStats.salesQuantity);
        totals.put("salesCount", periodStats.salesCount);
        totals.put("purchaseAmount", money(periodStats.purchaseCents));
        totals.put("purchaseQuantity", periodStats.purchaseQuantity);
        totals.put("currentStock", inventorySummary.totalStock);
        totals.put("stockCost", money(inventorySummary.totalCostCents));
        totals.put("lowStockProducts", inventorySummary.lowStockCount);
        totals.put("stagnantProducts", inventorySummary.stagnantProductCount);
        root.put("totals", totals);
        JSONArray items = new JSONArray();
        for (ProductPerformance item : products) {
            JSONObject product = new JSONObject();
            product.put("code", item.product.code);
            product.put("name", item.product.name);
            product.put("season", item.product.seasonLabel());
            product.put("style", item.product.style);
            product.put("category", item.product.category);
            product.put("purchasePrice", money(item.product.purchasePriceCents));
            product.put("salePrice", money(item.product.salePriceCents));
            product.put("stock", item.product.stock);
            product.put("salesQuantity", item.salesQuantity);
            product.put("salesAmount", money(item.salesCents));
            product.put("purchaseQuantity", item.purchaseQuantity);
            product.put("daysWithoutSales", item.daysWithoutSales);
            product.put("coverageDays", item.coverageDays >= 999 ? 999 : item.coverageDays);
            product.put("fastMoving", item.fastMoving);
            product.put("slowMoving", item.slowMoving);
            product.put("localSuggestedQuantity", item.suggestedQuantity);
            items.put(product);
        }
        root.put("products", items);
        root.put("seasonAnalysis", categoriesJson(seasons));
        root.put("styleAnalysis", categoriesJson(styles));
        root.put("priceBandAnalysis", categoriesJson(priceBands));
        return root;
    }

    private JSONArray categoriesJson(List<CategoryStats> categories) throws Exception {
        JSONArray array = new JSONArray();
        for (CategoryStats category : categories) {
            JSONObject item = new JSONObject();
            item.put("name", category.name);
            item.put("salesQuantity", category.salesQuantity);
            item.put("salesAmount", money(category.salesCents));
            item.put("stockQuantity", category.stockQuantity);
            item.put("stockCost", money(category.stockCostCents));
            array.put(item);
        }
        return array;
    }

    private String money(long cents) {
        return Formatters.editableMoney(cents);
    }
}
