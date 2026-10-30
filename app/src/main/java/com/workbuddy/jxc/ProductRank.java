package com.workbuddy.jxc;

public class ProductRank {
    public final long productId;
    public final String name;
    public final String code;
    public final String photoPath;
    public final int quantity;
    public final long amountCents;
    public final int stock;
    public final long lastSoldAt;
    public final int stockDays;

    public ProductRank(long productId, String name, String code, String photoPath, int quantity,
                       long amountCents, int stock, long lastSoldAt, int stockDays) {
        this.productId = productId;
        this.name = name;
        this.code = code;
        this.photoPath = photoPath;
        this.quantity = quantity;
        this.amountCents = amountCents;
        this.stock = stock;
        this.lastSoldAt = lastSoldAt;
        this.stockDays = stockDays;
    }
}
