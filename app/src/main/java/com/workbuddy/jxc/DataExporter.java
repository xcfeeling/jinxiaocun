package com.workbuddy.jxc;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DataExporter {
    private DataExporter() {
    }

    public static void writeJson(Context context, Uri destination) throws Exception {
        InventoryDatabase db = InventoryDatabase.getInstance(context);
        JSONObject root = new JSONObject();
        root.put("schemaVersion", 3);
        root.put("exportedAt", dateTime(System.currentTimeMillis()));
        root.put("products", productsJson(db.searchProducts("")));
        root.put("purchases", recordsJson(db.getRecords(true, 1000000)));
        root.put("sales", recordsJson(db.getRecords(false, 1000000)));
        JSONArray movements = new JSONArray();
        for (StockMovement movement : db.getMovements(-1, 1000000)) {
            JSONObject item = new JSONObject();
            item.put("id", movement.id);
            item.put("productId", movement.productId);
            item.put("type", movement.type);
            item.put("quantity", movement.quantity);
            item.put("stockBefore", movement.stockBefore);
            item.put("stockAfter", movement.stockAfter);
            item.put("note", movement.note);
            item.put("businessDate", dateTime(movement.createdAt));
            item.put("purchaseRecordId", movement.purchaseRecordId == null ? JSONObject.NULL : movement.purchaseRecordId);
            item.put("saleRecordId", movement.saleRecordId == null ? JSONObject.NULL : movement.saleRecordId);
            movements.put(item);
        }
        root.put("stockMovements", movements);
        try (OutputStream output = context.getContentResolver().openOutputStream(destination, "wt")) {
            if (output == null) throw new IOException("无法打开导出文件");
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void writeExcel(Context context, Uri destination) throws Exception {
        InventoryDatabase db = InventoryDatabase.getInstance(context);
        List<Sheet> sheets = new ArrayList<>();
        List<List<String>> products = new ArrayList<>();
        products.add(row("货号", "商品名称", "分类", "季节", "风格", "进货价(元)", "销售价(元)",
                "当前库存(件)", "库存成本(元)", "封面文件", "全部图片文件", "创建时间"));
        for (Product product : db.searchProducts("")) {
            products.add(row(product.code, product.name, product.category, product.seasonLabel(), product.style,
                    money(product.purchasePriceCents), money(product.salePriceCents),
                    String.valueOf(product.stock), money(product.stock * product.purchasePriceCents),
                    fileName(product.photoPath), photoFiles(product), dateTime(product.createdAt)));
        }
        sheets.add(new Sheet("商品库存", products));
        sheets.add(new Sheet("进货记录", recordRows(db.getRecords(true, 1000000), true)));
        sheets.add(new Sheet("销售记录", recordRows(db.getRecords(false, 1000000), false)));
        List<List<String>> movements = new ArrayList<>();
        movements.add(row("流水ID", "商品ID", "类型", "数量", "变动前库存", "变动后库存",
                "业务时间", "备注", "进货单ID", "销售单ID"));
        for (StockMovement movement : db.getMovements(-1, 1000000)) {
            movements.add(row(String.valueOf(movement.id), String.valueOf(movement.productId), movement.type,
                    String.valueOf(movement.quantity), String.valueOf(movement.stockBefore),
                    String.valueOf(movement.stockAfter), dateTime(movement.createdAt), movement.note,
                    String.valueOf(movement.purchaseRecordId == null ? "" : movement.purchaseRecordId),
                    String.valueOf(movement.saleRecordId == null ? "" : movement.saleRecordId)));
        }
        sheets.add(new Sheet("库存流水", movements));

        try (OutputStream raw = context.getContentResolver().openOutputStream(destination, "wt");
             ZipOutputStream zip = new ZipOutputStream(raw)) {
            entry(zip, "[Content_Types].xml", contentTypes(sheets.size()));
            entry(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip, "xl/workbook.xml", workbook(sheets));
            entry(zip, "xl/_rels/workbook.xml.rels", workbookRelationships(sheets.size()));
            for (int i = 0; i < sheets.size(); i++) {
                entry(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", worksheet(sheets.get(i)));
            }
        }
    }

    private static JSONArray productsJson(List<Product> products) {
        JSONArray array = new JSONArray();
        try {
            for (Product product : products) {
                JSONObject item = new JSONObject();
                item.put("id", product.id);
                item.put("code", product.code);
                item.put("name", product.name);
                item.put("season", product.season);
                item.put("seasonLabel", product.seasonLabel());
                item.put("style", product.style);
                item.put("category", product.category);
                item.put("purchasePrice", money(product.purchasePriceCents));
                item.put("salePrice", money(product.salePriceCents));
                item.put("stock", product.stock);
                item.put("stockCost", money(product.stock * product.purchasePriceCents));
                item.put("photoFile", fileName(product.photoPath));
                JSONArray photos = new JSONArray();
                for (String path : product.photoPaths) photos.put(fileName(path));
                item.put("photoFiles", photos);
                item.put("createdAt", dateTime(product.createdAt));
                item.put("updatedAt", dateTime(product.updatedAt));
                array.put(item);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return array;
    }

    private static JSONArray recordsJson(List<BusinessRecord> records) {
        JSONArray array = new JSONArray();
        try {
            for (BusinessRecord record : records) {
                JSONObject item = new JSONObject();
                item.put("id", record.id);
                item.put("productId", record.productId);
                item.put("productCode", record.productCode);
                item.put("productName", record.productName);
                item.put("quantity", record.quantity);
                item.put("unitPrice", money(record.unitPriceCents));
                item.put("total", money(record.totalCents()));
                if (!record.purchase) {
                    item.put("costPrice", money(record.costPriceCents));
                    item.put("grossProfit", money(record.profitCents()));
                }
                item.put(record.purchase ? "supplier" : "customer", record.partner);
                item.put("note", record.note);
                item.put("businessDate", dateTime(record.businessDate));
                array.put(item);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return array;
    }

    private static List<List<String>> recordRows(List<BusinessRecord> records, boolean purchase) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(purchase ? row("进货单ID", "货号", "商品", "数量", "进货单价(元)", "进货总额(元)",
                "供应商", "业务日期", "备注") : row("销售单ID", "货号", "商品", "数量",
                "销售单价(元)", "销售总额(元)", "成本单价(元)", "毛利(元)", "客户", "业务日期", "备注"));
        for (BusinessRecord record : records) {
            if (purchase) {
                rows.add(row(String.valueOf(record.id), record.productCode, record.productName,
                        String.valueOf(record.quantity), money(record.unitPriceCents), money(record.totalCents()),
                        record.partner, dateTime(record.businessDate), record.note));
            } else {
                rows.add(row(String.valueOf(record.id), record.productCode, record.productName,
                        String.valueOf(record.quantity), money(record.unitPriceCents), money(record.totalCents()),
                        money(record.costPriceCents), money(record.profitCents()), record.partner,
                        dateTime(record.businessDate), record.note));
            }
        }
        return rows;
    }

    private static String contentTypes(int count) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        for (int i = 1; i <= count; i++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return xml.append("</Types>").toString();
    }

    private static String workbook(List<Sheet> sheets) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < sheets.size(); i++) {
            xml.append("<sheet name=\"").append(escape(sheets.get(i).name)).append("\" sheetId=\"")
                    .append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return xml.append("</sheets></workbook>").toString();
    }

    private static String workbookRelationships(int count) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= count; i++) {
            xml.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i).append(".xml\"/>");
        }
        return xml.append("</Relationships>").toString();
    }

    private static String worksheet(Sheet sheet) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (List<String> row : sheet.rows) {
            xml.append("<row>");
            for (String value : row) {
                xml.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        .append(escape(value)).append("</t></is></c>");
            }
            xml.append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String escape(String value) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        return cleaned.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static List<String> row(String... values) {
        List<String> row = new ArrayList<>();
        for (String value : values) row.add(value == null ? "" : value);
        return row;
    }

    private static String money(long cents) {
        return Formatters.editableMoney(cents);
    }

    private static String dateTime(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date(timestamp));
    }

    private static String fileName(String path) {
        return path == null || path.isEmpty() ? "" : new java.io.File(path).getName();
    }

    private static String photoFiles(Product product) {
        StringBuilder names = new StringBuilder();
        for (String path : product.photoPaths) {
            if (names.length() > 0) names.append(" | ");
            names.append(fileName(path));
        }
        return names.toString();
    }

    private static class Sheet {
        final String name;
        final List<List<String>> rows;
        Sheet(String name, List<List<String>> rows) {
            this.name = name;
            this.rows = rows;
        }
    }
}
