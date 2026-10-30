package com.workbuddy.jxc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class ProductEditActivity extends Activity {
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private InventoryDatabase database;
    private long productId;
    private Product product;
    private ProductFormView form;
    private TextView stockText;
    private LinearLayout movements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = InventoryDatabase.getInstance(this);
        productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
        if (productId >= 0) {
            product = database.getProduct(productId);
            if (product == null) {
                Toast.makeText(this, "商品不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (product != null && stockText != null) {
            product = database.getProduct(productId);
            if (product == null) {
                finish();
                return;
            }
            updateStock();
            showMovements();
        }
    }

    private void buildScreen() {
        LinearLayout root = Ui.vertical(this);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(Ui.BACKGROUND);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 18));
        LinearLayout header = Ui.horizontal(this);
        Button back = Ui.button(this, "返回", false);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        header.addView(Ui.text(this, product == null ? "  手工新增商品" : "  商品详情", 24, Ui.TEXT, true));
        root.addView(header);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = Ui.vertical(this);
        scrollView.addView(content, Ui.matchWrap());
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout card = Ui.card(this);
        stockText = Ui.text(this, product == null ? "货号保存后自动生成" : "", 15, Ui.PRIMARY, true);
        card.addView(stockText);
        form = new ProductFormView(this, product, null, product == null);
        card.addView(form.getView(), Ui.matchWrap());
        Button save = Ui.button(this, product == null ? "确认新增" : "保存修改", true);
        save.setOnClickListener(v -> saveProduct());
        Ui.add(card, save, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50), 18);
        Ui.add(content, card, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 18);

        if (product != null) {
            updateStock();
            LinearLayout actions = Ui.card(this);
            actions.addView(Ui.text(this, "业务操作", 19, Ui.TEXT, true));
            LinearLayout buttons = Ui.horizontal(this);
            Button inbound = Ui.button(this, "记进货", false);
            inbound.setOnClickListener(v -> openRecord(true));
            LinearLayout.LayoutParams inboundParams = Ui.weightParams(Ui.dp(this, 50));
            inboundParams.setMarginEnd(Ui.dp(this, 10));
            buttons.addView(inbound, inboundParams);
            Button outbound = Ui.button(this, "记销售", true);
            outbound.setOnClickListener(v -> openRecord(false));
            buttons.addView(outbound, Ui.weightParams(Ui.dp(this, 50)));
            Ui.add(actions, buttons, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 14);
            Ui.add(content, actions, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 14);

            LinearLayout historyCard = Ui.card(this);
            historyCard.addView(Ui.text(this, "最近库存流水", 19, Ui.TEXT, true));
            movements = Ui.vertical(this);
            Ui.add(historyCard, movements, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 8);
            Ui.add(content, historyCard, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 14);

            Button delete = Ui.button(this, "删除商品及其业务记录", false);
            delete.setOnClickListener(v -> confirmDelete());
            Ui.add(content, delete, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50), 14);
            showMovements();
        }
        setContentView(root);
    }

    private void updateStock() {
        stockText.setText("货号 " + product.code + "  ·  当前库存 " + product.stock + " 件");
    }

    private void saveProduct() {
        try {
            ProductDraft input = form.read();
            if (product == null) {
                Product created = database.createProduct(input.name, input.purchasePriceCents,
                        input.salePriceCents, input.stock, input.season, input.style, input.category,
                        input.photoPath, input.photoPaths);
                Toast.makeText(this, "已新增，货号 " + created.code, Toast.LENGTH_LONG).show();
                finish();
            } else {
                product = database.updateProduct(product.id, input.name, input.purchasePriceCents,
                        input.salePriceCents, input.season, input.style, input.category,
                        input.photoPath, input.photoPaths);
                updateStock();
                Toast.makeText(this, "商品信息已保存", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openRecord(boolean purchase) {
        Intent intent = new Intent(this, RecordActivity.class);
        intent.putExtra(RecordActivity.EXTRA_IS_PURCHASE, purchase);
        intent.putExtra(RecordActivity.EXTRA_PRODUCT_ID, product.id);
        startActivity(intent);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this).setTitle("永久删除商品？")
                .setMessage("将同时删除“" + product.name + "”的全部进货、销售和库存流水，且影响历史统计。此操作不可恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("永久删除", (d, w) -> {
                    database.deleteProduct(product.id);
                    Toast.makeText(this, "商品及其关联记录已删除", Toast.LENGTH_SHORT).show();
                    finish();
                }).show();
    }

    private void showMovements() {
        movements.removeAllViews();
        List<StockMovement> items = database.getMovements(product.id, 30);
        if (items.isEmpty()) {
            movements.addView(Ui.text(this, "暂无库存流水", 14, Ui.MUTED, false));
            return;
        }
        for (StockMovement movement : items) {
            LinearLayout row = Ui.vertical(this);
            row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
            LinearLayout top = Ui.horizontal(this);
            String label = movement.type.equals("IN") ? "采购入库" :
                    movement.type.equals("OUT") ? "销售出库" : "初始库存";
            top.addView(Ui.text(this, label, 15, Ui.TEXT, true),
                    Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
            String prefix = movement.type.equals("OUT") ? "-" : "+";
            top.addView(Ui.text(this, prefix + movement.quantity + " 件", 16,
                    movement.type.equals("OUT") ? Ui.PRIMARY : Ui.GREEN, true));
            row.addView(top);
            String detail = "库存 " + movement.stockBefore + " → " + movement.stockAfter
                    + "  ·  " + Formatters.dateTime(movement.createdAt);
            if (!movement.note.isEmpty()) {
                detail += "\n" + movement.note;
            }
            row.addView(Ui.text(this, detail, 13, Ui.MUTED, false));
            movements.addView(row, Ui.matchWrap());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        form.onActivityResult(requestCode, resultCode, data);
    }
}
