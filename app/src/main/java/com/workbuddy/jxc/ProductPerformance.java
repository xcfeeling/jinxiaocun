package com.workbuddy.jxc;

public class ProductPerformance {
    public final Product product;
    public final int salesQuantity;
    public final long salesCents;
    public final int purchaseQuantity;
    public final long lastSoldAt;
    public final int daysWithoutSales;
    public final double coverageDays;
    public final int suggestedQuantity;
    public final boolean fastMoving;
    public final boolean slowMoving;

    public ProductPerformance(Product product, int salesQuantity, long salesCents, int purchaseQuantity,
                              long lastSoldAt, int daysWithoutSales, double coverageDays,
                              int suggestedQuantity, boolean fastMoving, boolean slowMoving) {
        this.product = product;
        this.salesQuantity = salesQuantity;
        this.salesCents = salesCents;
        this.purchaseQuantity = purchaseQuantity;
        this.lastSoldAt = lastSoldAt;
        this.daysWithoutSales = daysWithoutSales;
        this.coverageDays = coverageDays;
        this.suggestedQuantity = suggestedQuantity;
        this.fastMoving = fastMoving;
        this.slowMoving = slowMoving;
    }

    public String coverageLabel() {
        if (salesQuantity == 0) return "近期无销量";
        if (coverageDays >= 999) return "超过999天";
        return String.format(java.util.Locale.US, "%.0f天", coverageDays);
    }
}
