package com.workbuddy.jxc;

public class BusinessDraft {
    public static final String ADD_PRODUCT = "ADD_PRODUCT";
    public static final String PURCHASE = "PURCHASE";
    public static final String SALE = "SALE";

    public final String intent;
    public final String productName;
    public final ProductDraft productDraft;
    public final int quantity;
    public final long unitPriceCents;
    public final String partner;
    public final long businessDate;

    public BusinessDraft(ProductDraft productDraft) {
        this.intent = ADD_PRODUCT;
        this.productName = productDraft.name;
        this.productDraft = productDraft;
        this.quantity = 0;
        this.unitPriceCents = 0;
        this.partner = "";
        this.businessDate = System.currentTimeMillis();
    }

    public BusinessDraft(String intent, String productName, int quantity, long unitPriceCents,
                         String partner, long businessDate) {
        if (!PURCHASE.equals(intent) && !SALE.equals(intent)) {
            throw new IllegalArgumentException("无法判断是进货还是销售，请补充意图");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("没有识别到商品名称");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("没有识别到有效数量");
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("单价不能为负数");
        }
        this.intent = intent;
        this.productName = productName.trim();
        this.productDraft = null;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.partner = partner == null ? "" : partner.trim();
        this.businessDate = businessDate;
    }
}
