package com.workbuddy.jxc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsPanel {
    public static final int EXPORT_EXCEL = 6101;
    public static final int EXPORT_JSON = 6102;
    public static final int IMPORT_JSON = 6103;
    private final Activity activity;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LinearLayout view;
    private final EditText keyInput;
    private Button excelButton;
    private Button jsonButton;
    private Button importButton;
    private LinearLayout status;
    private TextView statusText;

    public SettingsPanel(Activity activity) {
        this.activity = activity;
        AppSettings settings = new AppSettings(activity);
        view = Ui.vertical(activity);
        boolean wide = activity.getResources().getConfiguration().screenWidthDp >= 700;
        LinearLayout columns = wide ? Ui.horizontal(activity) : Ui.vertical(activity);
        LinearLayout ai = Ui.card(activity);
        ai.addView(Ui.text(activity, "DeepSeek 配置", 20, Ui.TEXT, true));
        ai.addView(Ui.text(activity, "用于语音和自然语言商品建档；不配置时使用本地解析。", 13, Ui.MUTED, false));
        keyInput = Ui.input(activity, "请输入 sk- 开头的 API Key", false);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setText(settings.getDeepSeekApiKey());
        Ui.add(ai, keyInput, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        LinearLayout keyActions = Ui.horizontal(activity);
        Button save = Ui.button(activity, "保存配置", true);
        save.setOnClickListener(v -> saveKey());
        LinearLayout.LayoutParams saveParams = Ui.weightParams(Ui.dp(activity, 46));
        saveParams.setMarginEnd(Ui.dp(activity, 8));
        keyActions.addView(save, saveParams);
        Button clear = Ui.button(activity, "清空 Key", false);
        clear.setOnClickListener(v -> {
            keyInput.setText("");
            settings.setDeepSeekApiKey("");
            Toast.makeText(activity, "API Key 已清除", Toast.LENGTH_SHORT).show();
        });
        keyActions.addView(clear, Ui.weightParams(Ui.dp(activity, 46)));
        Ui.add(ai, keyActions, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 10);
        ai.addView(Ui.text(activity, "API Key 仅保存在本机应用私有空间。", 12, Ui.MUTED, false));
        addColumn(columns, ai, wide);

        LinearLayout backup = Ui.card(activity);
        backup.addView(Ui.text(activity, "数据备份与恢复", 20, Ui.TEXT, true));
        backup.addView(Ui.text(activity, "JSON 用于完整备份和恢复；Excel 用于查看与分析。", 13, Ui.MUTED, false));
        jsonButton = Ui.button(activity, "导出 JSON 完整备份", true);
        jsonButton.setOnClickListener(v -> chooseExport(false));
        Ui.add(backup, jsonButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 48), 14);
        excelButton = Ui.button(activity, "导出 Excel（.xlsx）", false);
        excelButton.setOnClickListener(v -> chooseExport(true));
        Ui.add(backup, excelButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 48), 10);
        importButton = Ui.button(activity, "导入 JSON 并恢复数据", false);
        importButton.setOnClickListener(v -> chooseImport());
        Ui.add(backup, importButton, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 48), 10);
        backup.addView(Ui.text(activity, "导入会替换当前业务数据。图片文件不嵌入 JSON，仅恢复本机仍存在的匹配图片。", 12, Ui.MUTED, false));
        status = Ui.vertical(activity);
        status.setVisibility(View.GONE);
        statusText = Ui.text(activity, "", 13, Ui.MUTED, false);
        status.addView(statusText);
        Ui.add(backup, status, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 8);
        addColumn(columns, backup, wide);
        view.addView(columns, Ui.matchWrap());
    }

    public LinearLayout getView() {
        return view;
    }

    private void addColumn(LinearLayout row, View card, boolean wide) {
        if (wide) {
            LinearLayout.LayoutParams params = Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT);
            if (row.getChildCount() > 0) params.setMarginStart(Ui.dp(activity, 12));
            row.addView(card, params);
        } else {
            Ui.add(row, card, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, row.getChildCount() == 0 ? 0 : 12);
        }
    }

    private void saveKey() {
        String key = keyInput.getText().toString().trim();
        new AppSettings(activity).setDeepSeekApiKey(key);
        Toast.makeText(activity, key.isEmpty() ? "已切换为本地解析" : "DeepSeek 配置已保存", Toast.LENGTH_SHORT).show();
    }

    private void chooseExport(boolean excel) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(excel ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "jxc_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date()) + (excel ? ".xlsx" : ".json"));
        activity.startActivityForResult(intent, excel ? EXPORT_EXCEL : EXPORT_JSON);
    }

    private void chooseImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        activity.startActivityForResult(intent, IMPORT_JSON);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode != EXPORT_EXCEL && requestCode != EXPORT_JSON && requestCode != IMPORT_JSON) ||
                resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return false;
        Uri uri = data.getData();
        if (requestCode == IMPORT_JSON) {
            confirmImport(uri);
            return true;
        }
        setBusy(true, "正在生成导出文件…");
        executor.execute(() -> {
            try {
                if (requestCode == EXPORT_EXCEL) DataExporter.writeExcel(activity, uri);
                else DataExporter.writeJson(activity, uri);
                complete("导出成功", null);
            } catch (Exception e) {
                complete(null, "导出失败：" + e.getMessage());
            }
        });
        return true;
    }

    private void confirmImport(Uri source) {
        new AlertDialog.Builder(activity).setTitle("用备份替换当前数据？")
                .setMessage("导入会替换当前全部商品、进货、销售和库存流水，且无法撤销。建议先导出 JSON 备份。\n\n图片二进制不包含在 JSON 中，仅恢复本机仍存在的匹配图片。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认替换", (dialog, which) -> {
                    setBusy(true, "正在校验并恢复备份…");
                    executor.execute(() -> {
                        try {
                            DataImporter.importJson(activity, source);
                            complete("备份恢复成功", null);
                        } catch (Exception e) {
                            complete(null, "恢复失败，现有数据未改变：" + e.getMessage());
                        }
                    });
                }).show();
    }

    private void complete(String success, String error) {
        activity.runOnUiThread(() -> {
            if (activity.isDestroyed()) return;
            setBusy(false, "");
            Toast.makeText(activity, error == null ? success : error, Toast.LENGTH_LONG).show();
        });
    }

    private void setBusy(boolean busy, String message) {
        excelButton.setEnabled(!busy);
        jsonButton.setEnabled(!busy);
        importButton.setEnabled(!busy);
        status.setVisibility(busy ? View.VISIBLE : View.GONE);
        statusText.setText(message);
    }

    public void destroy() {
        executor.shutdownNow();
    }
}
