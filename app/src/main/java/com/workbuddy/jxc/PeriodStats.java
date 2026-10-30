package com.workbuddy.jxc;

public class PeriodStats {
    public final long salesCents;
    public final long purchaseCents;
    public final long grossProfitCents;
    public final int salesQuantity;
    public final int purchaseQuantity;
    public final int salesCount;
    public final int purchaseCount;

    public PeriodStats(long salesCents, long purchaseCents, long grossProfitCents,
                       int salesQuantity, int purchaseQuantity, int salesCount, int purchaseCount) {
        this.salesCents = salesCents;
        this.purchaseCents = purchaseCents;
        this.grossProfitCents = grossProfitCents;
        this.salesQuantity = salesQuantity;
        this.purchaseQuantity = purchaseQuantity;
        this.salesCount = salesCount;
        this.purchaseCount = purchaseCount;
    }
}
