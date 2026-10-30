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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceAddActivity extends Activity implements VoiceInputController.Listener {
    private static final String EXAMPLE = "帮我添加一个商品，货号自增长，裙子，进货价60，销售价70，库存20";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private VoiceInputController voiceController;
    private EditText commandInput;
    private Button voiceButton;
    private Button parseButton;
    private ProgressBar progressBar;
    private LinearLayout progressRow;
    private ProductFormView previewForm;
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
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 18));
        LinearLayout header = Ui.horizontal(this);
        Button back = Ui.button(this, "返回", false);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        header.addView(Ui.text(this, "  语音新增商品", 24, Ui.TEXT, true));
        root.addView(header);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = Ui.vertical(this);
        scrollView.addView(content, Ui.matchWrap());
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout voiceCard = Ui.card(this);
        voiceCard.addView(Ui.text(this, "说出商品信息", 20, Ui.TEXT, true));
        voiceCard.addView(Ui.space(this, 6));
        voiceCard.addView(Ui.text(this, "请包含商品名称、进货价、销售价和库存。识别后自动解析，确认页可继续补充季节、风格和照片。",
                14, Ui.MUTED, false));
        voiceButton = Ui.button(this, "开始说话", true);
        voiceButton.setOnClickListener(v -> {
            if (listening) voiceController.stop(); else voiceController.start();
        });
        Ui.add(voiceCard, voiceButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54), 16);
        String parserName = new AppSettings(this).hasDeepSeekApiKey() ? "DeepSeek 大模型" : "内置本地解析";
        TextView parserStatus = Ui.text(this, "当前解析方式：" + parserName, 13, Ui.MUTED, false);
        parserStatus.setGravity(android.view.Gravity.CENTER);
        Ui.add(voiceCard, parserStatus, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 8);
        Ui.add(content, voiceCard, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 18);

        LinearLayout commandCard = Ui.card(this);
        LinearLayout commandHeader = Ui.horizontal(this);
        commandHeader.addView(Ui.text(this, "识别内容 / 文字输入", 17, Ui.TEXT, true),
                Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        Button example = Ui.button(this, "填入示例", false);
        example.setOnClickListener(v -> commandInput.setText(EXAMPLE));
        commandHeader.addView(example);
        commandCard.addView(commandHeader);
        commandInput = Ui.input(this, EXAMPLE, true);
        Ui.add(commandCard, commandInput, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 120), 12);
        parseButton = Ui.button(this, "解析并预览", true);
        parseButton.setOnClickListener(v -> parseCommand());
        Ui.add(commandCard, parseButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50), 14);
        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressRow = Ui.horizontal(this);
        progressRow.setGravity(android.view.Gravity.CENTER);
        progressRow.addView(progressBar, new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32)));
        progressRow.addView(Ui.text(this, "  正在理解商品信息…", 14, Ui.MUTED, false));
        progressRow.setVisibility(View.GONE);
        Ui.add(commandCard, progressRow, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 10);
        Ui.add(content, commandCard, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 14);
        TextView safety = Ui.text(this, "只有点击预览弹框中的“确认入库”才会保存；取消不会修改库存。",
                13, Ui.MUTED, false);
        safety.setGravity(android.view.Gravity.CENTER);
        Ui.add(content, safety, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        setContentView(root);
    }

    private void parseCommand() {
        if (parsing) return;
        String command = commandInput.getText().toString().trim();
        if (command.isEmpty()) {
            commandInput.setError("请先说出或输入商品信息");
            return;
        }
        String apiKey = new AppSettings(this).getDeepSeekApiKey();
        setParsing(true);
        executor.execute(() -> {
            ProductDraft draft = null;
            String source = "";
            String warning = null;
            Exception deepSeekError = null;
            if (!apiKey.isEmpty()) {
                try {
                    draft = DeepSeekParser.parse(command, apiKey);
                    source = "DeepSeek 大模型";
                } catch (Exception e) {
                    deepSeekError = e;
                }
            }
            if (draft == null) {
                try {
                    draft = LocalCommandParser.parse(command);
                    source = "本地解析";
                    if (deepSeekError != null) warning = "DeepSeek 暂不可用，已改用本地解析";
                } catch (Exception localError) {
                    String message = deepSeekError == null ? localError.getMessage() :
                            deepSeekError.getMessage() + "\n本地解析：" + localError.getMessage();
                    runOnUiThread(() -> {
                        if (!isDestroyed()) {
                            setParsing(false);
                            showError(message);
                        }
                    });
                    return;
                }
            }
            ProductDraft finalDraft = draft;
            String finalSource = source;
            String finalWarning = warning;
            runOnUiThread(() -> {
                if (!isDestroyed()) {
                    setParsing(false);
                    if (finalWarning != null) Toast.makeText(this, finalWarning, Toast.LENGTH_LONG).show();
                    showPreview(finalDraft, finalSource);
                }
            });
        });
    }

    private void setParsing(boolean value) {
        parsing = value;
        parseButton.setEnabled(!value);
        voiceButton.setEnabled(!value);
        parseButton.setText(value ? "正在解析…" : "解析并预览");
        progressRow.setVisibility(value ? View.VISIBLE : View.GONE);
    }

    private void showPreview(ProductDraft draft, String source) {
        LinearLayout content = Ui.vertical(this);
        content.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 22), Ui.dp(this, 8));
        content.addView(Ui.text(this, "解析方式：" + source + "  ·  货号确认后自动生成", 13, Ui.MUTED, false));
        previewForm = new ProductFormView(this, null, draft, true);
        content.addView(previewForm.getView(), Ui.matchWrap());
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(content, Ui.matchWrap());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("确认商品信息")
                .setView(scrollView).setPositiveButton("确认入库", null)
                .setNegativeButton("取消", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        ProductDraft input = previewForm.read();
                        Product created = InventoryDatabase.getInstance(this).createProduct(input.name,
                                input.purchasePriceCents, input.salePriceCents, input.stock,
                                input.season, input.style, input.photoPath);
                        dialog.dismiss();
                        Toast.makeText(this, created.name + " 已入库，货号 " + created.code,
                                Toast.LENGTH_LONG).show();
                        finish();
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }));
        dialog.show();
    }

    private void showError(String message) {
        new AlertDialog.Builder(this).setTitle("暂时无法解析").setMessage(message)
                .setPositiveButton("修改指令", null).show();
    }

    @Override
    public void onListeningChanged(boolean value) {
        listening = value;
        voiceButton.setText(value ? "说完后点击停止" : "开始说话");
    }

    @Override
    public void onPartialText(String text) {
        commandInput.setText(text);
    }

    @Override
    public void onResult(String text) {
        commandInput.setText(text);
        parseCommand();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        voiceController.onPermissionResult(requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (previewForm != null) previewForm.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        voiceController.destroy();
        executor.shutdownNow();
        super.onDestroy();
    }
}
