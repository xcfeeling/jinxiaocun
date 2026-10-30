package com.workbuddy.jxc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductAddActivity extends Activity implements VoiceInputController.Listener {
    private static final String EXAMPLE = "帮我添加一个商品，货号自增长，裙子，进货价60，销售价70，库存20";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private VoiceInputController voiceController;
    private ProductFormView form;
    private EditText commandInput;
    private LinearLayout voiceCard;
    private LinearLayout manualHint;
    private Button voiceMode;
    private Button manualMode;
    private Button micButton;
    private Button parseButton;
    private boolean voiceSelected = true;
    private boolean listening;
    private boolean parsing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        title.addView(Ui.text(this, "新增商品", 27, Ui.TEXT, true));
        title.addView(Ui.text(this, "语音智能填写与手工录入共用商品表单", 13, Ui.MUTED, false));
        header.addView(title);
        root.addView(header);

        LinearLayout modes = Ui.horizontal(this);
        voiceMode = Ui.button(this, "语音录入 · AI 自动填写", true);
        voiceMode.setOnClickListener(v -> setMode(true));
        LinearLayout.LayoutParams voiceParams = Ui.weightParams(Ui.dp(this, 58));
        voiceParams.setMarginEnd(Ui.dp(this, 10));
        modes.addView(voiceMode, voiceParams);
        manualMode = Ui.button(this, "手工录入 · 详细设置", false);
        manualMode.setOnClickListener(v -> setMode(false));
        modes.addView(manualMode, Ui.weightParams(Ui.dp(this, 58)));
        Ui.add(root, modes, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 16);

        ScrollView scroll = new ScrollView(this);
        boolean wide = getResources().getConfiguration().screenWidthDp >= 760;
        LinearLayout columns = wide ? Ui.horizontal(this) : Ui.vertical(this);
        columns.setGravity(android.view.Gravity.TOP);
        voiceCard = buildVoiceCard();
        LinearLayout formCard = Ui.card(this);
        formCard.addView(Ui.text(this, "商品信息", 20, Ui.TEXT, true));
        formCard.addView(Ui.text(this, "语音填写后也可修改；分类、季节、风格和图片均为详细资料", 12, Ui.MUTED, false));
        form = new ProductFormView(this, null, null, true);
        formCard.addView(form.getView(), Ui.matchWrap());
        LinearLayout actions = Ui.horizontal(this);
        Button cancel = Ui.button(this, "取消", false);
        cancel.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams cancelParams = Ui.weightParams(Ui.dp(this, 52));
        cancelParams.setMarginEnd(Ui.dp(this, 10));
        actions.addView(cancel, cancelParams);
        Button save = Ui.button(this, "确认入库", true);
        save.setOnClickListener(v -> saveProduct());
        actions.addView(save, Ui.weightParams(Ui.dp(this, 52)));
        Ui.add(formCard, actions, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 16);
        manualHint = Ui.card(this);
        manualHint.addView(Ui.text(this, "手工录入", 20, Ui.TEXT, true));
        manualHint.addView(Ui.text(this, "逐项填写右侧商品资料。图片最多9张，第一张作为封面；货号保存后自动生成。", 14, Ui.MUTED, false));

        if (wide) {
            LinearLayout left = Ui.vertical(this);
            left.addView(voiceCard, Ui.matchWrap());
            left.addView(manualHint, Ui.matchWrap());
            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            leftParams.setMarginEnd(Ui.dp(this, 14));
            columns.addView(left, leftParams);
            columns.addView(formCard, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        } else {
            columns.addView(voiceCard, Ui.matchWrap());
            columns.addView(manualHint, Ui.matchWrap());
            Ui.add(columns, formCard, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        }
        scroll.addView(columns, Ui.matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        setMode(true);
    }

    private LinearLayout buildVoiceCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "语音录入", 20, Ui.TEXT, true));
        card.addView(Ui.text(this, "说出名称、进货价、销售价和库存，识别后自动填写右侧表单。", 13, Ui.MUTED, false));
        micButton = Ui.button(this, "开始说话", true);
        micButton.setOnClickListener(v -> {
            if (listening) voiceController.stop(); else voiceController.start();
        });
        Ui.add(card, micButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54), 14);
        commandInput = Ui.input(this, EXAMPLE, true);
        Ui.add(card, commandInput, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 120), 12);
        Button example = Ui.button(this, "填入示例", false);
        example.setOnClickListener(v -> commandInput.setText(EXAMPLE));
        Ui.add(card, example, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 44), 8);
        parseButton = Ui.button(this, "智能识别并填写", true);
        parseButton.setOnClickListener(v -> parseCommand());
        Ui.add(card, parseButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48), 8);
        String parser = new AppSettings(this).hasDeepSeekApiKey() ? "DeepSeek 大模型" : "本地解析";
        card.addView(Ui.text(this, "当前：" + parser + " · 取消不会保存", 12, Ui.MUTED, false));
        return card;
    }

    private void setMode(boolean voice) {
        voiceSelected = voice;
        voiceMode.setBackground(Ui.roundRect(voice ? Ui.PRIMARY : Ui.SURFACE, 11,
                voice ? Ui.PRIMARY : Ui.BORDER, this));
        voiceMode.setTextColor(voice ? Ui.SURFACE : Ui.PRIMARY_DARK);
        manualMode.setBackground(Ui.roundRect(!voice ? Ui.PRIMARY : Ui.SURFACE, 11,
                !voice ? Ui.PRIMARY : Ui.BORDER, this));
        manualMode.setTextColor(!voice ? Ui.SURFACE : Ui.PRIMARY_DARK);
        voiceCard.setVisibility(voice ? View.VISIBLE : View.GONE);
        manualHint.setVisibility(voice ? View.GONE : View.VISIBLE);
    }

    private void parseCommand() {
        if (parsing) return;
        String command = commandInput.getText().toString().trim();
        if (command.isEmpty()) {
            commandInput.setError("请先输入商品信息");
            return;
        }
        String apiKey = new AppSettings(this).getDeepSeekApiKey();
        parsing = true;
        parseButton.setText("正在识别…");
        executor.execute(() -> {
            ProductDraft draft = null;
            Exception deepSeekError = null;
            if (!apiKey.isEmpty()) {
                try {
                    draft = DeepSeekParser.parse(command, apiKey);
                } catch (Exception e) {
                    deepSeekError = e;
                }
            }
            String warning = null;
            if (draft == null) {
                try {
                    draft = LocalCommandParser.parse(command);
                    if (deepSeekError != null) warning = "DeepSeek 不可用，已使用本地解析";
                } catch (Exception e) {
                    String message = deepSeekError == null ? e.getMessage() : deepSeekError.getMessage() + "\n" + e.getMessage();
                    runOnUiThread(() -> parseFailed(message));
                    return;
                }
            }
            ProductDraft result = draft;
            String finalWarning = warning;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                parsing = false;
                parseButton.setText("智能识别并填写");
                form.applyDraft(result);
                if (finalWarning != null) Toast.makeText(this, finalWarning, Toast.LENGTH_LONG).show();
                Toast.makeText(this, "已填写商品信息，请核对后确认入库", Toast.LENGTH_LONG).show();
            });
        });
    }

    private void parseFailed(String message) {
        if (isDestroyed()) return;
        parsing = false;
        parseButton.setText("智能识别并填写");
        new AlertDialog.Builder(this).setTitle("暂时无法解析").setMessage(message)
                .setPositiveButton("修改指令", null).show();
    }

    private void saveProduct() {
        try {
            ProductDraft input = form.read();
            Product created = InventoryDatabase.getInstance(this).createProduct(input.name,
                    input.purchasePriceCents, input.salePriceCents, input.stock, input.season,
                    input.style, input.category, input.photoPath, input.photoPaths);
            Toast.makeText(this, created.name + " 已入库，货号 " + created.code, Toast.LENGTH_LONG).show();
            finish();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
        form.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        voiceController.destroy();
        executor.shutdownNow();
        super.onDestroy();
    }
}
