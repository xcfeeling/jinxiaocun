package com.workbuddy.jxc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordFormView {
    private final Activity activity;
    private final boolean purchase;
    private final Runnable onSaved;
    private final List<Product> products;
    private final LinearLayout view;
    private final Spinner productSpinner;
    private final ImageView productImage;
    private final TextView stockText;
    private final EditText quantityInput;
    private final EditText priceInput;
    private final EditText partnerInput;
    private final EditText noteInput;
    private final TextView totalText;
    private final TextView profitText;
    private final TextView warningText;
    private final Button dateButton;
    private final Button saveButton;
    private long businessDate = System.currentTimeMillis();

    public RecordFormView(Activity activity, boolean purchase, long preselectedId, Runnable onSaved) {
        this.activity = activity;
        this.purchase = purchase;
        this.onSaved = onSaved;
        products = InventoryDatabase.getInstance(activity).searchProducts("");
        view = Ui.card(activity);
        LinearLayout titleRow = Ui.horizontal(activity);
        titleRow.addView(Ui.text(activity, purchase ? "快速进货入库" : "快速销售开单", 20, Ui.TEXT, true),
                Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        Button voice = Ui.button(activity, "语音录入", false);
        voice.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(activity, VoiceBusinessActivity.class);
            intent.putExtra(VoiceBusinessActivity.EXTRA_PREFERRED_INTENT,
                    purchase ? BusinessDraft.PURCHASE : BusinessDraft.SALE);
            activity.startActivity(intent);
        });
        LinearLayout.LayoutParams voiceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(activity, 40));
        voiceParams.setMarginEnd(Ui.dp(activity, 6));
        titleRow.addView(voice, voiceParams);
        Button clear = Ui.button(activity, "清空", false);
        clear.setOnClickListener(v -> clear());
        titleRow.addView(clear);
        view.addView(titleRow);

        view.addView(Ui.text(activity, "商品 *", 13, Ui.MUTED, true));
        productSpinner = new Spinner(activity);
        List<String> names = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            names.add(product.code + " · " + product.name);
            if (product.id == preselectedId) selected = i;
        }
        productSpinner.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, names));
        Ui.add(view, productSpinner, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 4);
        LinearLayout productInfo = Ui.horizontal(activity);
        productInfo.setPadding(Ui.dp(activity, 10), Ui.dp(activity, 10), Ui.dp(activity, 10), Ui.dp(activity, 10));
        productInfo.setBackground(Ui.roundRect(Ui.PRIMARY_LIGHT, 12, Ui.PRIMARY_LIGHT, activity));
        productImage = new ImageView(activity);
        productImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        productInfo.addView(productImage, new LinearLayout.LayoutParams(Ui.dp(activity, 58), Ui.dp(activity, 64)));
        stockText = Ui.text(activity, products.isEmpty() ? "请先新增商品" : "", 13, Ui.PRIMARY_DARK, true);
        stockText.setPadding(Ui.dp(activity, 12), 0, 0, 0);
        productInfo.addView(stockText, Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        Ui.add(view, productInfo, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 8);

        view.addView(Ui.text(activity, "数量 *", 13, Ui.MUTED, true));
        LinearLayout quantityRow = Ui.horizontal(activity);
        Button minus = Ui.button(activity, "−", false);
        minus.setOnClickListener(v -> step(-1));
        quantityRow.addView(minus);
        quantityInput = Ui.integerInput(activity, "数量");
        quantityInput.setGravity(android.view.Gravity.CENTER);
        quantityInput.setText("1");
        LinearLayout.LayoutParams quantityParams = Ui.weightParams(Ui.dp(activity, 46));
        quantityParams.setMarginStart(Ui.dp(activity, 6));
        quantityParams.setMarginEnd(Ui.dp(activity, 6));
        quantityRow.addView(quantityInput, quantityParams);
        Button plus = Ui.button(activity, "+", false);
        plus.setOnClickListener(v -> step(1));
        quantityRow.addView(plus);
        Ui.add(view, quantityRow, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 4);
        priceInput = field(purchase ? "进货单价（元）*" : "销售单价（元）*",
                Ui.decimalInput(activity, "请输入单价"));
        partnerInput = field(purchase ? "供应商" : "客户",
                Ui.input(activity, purchase ? "供应商名称（可选）" : "客户名称（可选）", false));
        dateButton = Ui.button(activity, "", false);
        dateButton.setOnClickListener(v -> chooseDate());
        Ui.add(view, dateButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 46), 10);
        noteInput = field("备注", Ui.input(activity, "备注信息（可选）", false));
        warningText = Ui.text(activity, "", 12, Ui.PRIMARY_DARK, true);
        warningText.setVisibility(View.GONE);
        view.addView(warningText);
        totalText = Ui.text(activity, "合计 ¥0", 23, Ui.PRIMARY_DARK, true);
        Ui.add(view, totalText, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        profitText = Ui.text(activity, "", 13, Ui.GREEN, true);
        view.addView(profitText);
        saveButton = Ui.button(activity, purchase ? "确认进货" : "确认销售", true);
        saveButton.setOnClickListener(v -> preview());
        Ui.add(view, saveButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 52), 10);

        productSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View selectedView, int position, long id) {
                Product product = products.get(position);
                priceInput.setText(Formatters.editableMoney(purchase ? product.purchasePriceCents : product.salePriceCents));
                updateProduct();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateAmount(); }
            @Override public void afterTextChanged(Editable s) { }
        };
        quantityInput.addTextChangedListener(watcher);
        priceInput.addTextChangedListener(watcher);
        if (!products.isEmpty()) productSpinner.setSelection(selected);
        updateDate();
        updateProduct();
        updateAmount();
    }

    public LinearLayout getView() {
        return view;
    }

    public void applyPurchasePlan(long productId, int quantity, long unitPrice) {
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).id == productId) {
                productSpinner.setSelection(i);
                break;
            }
        }
        quantityInput.setText(String.valueOf(quantity));
        priceInput.setText(Formatters.editableMoney(unitPrice));
        updateProduct();
        updateAmount();
    }

    public void applyVoiceDraft(long productId, BusinessDraft draft) {
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).id == productId) {
                productSpinner.setSelection(i);
                break;
            }
        }
        quantityInput.setText(String.valueOf(draft.quantity));
        priceInput.setText(Formatters.editableMoney(draft.unitPriceCents));
        partnerInput.setText(draft.partner);
        businessDate = draft.businessDate;
        updateDate();
        updateProduct();
        updateAmount();
    }

    private EditText field(String label, EditText input) {
        TextView labelView = Ui.text(activity, label, 13, Ui.MUTED, true);
        Ui.add(view, labelView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 10);
        Ui.add(view, input, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 4);
        return input;
    }

    private Product selectedProduct() {
        return products.isEmpty() ? null : products.get(productSpinner.getSelectedItemPosition());
    }

    private void updateProduct() {
        Product product = selectedProduct();
        if (product == null) {
            saveButton.setEnabled(false);
            return;
        }
        saveButton.setEnabled(true);
        Bitmap bitmap = PhotoManager.thumbnail(product.photoPath, Ui.dp(activity, 58));
        productImage.setImageBitmap(bitmap);
        stockText.setText(product.name + "\n当前库存 " + product.stock + " 件 · "
                + product.seasonLabel() + (product.style.isEmpty() ? "" : " / " + product.style));
        updateAmount();
    }

    private void updateAmount() {
        Product product = selectedProduct();
        if (product == null || totalText == null) return;
        try {
            int quantity = Integer.parseInt(quantityInput.getText().toString().trim());
            long price = Formatters.parseMoney(priceInput.getText().toString());
            long total = Math.multiplyExact(quantity, price);
            totalText.setText((purchase ? "进货金额  " : "销售金额  ") + Formatters.money(total));
            if (purchase) {
                profitText.setText("保存后库存增加 " + quantity + " 件");
                warningText.setVisibility(View.GONE);
                saveButton.setEnabled(quantity > 0);
            } else {
                long profit = Math.multiplyExact(quantity, price - product.purchasePriceCents);
                profitText.setText("预计毛利  " + Formatters.money(profit));
                boolean insufficient = quantity > product.stock;
                warningText.setVisibility(insufficient ? View.VISIBLE : View.GONE);
                warningText.setText("库存不足：当前仅 " + product.stock + " 件，禁止销售 " + quantity + " 件");
                saveButton.setEnabled(quantity > 0 && !insufficient);
            }
        } catch (Exception e) {
            totalText.setText("合计 --");
            profitText.setText("");
            saveButton.setEnabled(false);
        }
    }

    private void step(int amount) {
        try {
            int value = Math.max(1, Integer.parseInt(quantityInput.getText().toString().trim()) + amount);
            quantityInput.setText(String.valueOf(value));
        } catch (Exception e) {
            quantityInput.setText("1");
        }
    }

    private void updateDate() {
        dateButton.setText("业务日期：" + new SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
                .format(new Date(businessDate)));
    }

    private void chooseDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(businessDate);
        new DatePickerDialog(activity, (v, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 12, 0, 0);
            businessDate = selected.getTimeInMillis();
            updateDate();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void preview() {
        Product product = selectedProduct();
        if (product == null) return;
        try {
            int quantity = Integer.parseInt(quantityInput.getText().toString().trim());
            long price = Formatters.parseMoney(priceInput.getText().toString());
            if (quantity <= 0) throw new IllegalArgumentException("数量必须大于0");
            long total = Math.multiplyExact(quantity, price);
            new AlertDialog.Builder(activity).setTitle(purchase ? "确认进货" : "确认销售")
                    .setMessage(product.name + "\n" + quantity + " 件 × " + Formatters.money(price)
                            + "\n合计 " + Formatters.money(total) + "\n保存后同步更新库存和总览。")
                    .setNegativeButton("返回修改", null)
                    .setPositiveButton("确认保存", (d, w) -> save(product, quantity, price)).show();
        } catch (Exception e) {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void save(Product product, int quantity, long price) {
        try {
            InventoryDatabase db = InventoryDatabase.getInstance(activity);
            if (purchase) db.createPurchase(product.id, quantity, price, partnerInput.getText().toString(),
                    noteInput.getText().toString(), businessDate);
            else db.createSale(product.id, quantity, price, partnerInput.getText().toString(),
                    noteInput.getText().toString(), businessDate);
            Toast.makeText(activity, purchase ? "进货已保存，库存已增加" : "销售已保存，库存已扣减", Toast.LENGTH_LONG).show();
            if (onSaved != null) onSaved.run();
        } catch (IllegalArgumentException e) {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void clear() {
        quantityInput.setText("1");
        partnerInput.setText("");
        noteInput.setText("");
        businessDate = System.currentTimeMillis();
        updateDate();
        updateAmount();
    }
}
