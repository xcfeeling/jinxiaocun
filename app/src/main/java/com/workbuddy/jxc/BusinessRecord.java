package com.workbuddy.jxc;

public class BusinessRecord {
    public final long id;
    public final long productId;
    public final String productName;
    public final String productCode;
    public final int quantity;
    public final long unitPriceCents;
    public final long costPriceCents;
    public final String partner;
    public final String note;
    public final long businessDate;
    public final boolean purchase;

    public BusinessRecord(long id, long productId, String productName, String productCode,
                          int quantity, long unitPriceCents, long costPriceCents, String partner,
                          String note, long businessDate, boolean purchase) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.costPriceCents = costPriceCents;
        this.partner = partner;
        this.note = note;
        this.businessDate = businessDate;
        this.purchase = purchase;
    }

    public long totalCents() {
        return Math.multiplyExact(quantity, unitPriceCents);
    }

    public long profitCents() {
        return purchase ? 0 : Math.multiplyExact(quantity, unitPriceCents - costPriceCents);
    }
}
