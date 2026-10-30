package com.workbuddy.jxc;

import android.content.ContentValues;
import android.content.Context;

public final class DemoDataSeeder {
    private DemoDataSeeder() {
    }

    public static void seedIfNeeded(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences("demo_seed", Context.MODE_PRIVATE);
        if (preferences.getBoolean("seeded", false)) return;
        InventoryDatabase db = InventoryDatabase.getInstance(context);
        if (db.getSummary().productCount > 0) {
            preferences.edit().putBoolean("seeded", true).apply();
            return;
        }
        long now = System.currentTimeMillis();
        Seed[] seeds = {
                new Seed("法式碎花连衣裙", 8900, 16800, 8, "AUTUMN", "法式", "连衣裙",
                        new int[][]{{85, 5}, {62, 8}, {41, 6}, {25, 7}, {12, 6}, {3, 8}}),
                new Seed("通勤针织开衫", 6800, 12800, 5, "AUTUMN", "通勤", "针织",
                        new int[][]{{70, 4}, {45, 6}, {29, 5}, {18, 7}, {7, 6}, {1, 7}}),
                new Seed("高腰显瘦牛仔裤", 8500, 15900, 80, "ALL", "休闲", "裤装",
                        new int[][]{{105, 10}}),
                new Seed("甜美雪纺衬衫", 5500, 11900, 65, "SPRING", "甜美", "上衣",
                        new int[][]{{82, 5}}),
                new Seed("韩系风衣", 12800, 26800, 2, "AUTUMN", "韩系", "外套",
                        new int[][]{{35, 2}, {14, 3}, {4, 3}}),
                new Seed("夏季碎花半身裙", 4500, 9900, 120, "SUMMER", "甜美", "半身裙", new int[][]{}),
                new Seed("简约羊毛大衣", 19900, 39900, 5, "WINTER", "简约", "外套",
                        new int[][]{{28, 3}, {16, 4}, {9, 5}, {2, 8}}),
                new Seed("新中式盘扣上衣", 9500, 18900, 4, "ALL", "新中式", "上衣",
                        new int[][]{{52, 3}, {31, 4}, {11, 4}, {5, 4}}),
                new Seed("小香风外套", 13900, 26900, 3, "AUTUMN", "小香风", "外套",
                        new int[][]{{44, 2}, {21, 3}, {8, 3}, {2, 4}})
        };
        for (Seed seed : seeds) {
            Product product = db.createProduct(seed.name, seed.purchasePrice, seed.salePrice, 0,
                    seed.season, seed.style, seed.category, "", new java.util.ArrayList<>());
            int totalSales = 0;
            for (int[] sale : seed.sales) totalSales += sale[1];
            int totalPurchase = seed.stock + totalSales;
            int firstPurchase = (int) Math.ceil(totalPurchase * 0.6);
            db.createPurchase(product.id, firstPurchase, seed.purchasePrice, "广州示例供应商",
                    "演示首批备货", now - 100L * 86400000L);
            db.createPurchase(product.id, totalPurchase - firstPurchase, seed.purchasePrice,
                    "杭州示例供应商", "演示补货", now - 45L * 86400000L);
            for (int[] sale : seed.sales) {
                db.createSale(product.id, sale[1], seed.salePrice, "示例客户",
                        "演示销售", now - sale[0] * 86400000L);
            }
            ContentValues values = new ContentValues();
            values.put("created_at", now - 120L * 86400000L);
            db.getWritableDatabase().update("products", values, "id = ?",
                    new String[]{String.valueOf(product.id)});
        }
        preferences.edit().putBoolean("seeded", true).apply();
    }

    private static class Seed {
        final String name;
        final long purchasePrice;
        final long salePrice;
        final int stock;
        final String season;
        final String style;
        final String category;
        final int[][] sales;

        Seed(String name, long purchasePrice, long salePrice, int stock, String season,
             String style, String category, int[][] sales) {
            this.name = name;
            this.purchasePrice = purchasePrice;
            this.salePrice = salePrice;
            this.stock = stock;
            this.season = season;
            this.style = style;
            this.category = category;
            this.sales = sales;
        }
    }
}
