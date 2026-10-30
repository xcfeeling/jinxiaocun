package com.workbuddy.jxc;

public class InventorySummary {
    public final int productCount;
    public final long totalStock;
    public final long totalCostCents;
    public final long totalRetailCents;
    public final int lowStockCount;
    public final long stagnantStock;
    public final long stagnantCostCents;
    public final int stagnantProductCount;

    public InventorySummary(int productCount, long totalStock, long totalCostCents,
                            long totalRetailCents, int lowStockCount, long stagnantStock,
                            long stagnantCostCents, int stagnantProductCount) {
        this.productCount = productCount;
        this.totalStock = totalStock;
        this.totalCostCents = totalCostCents;
        this.totalRetailCents = totalRetailCents;
        this.lowStockCount = lowStockCount;
        this.stagnantStock = stagnantStock;
        this.stagnantCostCents = stagnantCostCents;
        this.stagnantProductCount = stagnantProductCount;
    }
}
