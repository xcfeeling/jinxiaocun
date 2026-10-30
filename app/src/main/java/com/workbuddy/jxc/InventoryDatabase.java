package com.workbuddy.jxc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InventoryDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 3;
    private static InventoryDatabase instance;

    public static synchronized InventoryDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new InventoryDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private InventoryDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "code TEXT NOT NULL UNIQUE," +
                "name TEXT NOT NULL," +
                "purchase_price INTEGER NOT NULL CHECK(purchase_price >= 0)," +
                "sale_price INTEGER NOT NULL CHECK(sale_price >= 0)," +
                "stock INTEGER NOT NULL CHECK(stock >= 0)," +
                "season TEXT NOT NULL DEFAULT 'ALL'," +
                "style TEXT NOT NULL DEFAULT ''," +
                "category TEXT NOT NULL DEFAULT ''," +
                "photo_path TEXT NOT NULL DEFAULT ''," +
                "photo_paths TEXT NOT NULL DEFAULT '[]'," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL)");
        createBusinessTables(db);
        db.execSQL("CREATE TABLE stock_movements (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "product_id INTEGER NOT NULL," +
                "type TEXT NOT NULL," +
                "quantity INTEGER NOT NULL," +
                "stock_before INTEGER NOT NULL," +
                "stock_after INTEGER NOT NULL," +
                "note TEXT NOT NULL DEFAULT ''," +
                "created_at INTEGER NOT NULL," +
                "purchase_record_id INTEGER," +
                "sale_record_id INTEGER," +
                "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE," +
                "FOREIGN KEY(purchase_record_id) REFERENCES purchase_records(id) ON DELETE CASCADE," +
                "FOREIGN KEY(sale_record_id) REFERENCES sales_records(id) ON DELETE CASCADE)");
        createMovementIndices(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE products ADD COLUMN season TEXT NOT NULL DEFAULT 'ALL'");
            db.execSQL("ALTER TABLE products ADD COLUMN style TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE products ADD COLUMN photo_path TEXT NOT NULL DEFAULT ''");
            createBusinessTables(db);
            db.execSQL("ALTER TABLE stock_movements ADD COLUMN purchase_record_id INTEGER");
            db.execSQL("ALTER TABLE stock_movements ADD COLUMN sale_record_id INTEGER");
            createMovementIndices(db);
            migrateLegacyMovements(db);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE products ADD COLUMN category TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE products ADD COLUMN photo_paths TEXT NOT NULL DEFAULT '[]'");
            try (Cursor cursor = db.query("products", new String[]{"id", "photo_path"},
                    "photo_path <> ''", null, null, null, null)) {
                while (cursor.moveToNext()) {
                    ContentValues values = new ContentValues();
                    values.put("photo_paths", new org.json.JSONArray().put(cursor.getString(1)).toString());
                    db.update("products", values, "id = ?",
                            new String[]{String.valueOf(cursor.getLong(0))});
                }
            }
        }
    }

    private void createBusinessTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS purchase_records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "product_id INTEGER NOT NULL," +
                "quantity INTEGER NOT NULL CHECK(quantity > 0)," +
                "unit_price INTEGER NOT NULL CHECK(unit_price >= 0)," +
                "partner TEXT NOT NULL DEFAULT ''," +
                "note TEXT NOT NULL DEFAULT ''," +
                "business_date INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS sales_records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "product_id INTEGER NOT NULL," +
                "quantity INTEGER NOT NULL CHECK(quantity > 0)," +
                "unit_price INTEGER NOT NULL CHECK(unit_price >= 0)," +
                "cost_price INTEGER NOT NULL CHECK(cost_price >= 0)," +
                "partner TEXT NOT NULL DEFAULT ''," +
                "note TEXT NOT NULL DEFAULT ''," +
                "business_date INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_purchase_product ON purchase_records(product_id, id DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_purchase_date ON purchase_records(business_date)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sales_product ON sales_records(product_id, id DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sales_date ON sales_records(business_date)");
    }

    private void createMovementIndices(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_movements_product ON stock_movements(product_id, id DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_movements_purchase ON stock_movements(purchase_record_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_movements_sale ON stock_movements(sale_record_id)");
    }

    private void migrateLegacyMovements(SQLiteDatabase db) {
        List<StockMovement> movements = new ArrayList<>();
        try (Cursor cursor = db.query("stock_movements", null, "type IN ('IN','OUT')",
                null, null, null, "id")) {
            while (cursor.moveToNext()) {
                movements.add(readMovement(cursor));
            }
        }
        for (StockMovement movement : movements) {
            Product product = findProduct(db, movement.productId);
            if (product == null) {
                continue;
            }
            boolean purchase = movement.type.equals("IN");
            ContentValues values = new ContentValues();
            values.put("product_id", movement.productId);
            values.put("quantity", movement.quantity);
            values.put("unit_price", purchase ? product.purchasePriceCents : product.salePriceCents);
            if (!purchase) {
                values.put("cost_price", product.purchasePriceCents);
            }
            values.put("partner", "");
            values.put("note", movement.note);
            values.put("business_date", movement.createdAt);
            values.put("created_at", movement.createdAt);
            long recordId = db.insertOrThrow(purchase ? "purchase_records" : "sales_records", null, values);
            ContentValues link = new ContentValues();
            link.put(purchase ? "purchase_record_id" : "sale_record_id", recordId);
            db.update("stock_movements", link, "id = ?",
                    new String[]{String.valueOf(movement.id)});
        }
    }

    public Product createProduct(String name, long purchasePriceCents, long salePriceCents,
                                 int initialStock) {
        return createProduct(name, purchasePriceCents, salePriceCents, initialStock, "ALL", "", "");
    }

    public Product createProduct(String name, long purchasePriceCents, long salePriceCents,
                                 int initialStock, String season, String style, String photoPath) {
        List<String> photos = clean(photoPath).isEmpty() ? new ArrayList<>() :
                java.util.Collections.singletonList(photoPath);
        return createProduct(name, purchasePriceCents, salePriceCents, initialStock,
                season, style, "", photoPath, photos);
    }

    public Product createProduct(String name, long purchasePriceCents, long salePriceCents,
                                 int initialStock, String season, String style, String category,
                                 String photoPath, List<String> photoPaths) {
        validateProduct(name, purchasePriceCents, salePriceCents, initialStock);
        List<String> photos = normalizePhotos(photoPath, photoPaths);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long id;
            try (Cursor cursor = db.rawQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM products", null)) {
                cursor.moveToFirst();
                id = cursor.getLong(0);
            }
            long now = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("code", String.format(Locale.US, "%06d", id));
            values.put("name", name.trim());
            values.put("purchase_price", purchasePriceCents);
            values.put("sale_price", salePriceCents);
            values.put("stock", initialStock);
            values.put("season", normalizeSeason(season));
            values.put("style", clean(style));
            values.put("category", clean(category));
            values.put("photo_path", photos.isEmpty() ? "" : photos.get(0));
            values.put("photo_paths", new org.json.JSONArray(photos).toString());
            values.put("created_at", now);
            values.put("updated_at", now);
            db.insertOrThrow("products", null, values);
            if (initialStock > 0) {
                insertMovement(db, id, "INITIAL", initialStock, 0, initialStock,
                        "新增商品", now, null, null);
            }
            db.setTransactionSuccessful();
            return findProduct(db, id);
        } finally {
            db.endTransaction();
        }
    }

    public Product updateProduct(long id, String name, long purchasePriceCents, long salePriceCents) {
        Product current = getProduct(id);
        if (current == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        return updateProduct(id, name, purchasePriceCents, salePriceCents, current.season,
                current.style, current.category, current.photoPath, current.photoPaths);
    }

    public Product updateProduct(long id, String name, long purchasePriceCents, long salePriceCents,
                                 String season, String style, String photoPath) {
        return updateProduct(id, name, purchasePriceCents, salePriceCents, season, style, "", photoPath,
                clean(photoPath).isEmpty() ? new ArrayList<>() : java.util.Collections.singletonList(photoPath));
    }

    public Product updateProduct(long id, String name, long purchasePriceCents, long salePriceCents,
                                 String season, String style, String category, String photoPath,
                                 List<String> photoPaths) {
        if (clean(name).isEmpty()) {
            throw new IllegalArgumentException("请输入商品名称");
        }
        if (purchasePriceCents < 0 || salePriceCents < 0) {
            throw new IllegalArgumentException("价格不能为负数");
        }
        List<String> photos = normalizePhotos(photoPath, photoPaths);
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("purchase_price", purchasePriceCents);
        values.put("sale_price", salePriceCents);
        values.put("season", normalizeSeason(season));
        values.put("style", clean(style));
        values.put("category", clean(category));
        values.put("photo_path", photos.isEmpty() ? "" : photos.get(0));
        values.put("photo_paths", new org.json.JSONArray(photos).toString());
        values.put("updated_at", System.currentTimeMillis());
        int changed = getWritableDatabase().update("products", values, "id = ?",
                new String[]{String.valueOf(id)});
        if (changed != 1) {
            throw new IllegalArgumentException("商品不存在");
        }
        return getProduct(id);
    }

    public BusinessRecord createPurchase(long productId, int quantity, long unitPriceCents,
                                         String partner, String note, long businessDate) {
        validateRecord(quantity, unitPriceCents, businessDate);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Product product = requireProduct(db, productId);
            long result = (long) product.stock + quantity;
            if (result > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("库存数量过大");
            }
            long now = System.currentTimeMillis();
            ContentValues record = recordValues(productId, quantity, unitPriceCents, partner, note,
                    businessDate, now);
            long recordId = db.insertOrThrow("purchase_records", null, record);
            ContentValues productValues = new ContentValues();
            productValues.put("stock", (int) result);
            productValues.put("purchase_price", unitPriceCents);
            productValues.put("updated_at", now);
            db.update("products", productValues, "id = ?", new String[]{String.valueOf(productId)});
            insertMovement(db, productId, "IN", quantity, product.stock, (int) result,
                    clean(note), businessDate, recordId, null);
            db.setTransactionSuccessful();
            return findRecord(db, true, recordId);
        } finally {
            db.endTransaction();
        }
    }

    public BusinessRecord createSale(long productId, int quantity, long unitPriceCents,
                                     String partner, String note, long businessDate) {
        validateRecord(quantity, unitPriceCents, businessDate);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Product product = requireProduct(db, productId);
            if (product.stock < quantity) {
                throw new IllegalArgumentException("库存不足，当前仅剩" + product.stock + "件");
            }
            int result = product.stock - quantity;
            long now = System.currentTimeMillis();
            ContentValues record = recordValues(productId, quantity, unitPriceCents, partner, note,
                    businessDate, now);
            record.put("cost_price", product.purchasePriceCents);
            long recordId = db.insertOrThrow("sales_records", null, record);
            ContentValues productValues = new ContentValues();
            productValues.put("stock", result);
            productValues.put("updated_at", now);
            db.update("products", productValues, "id = ?", new String[]{String.valueOf(productId)});
            insertMovement(db, productId, "OUT", quantity, product.stock, result,
                    clean(note), businessDate, null, recordId);
            db.setTransactionSuccessful();
            return findRecord(db, false, recordId);
        } finally {
            db.endTransaction();
        }
    }

    public void deletePurchase(long recordId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            BusinessRecord record = findRecord(db, true, recordId);
            if (record == null) {
                throw new IllegalArgumentException("进货记录不存在");
            }
            Product product = requireProduct(db, record.productId);
            if (product.stock < record.quantity) {
                throw new IllegalArgumentException("删除后会变成负库存，无法删除。请先处理相关销售记录");
            }
            db.delete("stock_movements", "purchase_record_id = ?",
                    new String[]{String.valueOf(recordId)});
            db.delete("purchase_records", "id = ?", new String[]{String.valueOf(recordId)});
            ContentValues values = new ContentValues();
            values.put("stock", product.stock - record.quantity);
            try (Cursor cursor = db.rawQuery("SELECT unit_price FROM purchase_records " +
                    "WHERE product_id = ? ORDER BY business_date DESC, id DESC LIMIT 1",
                    new String[]{String.valueOf(record.productId)})) {
                if (cursor.moveToFirst()) {
                    values.put("purchase_price", cursor.getLong(0));
                }
            }
            values.put("updated_at", System.currentTimeMillis());
            db.update("products", values, "id = ?", new String[]{String.valueOf(record.productId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteSale(long recordId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            BusinessRecord record = findRecord(db, false, recordId);
            if (record == null) {
                throw new IllegalArgumentException("销售记录不存在");
            }
            Product product = requireProduct(db, record.productId);
            long result = (long) product.stock + record.quantity;
            if (result > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("库存数量过大");
            }
            db.delete("stock_movements", "sale_record_id = ?", new String[]{String.valueOf(recordId)});
            db.delete("sales_records", "id = ?", new String[]{String.valueOf(recordId)});
            ContentValues values = new ContentValues();
            values.put("stock", (int) result);
            values.put("updated_at", System.currentTimeMillis());
            db.update("products", values, "id = ?", new String[]{String.valueOf(record.productId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteProduct(long productId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            requireProduct(db, productId);
            String[] args = {String.valueOf(productId)};
            db.delete("stock_movements", "product_id = ?", args);
            db.delete("purchase_records", "product_id = ?", args);
            db.delete("sales_records", "product_id = ?", args);
            db.delete("products", "id = ?", args);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Product getProduct(long id) {
        return findProduct(getReadableDatabase(), id);
    }

    private Product requireProduct(SQLiteDatabase db, long id) {
        Product product = findProduct(db, id);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        return product;
    }

    private Product findProduct(SQLiteDatabase db, long id) {
        try (Cursor cursor = db.query("products", null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return cursor.moveToFirst() ? readProduct(cursor) : null;
        }
    }

    public List<Product> searchProducts(String query) {
        List<Product> products = new ArrayList<>();
        String trimmed = clean(query);
        String selection = null;
        String[] args = null;
        if (!trimmed.isEmpty()) {
            selection = "(name LIKE ? OR code LIKE ? OR style LIKE ? OR category LIKE ? OR season = ?)";
            String pattern = "%" + trimmed + "%";
            args = new String[]{pattern, pattern, pattern, pattern, seasonCode(trimmed)};
        }
        try (Cursor cursor = getReadableDatabase().query("products", null, selection, args,
                null, null, "updated_at DESC, id DESC")) {
            while (cursor.moveToNext()) {
                products.add(readProduct(cursor));
            }
        }
        return products;
    }

    public InventorySummary getSummary() {
        int productCount;
        long stock;
        long cost;
        long retail;
        int lowStock;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*), COALESCE(SUM(stock), 0), " +
                        "COALESCE(SUM(stock * purchase_price), 0), " +
                        "COALESCE(SUM(stock * sale_price), 0), " +
                        "COALESCE(SUM(CASE WHEN stock <= 5 THEN 1 ELSE 0 END), 0) FROM products", null)) {
            cursor.moveToFirst();
            productCount = cursor.getInt(0);
            stock = cursor.getLong(1);
            cost = cursor.getLong(2);
            retail = cursor.getLong(3);
            lowStock = cursor.getInt(4);
        }
        long threshold = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
        long stagnantStock = 0;
        long stagnantCost = 0;
        int stagnantProducts = 0;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(p.stock), 0), COALESCE(SUM(p.stock * p.purchase_price), 0), COUNT(*) " +
                        "FROM products p WHERE p.stock > 0 AND p.created_at < ? AND NOT EXISTS " +
                        "(SELECT 1 FROM sales_records s WHERE s.product_id = p.id AND s.business_date >= ?)",
                new String[]{String.valueOf(threshold), String.valueOf(threshold)})) {
            cursor.moveToFirst();
            stagnantStock = cursor.getLong(0);
            stagnantCost = cursor.getLong(1);
            stagnantProducts = cursor.getInt(2);
        }
        return new InventorySummary(productCount, stock, cost, retail, lowStock,
                stagnantStock, stagnantCost, stagnantProducts);
    }

    public boolean isProductStagnant(long productId, long threshold) {
        Product product = getProduct(productId);
        if (product == null || product.stock <= 0 || product.createdAt >= threshold) {
            return false;
        }
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM sales_records WHERE product_id = ? AND business_date >= ? LIMIT 1",
                new String[]{String.valueOf(productId), String.valueOf(threshold)})) {
            return !cursor.moveToFirst();
        }
    }

    public PeriodStats getPeriodStats(long start, long end) {
        long sales = total(false, "quantity * unit_price", start, end);
        long purchases = total(true, "quantity * unit_price", start, end);
        long profit = total(false, "quantity * (unit_price - cost_price)", start, end);
        int salesQuantity = (int) total(false, "quantity", start, end);
        int purchaseQuantity = (int) total(true, "quantity", start, end);
        int salesCount = (int) total(false, "1", start, end);
        int purchaseCount = (int) total(true, "1", start, end);
        return new PeriodStats(sales, purchases, profit, salesQuantity, purchaseQuantity,
                salesCount, purchaseCount);
    }

    private long total(boolean purchase, String expression, long start, long end) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(" + expression +
                        "), 0) FROM " + (purchase ? "purchase_records" : "sales_records") +
                        " WHERE business_date >= ? AND business_date < ?",
                new String[]{String.valueOf(start), String.valueOf(end)})) {
            cursor.moveToFirst();
            return cursor.getLong(0);
        }
    }

    public List<TrendPoint> getTrend(List<DateRanges.Range> ranges) {
        List<TrendPoint> points = new ArrayList<>();
        for (DateRanges.Range range : ranges) {
            points.add(new TrendPoint(range.label,
                    total(false, "quantity * unit_price", range.start, range.end),
                    total(true, "quantity * unit_price", range.start, range.end)));
        }
        return points;
    }

    public List<CategoryStats> getCategoryStats(boolean season, long start, long end) {
        String column = season ? "p.season" : "COALESCE(NULLIF(p.style, ''), '未设置')";
        Map<String, long[]> values = new LinkedHashMap<>();
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT " + column +
                ", COALESCE(SUM(p.stock), 0), COALESCE(SUM(p.stock * p.purchase_price), 0) " +
                "FROM products p GROUP BY " + column, null)) {
            while (cursor.moveToNext()) {
                values.put(cursor.getString(0), new long[]{0, 0, cursor.getLong(1), cursor.getLong(2)});
            }
        }
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT " + column +
                        ", COALESCE(SUM(s.quantity), 0), COALESCE(SUM(s.quantity * s.unit_price), 0) " +
                        "FROM sales_records s JOIN products p ON p.id = s.product_id " +
                        "WHERE s.business_date >= ? AND s.business_date < ? GROUP BY " + column,
                new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                long[] item = values.containsKey(key) ? values.get(key) : new long[4];
                item[0] = cursor.getLong(1);
                item[1] = cursor.getLong(2);
                values.put(key, item);
            }
        }
        List<CategoryStats> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : values.entrySet()) {
            long[] item = entry.getValue();
            String name = season ? seasonLabel(entry.getKey()) : entry.getKey();
            result.add(new CategoryStats(name, (int) item[0], item[1], item[2], item[3]));
        }
        result.sort((a, b) -> Long.compare(b.salesQuantity + b.stockQuantity,
                a.salesQuantity + a.stockQuantity));
        return result;
    }

    public List<ProductRank> getHotProducts(long start, long end, int limit) {
        List<ProductRank> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT p.id, p.name, p.code, p.photo_path, SUM(s.quantity), " +
                        "SUM(s.quantity * s.unit_price), p.stock, MAX(s.business_date), p.created_at " +
                        "FROM sales_records s JOIN products p ON p.id = s.product_id " +
                        "WHERE s.business_date >= ? AND s.business_date < ? " +
                        "GROUP BY p.id ORDER BY SUM(s.quantity) DESC, SUM(s.quantity * s.unit_price) DESC LIMIT ?",
                new String[]{String.valueOf(start), String.valueOf(end), String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                result.add(readRank(cursor));
            }
        }
        return result;
    }

    public List<ProductRank> getOverstockProducts(int limit) {
        List<ProductRank> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT p.id, p.name, p.code, p.photo_path, 0, p.stock * p.purchase_price, p.stock, " +
                        "COALESCE(MAX(s.business_date), 0), p.created_at FROM products p " +
                        "LEFT JOIN sales_records s ON s.product_id = p.id WHERE p.stock > 0 " +
                        "GROUP BY p.id ORDER BY MAX(s.business_date) IS NULL DESC, " +
                        "COALESCE(MAX(s.business_date), 0), p.stock * p.purchase_price DESC LIMIT ?",
                new String[]{String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                result.add(readRank(cursor));
            }
        }
        return result;
    }

    private ProductRank readRank(Cursor cursor) {
        long lastSold = cursor.getLong(7);
        long since = lastSold > 0 ? lastSold : cursor.getLong(8);
        int days = (int) Math.max(0, (System.currentTimeMillis() - since) / (24L * 60 * 60 * 1000));
        return new ProductRank(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                cursor.getString(3), cursor.getInt(4), cursor.getLong(5), cursor.getInt(6),
                lastSold, days);
    }

    public List<BusinessRecord> getRecords(boolean purchase, int limit) {
        List<BusinessRecord> records = new ArrayList<>();
        String table = purchase ? "purchase_records" : "sales_records";
        try (Cursor cursor = getReadableDatabase().rawQuery(recordSelect(purchase) +
                        " ORDER BY r.business_date DESC, r.id DESC LIMIT ?",
                new String[]{String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                records.add(readRecord(cursor, purchase));
            }
        }
        return records;
    }

    public BusinessRecord getRecord(boolean purchase, long id) {
        return findRecord(getReadableDatabase(), purchase, id);
    }

    private BusinessRecord findRecord(SQLiteDatabase db, boolean purchase, long id) {
        try (Cursor cursor = db.rawQuery(recordSelect(purchase) + " WHERE r.id = ?",
                new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst() ? readRecord(cursor, purchase) : null;
        }
    }

    private String recordSelect(boolean purchase) {
        return "SELECT r.id, r.product_id, p.name, p.code, r.quantity, r.unit_price, " +
                (purchase ? "0" : "r.cost_price") +
                ", r.partner, r.note, r.business_date FROM " +
                (purchase ? "purchase_records" : "sales_records") +
                " r JOIN products p ON p.id = r.product_id";
    }

    private BusinessRecord readRecord(Cursor cursor, boolean purchase) {
        return new BusinessRecord(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                cursor.getString(3), cursor.getInt(4), cursor.getLong(5), cursor.getLong(6),
                cursor.getString(7), cursor.getString(8), cursor.getLong(9), purchase);
    }

    public List<StockMovement> getMovements(long productId, int limit) {
        List<StockMovement> movements = new ArrayList<>();
        String selection = productId >= 0 ? "product_id = ?" : null;
        String[] args = productId >= 0 ? new String[]{String.valueOf(productId)} : null;
        try (Cursor cursor = getReadableDatabase().query("stock_movements", null, selection, args,
                null, null, "id DESC", String.valueOf(limit))) {
            while (cursor.moveToNext()) {
                movements.add(readMovement(cursor));
            }
        }
        return movements;
    }

    public List<StockPoint> getStockTrend(long productId) {
        List<StockPoint> points = new ArrayList<>();
        Product product = getProduct(productId);
        if (product == null) {
            return points;
        }
        for (DateRanges.Range range : DateRanges.trend(DateRanges.DAY, System.currentTimeMillis())) {
            int delta = 0;
            try (Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(CASE WHEN type IN ('IN','INITIAL') THEN quantity ELSE -quantity END), 0) " +
                            "FROM stock_movements WHERE product_id = ? AND created_at >= ?",
                    new String[]{String.valueOf(productId), String.valueOf(range.end)})) {
                cursor.moveToFirst();
                delta = cursor.getInt(0);
            }
            points.add(new StockPoint(range.label, Math.max(0, product.stock - delta)));
        }
        return points;
    }

    private Product readProduct(Cursor cursor) {
        String cover = cursor.getString(cursor.getColumnIndexOrThrow("photo_path"));
        List<String> photos = parsePhotos(cursor.getString(cursor.getColumnIndexOrThrow("photo_paths")), cover);
        return new Product(cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("code")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getLong(cursor.getColumnIndexOrThrow("purchase_price")),
                cursor.getLong(cursor.getColumnIndexOrThrow("sale_price")),
                cursor.getInt(cursor.getColumnIndexOrThrow("stock")),
                cursor.getString(cursor.getColumnIndexOrThrow("season")),
                cursor.getString(cursor.getColumnIndexOrThrow("style")),
                cursor.getString(cursor.getColumnIndexOrThrow("category")), cover, photos,
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")));
    }

    private List<String> parsePhotos(String json, String cover) {
        List<String> photos = new ArrayList<>();
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length() && photos.size() < 9; i++) {
                String path = array.optString(i);
                if (!path.isEmpty()) photos.add(path);
            }
        } catch (Exception ignored) {
        }
        if (photos.isEmpty() && !clean(cover).isEmpty()) {
            photos.add(cover);
        }
        return photos;
    }

    private StockMovement readMovement(Cursor cursor) {
        int purchaseIndex = cursor.getColumnIndexOrThrow("purchase_record_id");
        int saleIndex = cursor.getColumnIndexOrThrow("sale_record_id");
        return new StockMovement(cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("product_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("type")),
                cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                cursor.getInt(cursor.getColumnIndexOrThrow("stock_before")),
                cursor.getInt(cursor.getColumnIndexOrThrow("stock_after")),
                cursor.getString(cursor.getColumnIndexOrThrow("note")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                cursor.isNull(purchaseIndex) ? null : cursor.getLong(purchaseIndex),
                cursor.isNull(saleIndex) ? null : cursor.getLong(saleIndex));
    }

    private ContentValues recordValues(long productId, int quantity, long unitPrice,
                                       String partner, String note, long businessDate, long now) {
        ContentValues values = new ContentValues();
        values.put("product_id", productId);
        values.put("quantity", quantity);
        values.put("unit_price", unitPrice);
        values.put("partner", clean(partner));
        values.put("note", clean(note));
        values.put("business_date", businessDate);
        values.put("created_at", now);
        return values;
    }

    private void insertMovement(SQLiteDatabase db, long productId, String type, int quantity,
                                int before, int after, String note, long date, Long purchaseId,
                                Long saleId) {
        ContentValues values = new ContentValues();
        values.put("product_id", productId);
        values.put("type", type);
        values.put("quantity", quantity);
        values.put("stock_before", before);
        values.put("stock_after", after);
        values.put("note", note);
        values.put("created_at", date);
        if (purchaseId != null) {
            values.put("purchase_record_id", purchaseId);
        }
        if (saleId != null) {
            values.put("sale_record_id", saleId);
        }
        db.insertOrThrow("stock_movements", null, values);
    }

    private void validateProduct(String name, long purchasePriceCents, long salePriceCents, int stock) {
        if (clean(name).isEmpty()) {
            throw new IllegalArgumentException("请输入商品名称");
        }
        if (purchasePriceCents < 0 || salePriceCents < 0) {
            throw new IllegalArgumentException("价格不能为负数");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("库存不能为负数");
        }
    }

    private void validateRecord(int quantity, long unitPrice, long businessDate) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("单价不能为负数");
        }
        if (businessDate <= 0) {
            throw new IllegalArgumentException("请选择业务日期");
        }
    }

    private String normalizeSeason(String season) {
        if ("SPRING".equals(season) || "SUMMER".equals(season) || "AUTUMN".equals(season) ||
                "WINTER".equals(season)) {
            return season;
        }
        return "ALL";
    }

    private String seasonCode(String label) {
        switch (label) {
            case "春": return "SPRING";
            case "夏": return "SUMMER";
            case "秋": return "AUTUMN";
            case "冬": return "WINTER";
            case "四季款": return "ALL";
            default: return "";
        }
    }

    private String seasonLabel(String code) {
        switch (code) {
            case "SPRING": return "春";
            case "SUMMER": return "夏";
            case "AUTUMN": return "秋";
            case "WINTER": return "冬";
            default: return "四季款";
        }
    }

    private List<String> normalizePhotos(String cover, List<String> paths) {
        List<String> photos = new ArrayList<>();
        if (paths != null) {
            for (String path : paths) {
                String cleaned = clean(path);
                if (!cleaned.isEmpty() && !photos.contains(cleaned) && photos.size() < 9) {
                    photos.add(cleaned);
                }
            }
        }
        String cleanedCover = clean(cover);
        if (photos.isEmpty() && !cleanedCover.isEmpty()) {
            photos.add(cleanedCover);
        }
        return photos;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
