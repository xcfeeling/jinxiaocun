package com.workbuddy.jxc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalysisActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Button[] windowButtons = new Button[3];
    private final int[] windows = {30, 90, 180};
    private LinearLayout content;
    private Button shareButton;
    private int windowDays = 90;
    private boolean loading;
    private AnalysisSnapshot snapshot;
    private AnalysisReport report;
    private String analysisError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        loadAnalysis();
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
        title.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        title.addView(Ui.text(this, "近期经营分析", 27, Ui.TEXT, true));
        title.addView(Ui.text(this, "真实数据聚合 + DeepSeek 决策辅助", 13, Ui.MUTED, false));
        header.addView(title, Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        shareButton = Ui.button(this, "分享报告", false);
        shareButton.setEnabled(false);
        shareButton.setOnClickListener(v -> shareReport());
        header.addView(shareButton);
        root.addView(header);

        LinearLayout windowRow = Ui.horizontal(this);
        for (int i = 0; i < windows.length; i++) {
            int days = windows[i];
            windowButtons[i] = Ui.button(this, "近" + days + "天", days == windowDays);
            windowButtons[i].setOnClickListener(v -> {
                if (!loading && windowDays != days) {
                    windowDays = days;
                    loadAnalysis();
                }
            });
            LinearLayout.LayoutParams params = Ui.weightParams(Ui.dp(this, 44));
            params.setMarginEnd(i == 2 ? 0 : Ui.dp(this, 8));
            windowRow.addView(windowButtons[i], params);
        }
        Ui.add(root, windowRow, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 14);
        ScrollView scroll = new ScrollView(this);
        content = Ui.vertical(this);
        scroll.addView(content, Ui.matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private void loadAnalysis() {
        loading = true;
        snapshot = null;
        report = null;
        analysisError = null;
        shareButton.setEnabled(false);
        for (Button button : windowButtons) button.setEnabled(false);
        content.removeAllViews();
        content.addView(Ui.text(this, "正在汇总全部商品与业务记录，并生成近期分析…", 16, Ui.MUTED, false));
        String apiKey = new AppSettings(this).getDeepSeekApiKey();
        executor.execute(() -> {
            AnalysisSnapshot generated = AnalysisReportGenerator.generate(
                    InventoryDatabase.getInstance(this), windowDays);
            AnalysisReport aiReport = null;
            String error = null;
            try {
                if (apiKey.isEmpty()) throw new IllegalArgumentException("请先在设置中配置 DeepSeek API Key");
                aiReport = DeepSeekAnalyzer.analyze(generated, apiKey);
            } catch (Exception e) {
                error = e.getMessage();
            }
            AnalysisSnapshot finalSnapshot = generated;
            AnalysisReport finalReport = aiReport;
            String finalError = error;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                snapshot = finalSnapshot;
                report = finalReport;
                analysisError = finalError;
                loading = false;
                for (Button button : windowButtons) button.setEnabled(true);
                shareButton.setEnabled(true);
                render();
            });
        });
    }

    private void render() {
        content.removeAllViews();
        updateWindowStyles();
        if (snapshot.products.isEmpty()) {
            content.addView(emptyCard("还没有商品数据", "请先新增商品并记录进货、销售，再生成经营分析。"));
            return;
        }
        LinearLayout summary = section("近" + windowDays + "天经营概况",
                "生成时间 " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                        .format(new Date(snapshot.generatedAt)));
        summary.addView(Ui.text(this, "销售额 " + Formatters.money(snapshot.periodStats.salesCents)
                + " · 毛利 " + Formatters.money(snapshot.periodStats.grossProfitCents), 20, Ui.TEXT, true));
        summary.addView(Ui.text(this, "销量 " + snapshot.periodStats.salesQuantity + " 件 · 销售 "
                + snapshot.periodStats.salesCount + " 笔 · 当前库存 " + snapshot.inventorySummary.totalStock
                + " 件 · 库存成本 " + Formatters.money(snapshot.inventorySummary.totalCostCents), 13, Ui.MUTED, false));
        addCard(summary);

        if (report != null) {
            LinearLayout ai = section("DeepSeek 经营判断", "AI 解读基于上方聚合数据，建议仍需结合实际门店情况");
            ai.addView(Ui.text(this, report.summary, 15, Ui.TEXT, false));
            if (!report.marketNotes.isEmpty()) {
                ai.addView(Ui.space(this, 8));
                ai.addView(Ui.text(this, "季节与市场参考：" + report.marketNotes, 13, Ui.MUTED, false));
            }
            addCard(ai);
            if (!report.categoryInsights.isEmpty()) addCard(stringCard("分类机会", report.categoryInsights));
            if (!report.risks.isEmpty()) addCard(stringCard("风险提示", report.risks));
        } else {
            LinearLayout error = section("AI 分析未完成", "以下展示本地确定性分析，不冒充 DeepSeek 结果");
            error.addView(Ui.text(this, analysisError == null ? "请稍后重试" : analysisError, 13, Ui.PRIMARY_DARK, false));
            Button retry = Ui.button(this, "重试 DeepSeek 分析", true);
            retry.setOnClickListener(v -> loadAnalysis());
            Ui.add(error, retry, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46), 10);
            addCard(error);
        }

        List<ProductPerformance> fast = new ArrayList<>();
        List<ProductPerformance> slow = new ArrayList<>();
        List<ProductPerformance> localPurchases = new ArrayList<>();
        for (ProductPerformance item : snapshot.products) {
            if (item.fastMoving) fast.add(item);
            if (item.slowMoving) slow.add(item);
            if (item.suggestedQuantity > 0) localPurchases.add(item);
        }
        fast.sort((a, b) -> Integer.compare(b.salesQuantity, a.salesQuantity));
        slow.sort((a, b) -> Integer.compare(b.daysWithoutSales, a.daysWithoutSales));
        localPurchases.sort((a, b) -> Integer.compare(b.suggestedQuantity, a.suggestedQuantity));
        LinearLayout fastCard = section("快销商品", "依据近期销量，结合库存可售天数");
        if (fast.isEmpty()) fastCard.addView(Ui.text(this, "当前窗口没有达到快销阈值的商品", 13, Ui.MUTED, false));
        for (int i = 0; i < Math.min(5, fast.size()); i++) {
            ProductPerformance item = fast.get(i);
            fastCard.addView(performanceRow(item, aiReason(report == null ? null : report.fastMoving, item.product.code),
                    "近" + windowDays + "天售 " + item.salesQuantity + " 件 · 可售 " + item.coverageLabel()));
        }
        addCard(fastCard);
        LinearLayout slowCard = section("滞销商品", "当前有库存且超过30天未销售");
        if (slow.isEmpty()) slowCard.addView(Ui.text(this, "暂无符合滞销条件的商品", 13, Ui.MUTED, false));
        for (int i = 0; i < Math.min(5, slow.size()); i++) {
            ProductPerformance item = slow.get(i);
            slowCard.addView(performanceRow(item, aiReason(report == null ? null : report.slowMoving, item.product.code),
                    item.daysWithoutSales + " 天未售 · 库存 " + item.product.stock + " 件"));
        }
        addCard(slowCard);

        LinearLayout purchaseCard = section("建议补货", report != null ? "DeepSeek 建议 · 已校验商品编码和数量" :
                "本地规则建议 · 近期需求扣除当前库存并考虑季节修正");
        Map<String, ProductPerformance> byCode = new HashMap<>();
        for (ProductPerformance item : snapshot.products) byCode.put(item.product.code, item);
        int shown = 0;
        if (report != null) {
            for (AnalysisItem suggestion : report.purchases) {
                ProductPerformance item = byCode.get(suggestion.code);
                if (item == null || suggestion.quantity <= 0) continue;
                purchaseCard.addView(purchaseRow(item, suggestion.quantity, suggestion.reason));
                if (++shown >= 10) break;
            }
        } else {
            for (ProductPerformance item : localPurchases) {
                purchaseCard.addView(purchaseRow(item, item.suggestedQuantity,
                        "按目标库存天数与近期日均销量计算，季节修正已计入"));
                if (++shown >= 10) break;
            }
        }
        if (shown == 0) purchaseCard.addView(Ui.text(this, "当前没有建议补货的商品。慢销、无销量或库存充足的商品不盲目补货。",
                13, Ui.MUTED, false));
        addCard(purchaseCard);

        LinearLayout method = section("分析口径", "数据事实与市场判断分开");
        method.addView(Ui.text(this, "快销阈值随分析窗口调整；滞销为当前有库存且至少30天未售。可售天数 = 当前库存 ÷ 近期日均销量。基础补货量以30/45天目标库存为参考，快销目标30天，其他有销量商品目标45天，当季商品系数1.15、非当季0.85。DeepSeek 的市场说明仅作一般经验参考，不代表实时市场数据。",
                12, Ui.MUTED, false));
        addCard(method);
    }

    private View performanceRow(ProductPerformance item, String reason, String metrics) {
        LinearLayout row = Ui.vertical(this);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        row.addView(Ui.text(this, item.product.name + "  " + item.product.code, 15, Ui.TEXT, true));
        row.addView(Ui.text(this, metrics, 12, Ui.PRIMARY_DARK, false));
        if (reason != null && !reason.isEmpty()) row.addView(Ui.text(this, reason, 12, Ui.MUTED, false));
        return row;
    }

    private View purchaseRow(ProductPerformance item, int quantity, String reason) {
        LinearLayout row = Ui.vertical(this);
        row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        row.addView(Ui.text(this, item.product.name + "  " + item.product.code, 16, Ui.TEXT, true));
        row.addView(Ui.text(this, "近期销量 " + item.salesQuantity + " 件 · 当前库存 " + item.product.stock
                + " 件 · 建议补 " + quantity + " 件", 14, Ui.PRIMARY_DARK, true));
        row.addView(Ui.text(this, reason, 12, Ui.MUTED, false));
        Button purchase = Ui.button(this, "带建议数量去进货", false);
        purchase.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecordActivity.class);
            intent.putExtra(RecordActivity.EXTRA_IS_PURCHASE, true);
            intent.putExtra(RecordActivity.EXTRA_PRODUCT_ID, item.product.id);
            intent.putExtra(RecordActivity.EXTRA_QUANTITY, quantity);
            intent.putExtra(RecordActivity.EXTRA_UNIT_PRICE, item.product.purchasePriceCents);
            startActivity(intent);
        });
        Ui.add(row, purchase, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 40), 6);
        return row;
    }

    private String aiReason(List<AnalysisItem> items, String code) {
        if (items == null) return "";
        for (AnalysisItem item : items) if (item.code.equals(code)) return item.reason;
        return "";
    }

    private LinearLayout stringCard(String title, List<String> lines) {
        LinearLayout card = section(title, "");
        for (String line : lines) card.addView(Ui.text(this, "· " + line, 13, Ui.MUTED, false));
        return card;
    }

    private LinearLayout emptyCard(String title, String message) {
        LinearLayout card = section(title, "");
        card.addView(Ui.text(this, message, 14, Ui.MUTED, false));
        return card;
    }

    private LinearLayout section(String title, String subtitle) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, title, 19, Ui.TEXT, true));
        if (!subtitle.isEmpty()) card.addView(Ui.text(this, subtitle, 12, Ui.MUTED, false));
        card.addView(Ui.space(this, 8));
        return card;
    }

    private void addCard(View card) {
        Ui.add(content, card, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
    }

    private void updateWindowStyles() {
        for (int i = 0; i < windows.length; i++) {
            boolean selected = windows[i] == windowDays;
            windowButtons[i].setBackground(Ui.roundRect(selected ? Ui.PRIMARY : Ui.SURFACE, 11,
                    selected ? Ui.PRIMARY : Ui.BORDER, this));
            windowButtons[i].setTextColor(selected ? Ui.SURFACE : Ui.PRIMARY_DARK);
        }
    }

    private void shareReport() {
        if (snapshot == null) return;
        StringBuilder text = new StringBuilder("近" + windowDays + "天经营分析\n");
        text.append("销售额：").append(Formatters.money(snapshot.periodStats.salesCents))
                .append("\n毛利：").append(Formatters.money(snapshot.periodStats.grossProfitCents))
                .append("\n销量：").append(snapshot.periodStats.salesQuantity).append("件")
                .append("\n当前库存：").append(snapshot.inventorySummary.totalStock).append("件\n");
        if (report != null) {
            text.append("\nDeepSeek 总结：\n").append(report.summary);
            if (!report.purchases.isEmpty()) {
                text.append("\n\n采购建议：");
                for (AnalysisItem item : report.purchases) {
                    text.append("\n").append(item.name).append(" ").append(item.code).append("：")
                            .append(item.quantity).append("件，").append(item.reason);
                }
            }
        } else {
            text.append("\n本地分析（AI未完成）");
            for (ProductPerformance item : snapshot.products) {
                if (item.suggestedQuantity > 0) {
                    text.append("\n").append(item.product.name).append("：建议补")
                            .append(item.suggestedQuantity).append("件");
                }
            }
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text.toString());
        startActivity(Intent.createChooser(intent, "分享经营分析"));
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
