package com.workbuddy.jxc;

public class CategoryStats {
    public final String name;
    public final int salesQuantity;
    public final long salesCents;
    public final long stockQuantity;
    public final long stockCostCents;

    public CategoryStats(String name, int salesQuantity, long salesCents,
                         long stockQuantity, long stockCostCents) {
        this.name = name;
        this.salesQuantity = salesQuantity;
        this.salesCents = salesCents;
        this.stockQuantity = stockQuantity;
        this.stockCostCents = stockCostCents;
    }
}
