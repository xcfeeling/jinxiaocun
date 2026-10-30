package com.workbuddy.jxc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceBusinessActivity extends Activity implements VoiceInputController.Listener {
    public static final String EXTRA_PREFERRED_INTENT = "preferred_intent";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private VoiceInputController voiceController;
    private EditText commandInput;
    private Button micButton;
    private Button parseButton;
    private LinearLayout formContainer;
    private ProductFormView productForm;
    private String preferredIntent;
    private boolean listening;
    private boolean parsing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferredIntent = getIntent().getStringExtra(EXTRA_PREFERRED_INTENT);
        voiceController = new VoiceInputController(this, this);
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root = Ui.vertical(this);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(Ui.BACKGROUND);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 16));
        LinearLayout header = Ui.horizontal(this);
        Button back = Ui.button(this, "返回", false);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        LinearLayout title = Ui.vertical(this);
        title.setPadding(Ui.dp(this, 14), 0, 0, 0);
        title.addView(Ui.text(this, "语音业务助手", 27, Ui.TEXT, true));
        title.addView(Ui.text(this, "新增商品、进货、销售，一句话完成录入", 13, Ui.MUTED, false));
        header.addView(title);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.vertical(this);
        scroll.addView(content, Ui.matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout voiceCard = Ui.card(this);
        voiceCard.addView(Ui.text(this, "说出要办理的业务", 20, Ui.TEXT, true));
        voiceCard.addView(Ui.text(this, "例如：\n新增商品：新增裙子，进货价60，销售价70，库存20\n进货：连衣裙进货20件，进价60，供应商广州美衣\n销售：裙子卖出3件，售价70，客户张姐", 14, Ui.MUTED, false));
        commandInput = Ui.input(this, "也可以在这里直接输入完整业务内容", true);
        Ui.add(voiceCard, commandInput, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 110), 12);
        micButton = Ui.button(this, "开始说话", true);
        micButton.setOnClickListener(v -> {
            if (listening) voiceController.stop(); else voiceController.start();
        });
        Ui.add(voiceCard, micButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52), 10);
        parseButton = Ui.button(this, "识别意图并填写表单", true);
        parseButton.setOnClickListener(v -> parseCommand());
        Ui.add(voiceCard, parseButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48), 8);
        String parser = new AppSettings(this).hasDeepSeekApiKey() ? "DeepSeek + 本地规则备用" : "本地规则";
        voiceCard.addView(Ui.text(this, "当前解析：" + parser + " · 确认前不会修改数据", 12, Ui.MUTED, false));
        Ui.add(content, voiceCard, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 14);
        formContainer = Ui.vertical(this);
        Ui.add(content, formContainer, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 14);
        formContainer.addView(Ui.text(this, "识别后将在这里显示可编辑的业务表单", 14, Ui.MUTED, false));
        setContentView(root);
    }

    private void parseCommand() {
        if (parsing) return;
        String command = commandInput.getText().toString().trim();
        if (command.isEmpty()) {
            commandInput.setError("请先说出或输入业务内容");
            return;
        }
        String apiKey = new AppSettings(this).getDeepSeekApiKey();
        parsing = true;
        parseButton.setEnabled(false);
        parseButton.setText("正在识别业务意图…");
        executor.execute(() -> {
            BusinessDraft draft = null;
            Exception deepSeekError = null;
            String source = "本地规则";
            if (!apiKey.isEmpty()) {
                try {
                    draft = DeepSeekBusinessParser.parse(command, apiKey, preferredIntent);
                    source = "DeepSeek";
                } catch (Exception e) {
                    deepSeekError = e;
                }
            }
            if (draft == null) {
                try {
                    draft = LocalBusinessParser.parse(command, preferredIntent);
                } catch (Exception localError) {
                    String message = deepSeekError == null ? localError.getMessage() :
                            deepSeekError.getMessage() + "\n本地解析：" + localError.getMessage();
                    runOnUiThread(() -> showError(message));
                    return;
                }
            }
            BusinessDraft result = draft;
            String finalSource = source;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                parsing = false;
                parseButton.setEnabled(true);
                parseButton.setText("识别意图并填写表单");
                try {
                    showDraft(result);
                    Toast.makeText(this, finalSource + "识别完成，请核对表单", Toast.LENGTH_LONG).show();
                } catch (IllegalArgumentException e) {
                    showError(e.getMessage());
                }
            });
        });
    }

    private void showDraft(BusinessDraft draft) {
        formContainer.removeAllViews();
        productForm = null;
        if (BusinessDraft.ADD_PRODUCT.equals(draft.intent)) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.text(this, "确认新增商品", 21, Ui.TEXT, true));
            card.addView(Ui.text(this, "可继续修改分类、季节、风格和图片，货号保存后自动生成", 12, Ui.MUTED, false));
            productForm = new ProductFormView(this, null, draft.productDraft, true);
            card.addView(productForm.getView(), Ui.matchWrap());
            Button save = Ui.button(this, "确认新增商品", true);
            save.setOnClickListener(v -> saveProduct());
            Ui.add(card, save, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52), 14);
            formContainer.addView(card, Ui.matchWrap());
            return;
        }
        Product matched = matchProduct(draft.productName);
        if (matched == null) {
            throw new IllegalArgumentException("没有找到商品“" + draft.productName + "”，请先新增商品建档");
        }
        RecordFormView form = new RecordFormView(this, BusinessDraft.PURCHASE.equals(draft.intent),
                matched.id, this::finish);
        form.applyVoiceDraft(matched.id, draft);
        formContainer.addView(form.getView(), Ui.matchWrap());
        Toast.makeText(this, "已匹配：" + matched.code + " · " + matched.name, Toast.LENGTH_LONG).show();
    }

    private Product matchProduct(String value) {
        List<Product> matches = InventoryDatabase.getInstance(this).searchProducts(value);
        Product partial = null;
        for (Product product : matches) {
            if (product.name.equalsIgnoreCase(value) || product.code.equalsIgnoreCase(value)) return product;
            if (partial == null && (product.name.contains(value) || value.contains(product.name))) partial = product;
        }
        return partial;
    }

    private void saveProduct() {
        try {
            ProductDraft input = productForm.read();
            Product created = InventoryDatabase.getInstance(this).createProduct(input.name,
                    input.purchasePriceCents, input.salePriceCents, input.stock, input.season,
                    input.style, input.category, input.photoPath, input.photoPaths);
            Toast.makeText(this, created.name + " 已新增，货号 " + created.code, Toast.LENGTH_LONG).show();
            finish();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showError(String message) {
        if (isDestroyed()) return;
        parsing = false;
        parseButton.setEnabled(true);
        parseButton.setText("识别意图并填写表单");
        new AlertDialog.Builder(this).setTitle("无法完成识别")
                .setMessage(message).setPositiveButton("修改内容", null).show();
    }

    @Override public void onListeningChanged(boolean value) {
        listening = value;
        micButton.setText(value ? "说完后点击停止" : "开始说话");
    }
    @Override public void onPartialText(String text) { commandInput.setText(text); }
    @Override public void onResult(String text) { commandInput.setText(text); parseCommand(); }
    @Override public void onError(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        voiceController.onPermissionResult(requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (productForm != null) productForm.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        voiceController.destroy();
        executor.shutdownNow();
        super.onDestroy();
    }
}
