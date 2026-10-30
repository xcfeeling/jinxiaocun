package com.workbuddy.jxc;

public class StockMovement {
    public final long id;
    public final long productId;
    public final String type;
    public final int quantity;
    public final int stockBefore;
    public final int stockAfter;
    public final String note;
    public final long createdAt;
    public final Long purchaseRecordId;
    public final Long saleRecordId;

    public StockMovement(long id, long productId, String type, int quantity, int stockBefore,
                         int stockAfter, String note, long createdAt, Long purchaseRecordId,
                         Long saleRecordId) {
        this.id = id;
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.note = note;
        this.createdAt = createdAt;
        this.purchaseRecordId = purchaseRecordId;
        this.saleRecordId = saleRecordId;
    }
}
