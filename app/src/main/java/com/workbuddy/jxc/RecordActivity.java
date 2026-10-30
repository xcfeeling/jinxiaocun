package com.workbuddy.jxc;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class RecordActivity extends Activity {
    public static final String EXTRA_IS_PURCHASE = "is_purchase";
    public static final String EXTRA_PRODUCT_ID = "product_id";
    public static final String EXTRA_QUANTITY = "quantity";
    public static final String EXTRA_UNIT_PRICE = "unit_price";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean purchase = getIntent().getBooleanExtra(EXTRA_IS_PURCHASE, true);
        long productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
        LinearLayout root = Ui.vertical(this);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(Ui.BACKGROUND);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 18));
        LinearLayout header = Ui.horizontal(this);
        Button back = Ui.button(this, "返回", false);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        header.addView(Ui.text(this, purchase ? "  进货管理" : "  销售管理", 24, Ui.TEXT, true));
        root.addView(header);
        ScrollView scroll = new ScrollView(this);
        RecordFormView form = new RecordFormView(this, purchase, productId, this::finish);
        int plannedQuantity = getIntent().getIntExtra(EXTRA_QUANTITY, 0);
        if (purchase && plannedQuantity > 0) {
            form.applyPurchasePlan(productId, plannedQuantity,
                    getIntent().getLongExtra(EXTRA_UNIT_PRICE, 0));
        }
        scroll.addView(form.getView(), Ui.matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }
}
