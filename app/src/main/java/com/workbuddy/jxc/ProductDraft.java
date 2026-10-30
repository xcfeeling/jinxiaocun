package com.workbuddy.jxc;

import java.util.ArrayList;
import java.util.List;

public class ProductDraft {
    public final String name;
    public final long purchasePriceCents;
    public final long salePriceCents;
    public final int stock;
    public final String season;
    public final String style;
    public final String category;
    public final String photoPath;
    public final List<String> photoPaths;

    public ProductDraft(String name, long purchasePriceCents, long salePriceCents, int stock) {
        this(name, purchasePriceCents, salePriceCents, stock, "ALL", "", "", "", new ArrayList<>());
    }

    public ProductDraft(String name, long purchasePriceCents, long salePriceCents, int stock,
                        String season, String style, String photoPath) {
        this(name, purchasePriceCents, salePriceCents, stock, season, style, "", photoPath,
                photoPath == null || photoPath.isEmpty() ? new ArrayList<>() : java.util.Collections.singletonList(photoPath));
    }

    public ProductDraft(String name, long purchasePriceCents, long salePriceCents, int stock,
                        String season, String style, String category, String photoPath,
                        List<String> photoPaths) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("没有识别到商品名称，请补充后重试");
        }
        if (purchasePriceCents < 0 || salePriceCents < 0) {
            throw new IllegalArgumentException("价格不能为负数");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("库存不能为负数");
        }
        this.name = name.trim();
        this.purchasePriceCents = purchasePriceCents;
        this.salePriceCents = salePriceCents;
        this.stock = stock;
        this.season = season;
        this.style = style == null ? "" : style.trim();
        this.category = category == null ? "" : category.trim();
        this.photoPath = photoPath == null ? "" : photoPath;
        this.photoPaths = photoPaths == null ? new ArrayList<>() : new ArrayList<>(photoPaths);
    }
}
