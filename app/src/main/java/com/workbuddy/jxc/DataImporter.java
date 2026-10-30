package com.workbuddy.jxc;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DataImporter {
    private DataImporter() {
    }

    public static void importJson(Context context, Uri source) throws Exception {
        String text = read(context, source);
        JSONObject root = new JSONObject(text);
        int schema = root.getInt("schemaVersion");
        if (schema < 2 || schema > 3) {
            throw new IllegalArgumentException("不支持此备份版本，仅支持 schemaVersion 2 或 3");
        }
        JSONArray products = root.getJSONArray("products");
        JSONArray purchases = root.getJSONArray("purchases");
        JSONArray sales = root.getJSONArray("sales");
        JSONArray movements = root.getJSONArray("stockMovements");
        validate(products, purchases, sales, movements);

        Map<String, String> availablePhotos = availablePhotos(context);
        SQLiteDatabase db = InventoryDatabase.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("stock_movements", null, null);
            db.delete("sales_records", null, null);
            db.delete("purchase_records", null, null);
            db.delete("products", null, null);
            restoreProducts(db, products, availablePhotos);
            restoreRecords(db, purchases, true);
            restoreRecords(db, sales, false);
            restoreMovements(db, movements);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void validate(JSONArray products, JSONArray purchases, JSONArray sales,
                                 JSONArray movements) throws Exception {
        Set<Long> productIds = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < products.length(); i++) {
            JSONObject item = products.getJSONObject(i);
            long id = positiveId(item, "id");
            if (!productIds.add(id)) throw new IllegalArgumentException("商品ID重复：" + id);
            String code = required(item, "code");
            if (!codes.add(code)) throw new IllegalArgumentException("商品货号重复：" + code);
            required(item, "name");
            money(item, "purchasePrice");
            money(item, "salePrice");
            int stock = item.getInt("stock");
            if (stock < 0) throw new IllegalArgumentException("商品库存不能为负数：" + code);
            date(item, "createdAt");
            date(item, "updatedAt");
        }
        Set<Long> purchaseIds = recordIds(purchases, productIds, true);
        Set<Long> saleIds = recordIds(sales, productIds, false);
        for (int i = 0; i < movements.length(); i++) {
            JSONObject item = movements.getJSONObject(i);
            positiveId(item, "id");
            if (!productIds.contains(positiveId(item, "productId"))) {
                throw new IllegalArgumentException("库存流水关联了不存在的商品");
            }
            String type = required(item, "type");
            if (!type.equals("IN") && !type.equals("OUT") && !type.equals("INITIAL")) {
                throw new IllegalArgumentException("未知库存流水类型：" + type);
            }
            int quantity = item.getInt("quantity");
            int before = item.getInt("stockBefore");
            int after = item.getInt("stockAfter");
            if (quantity < 0 || before < 0 || after < 0) {
                throw new IllegalArgumentException("库存流水数量不能为负数");
            }
            if (type.equals("OUT") && before - after != quantity) {
                throw new IllegalArgumentException("销售出库流水前后库存不一致");
            }
            if (!type.equals("OUT") && after - before != quantity) {
                throw new IllegalArgumentException("入库流水前后库存不一致");
            }
            date(item, "businessDate");
            if (!item.isNull("purchaseRecordId") && !purchaseIds.contains(item.getLong("purchaseRecordId"))) {
                throw new IllegalArgumentException("库存流水关联了不存在的进货单");
            }
            if (!item.isNull("saleRecordId") && !saleIds.contains(item.getLong("saleRecordId"))) {
                throw new IllegalArgumentException("库存流水关联了不存在的销售单");
            }
        }
    }

    private static Set<Long> recordIds(JSONArray records, Set<Long> productIds,
                                       boolean purchase) throws Exception {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < records.length(); i++) {
            JSONObject item = records.getJSONObject(i);
            long id = positiveId(item, "id");
            if (!ids.add(id)) throw new IllegalArgumentException("业务单ID重复：" + id);
            if (!productIds.contains(positiveId(item, "productId"))) {
                throw new IllegalArgumentException("业务单关联了不存在的商品");
            }
            int quantity = item.getInt("quantity");
            if (quantity <= 0) throw new IllegalArgumentException("业务单数量必须大于0");
            long unitPrice = money(item, "unitPrice");
            long total = money(item, "total");
            try {
                if (total != Math.multiplyExact(quantity, unitPrice)) {
                    throw new IllegalArgumentException("业务单总额与数量、单价不一致");
                }
                if (!purchase) {
                    long cost = money(item, "costPrice");
                    long profit = Formatters.parseMoney(required(item, "grossProfit"));
                    if (profit != Math.multiplyExact(quantity, unitPrice - cost)) {
                        throw new IllegalArgumentException("销售单毛利与销售价、成本不一致");
                    }
                }
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("业务单金额超出支持范围");
            }
            date(item, "businessDate");
        }
        return ids;
    }

    private static void restoreProducts(SQLiteDatabase db, JSONArray products,
                                        Map<String, String> availablePhotos) throws Exception {
        for (int i = 0; i < products.length(); i++) {
            JSONObject item = products.getJSONObject(i);
            JSONArray restoredPhotos = new JSONArray();
            String cover = "";
            if (item.has("photoFiles")) {
                JSONArray files = item.getJSONArray("photoFiles");
                for (int p = 0; p < files.length() && restoredPhotos.length() < 9; p++) {
                    String path = availablePhotos.get(files.getString(p));
                    if (path != null) restoredPhotos.put(path);
                }
            }
            String legacyCover = availablePhotos.get(item.optString("photoFile"));
            if (restoredPhotos.length() == 0 && legacyCover != null) {
                restoredPhotos.put(legacyCover);
            }
            if (restoredPhotos.length() > 0) cover = restoredPhotos.getString(0);
            ContentValues values = new ContentValues();
            values.put("id", item.getLong("id"));
            values.put("code", item.getString("code"));
            values.put("name", item.getString("name"));
            values.put("purchase_price", money(item, "purchasePrice"));
            values.put("sale_price", money(item, "salePrice"));
            values.put("stock", item.getInt("stock"));
            values.put("season", item.optString("season", "ALL"));
            values.put("style", item.optString("style"));
            values.put("category", item.optString("category"));
            values.put("photo_path", cover);
            values.put("photo_paths", restoredPhotos.toString());
            values.put("created_at", date(item, "createdAt"));
            values.put("updated_at", date(item, "updatedAt"));
            db.insertOrThrow("products", null, values);
        }
    }

    private static void restoreRecords(SQLiteDatabase db, JSONArray records,
                                       boolean purchase) throws Exception {
        for (int i = 0; i < records.length(); i++) {
            JSONObject item = records.getJSONObject(i);
            ContentValues values = new ContentValues();
            values.put("id", item.getLong("id"));
            values.put("product_id", item.getLong("productId"));
            values.put("quantity", item.getInt("quantity"));
            values.put("unit_price", money(item, "unitPrice"));
            if (!purchase) values.put("cost_price", money(item, "costPrice"));
            values.put("partner", item.optString(purchase ? "supplier" : "customer"));
            values.put("note", item.optString("note"));
            long date = date(item, "businessDate");
            values.put("business_date", date);
            values.put("created_at", date);
            db.insertOrThrow(purchase ? "purchase_records" : "sales_records", null, values);
        }
    }

    private static void restoreMovements(SQLiteDatabase db, JSONArray movements) throws Exception {
        for (int i = 0; i < movements.length(); i++) {
            JSONObject item = movements.getJSONObject(i);
            ContentValues values = new ContentValues();
            values.put("id", item.getLong("id"));
            values.put("product_id", item.getLong("productId"));
            values.put("type", item.getString("type"));
            values.put("quantity", item.getInt("quantity"));
            values.put("stock_before", item.getInt("stockBefore"));
            values.put("stock_after", item.getInt("stockAfter"));
            values.put("note", item.optString("note"));
            values.put("created_at", date(item, "businessDate"));
            if (!item.isNull("purchaseRecordId")) values.put("purchase_record_id", item.getLong("purchaseRecordId"));
            if (!item.isNull("saleRecordId")) values.put("sale_record_id", item.getLong("saleRecordId"));
            db.insertOrThrow("stock_movements", null, values);
        }
    }

    private static Map<String, String> availablePhotos(Context context) {
        Map<String, String> photos = new HashMap<>();
        java.io.File[] files = PhotoManager.directory(context).listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isFile()) photos.put(file.getName(), file.getAbsolutePath());
            }
        }
        return photos;
    }

    private static long positiveId(JSONObject item, String key) throws Exception {
        long id = item.getLong(key);
        if (id <= 0) throw new IllegalArgumentException(key + " 必须大于0");
        return id;
    }

    private static String required(JSONObject item, String key) throws Exception {
        String value = item.getString(key).trim();
        if (value.isEmpty()) throw new IllegalArgumentException("缺少必要字段：" + key);
        return value;
    }

    private static long money(JSONObject item, String key) throws Exception {
        long value = Formatters.parseMoney(required(item, key));
        if (value < 0) throw new IllegalArgumentException("金额不能为负数：" + key);
        return value;
    }

    private static long date(JSONObject item, String key) throws Exception {
        String value = required(item, key);
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            format.setLenient(false);
            Date parsed = format.parse(value);
            if (parsed == null) throw new ParseException(value, 0);
            return parsed.getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("日期格式不正确：" + key);
        }
    }

    private static String read(Context context, Uri source) throws IOException {
        StringBuilder text = new StringBuilder();
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            if (input == null) throw new IOException("无法读取备份文件");
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            char[] buffer = new char[8192];
            int count;
            int total = 0;
            while ((count = reader.read(buffer)) >= 0) {
                total += count;
                if (total > 50 * 1024 * 1024) throw new IOException("备份文件不能超过50MB");
                text.append(buffer, 0, count);
            }
        }
        return text.toString();
    }
}
