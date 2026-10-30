package com.workbuddy.jxc;

public class TrendPoint {
    public final String label;
    public final long salesCents;
    public final long purchaseCents;

    public TrendPoint(String label, long salesCents, long purchaseCents) {
        this.label = label;
        this.salesCents = salesCents;
        this.purchaseCents = purchaseCents;
    }
}
