package com.workbuddy.jxc;

import java.util.ArrayList;
import java.util.List;

public class Product {
    public final long id;
    public final String code;
    public final String name;
    public final long purchasePriceCents;
    public final long salePriceCents;
    public final int stock;
    public final String season;
    public final String style;
    public final String category;
    public final String photoPath;
    public final List<String> photoPaths;
    public final long createdAt;
    public final long updatedAt;

    public Product(long id, String code, String name, long purchasePriceCents,
                   long salePriceCents, int stock, String season, String style, String category,
                   String photoPath, List<String> photoPaths, long createdAt, long updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.purchasePriceCents = purchasePriceCents;
        this.salePriceCents = salePriceCents;
        this.stock = stock;
        this.season = season;
        this.style = style;
        this.category = category;
        this.photoPath = photoPath;
        this.photoPaths = photoPaths == null ? new ArrayList<>() : photoPaths;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String seasonLabel() {
        switch (season) {
            case "SPRING": return "春";
            case "SUMMER": return "夏";
            case "AUTUMN": return "秋";
            case "WINTER": return "冬";
            default: return "四季款";
        }
    }
}
