package com.workbuddy.jxc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private InventoryDatabase database;
    private FrameLayout pageContainer;
    private final Button[] navigation = new Button[5];
    private boolean wide;
    private int currentTab;
    private int periodMode = DateRanges.DAY;
    private long periodAnchor = System.currentTimeMillis();
    private SettingsPanel settingsPanel;
    private long selectedProductId = -1;
    private int inventoryFilter;
    private int inventorySort;
    private String inventorySearch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.APPLICATION_ID.endsWith(".demo")) {
            DemoDataSeeder.seedIfNeeded(this);
        }
        database = InventoryDatabase.getInstance(this);
        wide = getResources().getConfiguration().screenWidthDp >= 800;
        LinearLayout root = Ui.vertical(this);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(Ui.BACKGROUND);
        if (wide) {
            LinearLayout body = Ui.horizontal(this);
            body.setGravity(android.view.Gravity.TOP);
            body.addView(buildSidebar(), new LinearLayout.LayoutParams(Ui.dp(this, 196),
                    ViewGroup.LayoutParams.MATCH_PARENT));
            pageContainer = new FrameLayout(this);
            body.addView(pageContainer, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
            root.addView(body);
        } else {
            LinearLayout brand = Ui.horizontal(this);
            brand.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 4));
            LinearLayout brandText = Ui.vertical(this);
            brandText.addView(Ui.text(this, "女装批发进销存 3.3", 22, Ui.TEXT, true));
            brandText.addView(Ui.text(this, "让服装生意更简单", 12, Ui.MUTED, false));
            brand.addView(brandText, Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
            Button voice = Ui.button(this, "语音助手", true);
            voice.setOnClickListener(v -> startActivity(new Intent(this, VoiceBusinessActivity.class)));
            brand.addView(voice);
            root.addView(brand);
            pageContainer = new FrameLayout(this);
            root.addView(pageContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
            root.addView(buildBottomNavigation());
        }
        setContentView(root);
    }

    private View buildSidebar() {
        LinearLayout sidebar = Ui.vertical(this);
        sidebar.setPadding(Ui.dp(this, 14), Ui.dp(this, 24), Ui.dp(this, 14), Ui.dp(this, 18));
        sidebar.setBackground(Ui.gradient(0xFFFFF4F5, 0xFFFFF9F4, 0, this));
        TextView brand = Ui.text(this, "女装批发进销存 3.3", 18, Ui.TEXT, true);
        brand.setPadding(Ui.dp(this, 10), 0, 0, 0);
        sidebar.addView(brand);
        TextView slogan = Ui.text(this, "让服装生意更简单", 12, Ui.MUTED, false);
        slogan.setPadding(Ui.dp(this, 10), 0, 0, Ui.dp(this, 28));
        sidebar.addView(slogan);
        String[] labels = {"概览", "进货", "销售", "库存", "设置"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            navigation[i] = Ui.button(this, labels[i], i == 0);
            navigation[i].setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            navigation[i].setOnClickListener(v -> selectTab(index));
            Ui.add(sidebar, navigation[i], ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52), 8);
        }
        Button voice = Ui.button(this, "语音助手", true);
        voice.setOnClickListener(v -> startActivity(new Intent(this, VoiceBusinessActivity.class)));
        Ui.add(sidebar, voice, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50), 16);
        TextView footer = Ui.text(this, "好货 · 好生意\n从这里开始", 13, Ui.MUTED, false);
        footer.setGravity(android.view.Gravity.CENTER);
        sidebar.addView(footer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return sidebar;
    }

    private View buildBottomNavigation() {
        LinearLayout bar = Ui.horizontal(this);
        bar.setPadding(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 8));
        bar.setBackgroundColor(Ui.SURFACE);
        String[] labels = {"概览", "进货", "销售", "库存", "设置"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            navigation[i] = Ui.button(this, labels[i], i == 0);
            navigation[i].setTextSize(12);
            navigation[i].setOnClickListener(v -> selectTab(index));
            LinearLayout.LayoutParams params = Ui.weightParams(Ui.dp(this, 46));
            params.setMarginEnd(i == 4 ? 0 : Ui.dp(this, 4));
            bar.addView(navigation[i], params);
        }
        return bar;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTab == 4 && settingsPanel != null) {
            return;
        }
        selectTab(currentTab);
    }

    private void selectTab(int index) {
        currentTab = index;
        if (settingsPanel != null) {
            settingsPanel.destroy();
            settingsPanel = null;
        }
        for (int i = 0; i < navigation.length; i++) {
            boolean selected = i == index;
            navigation[i].setBackground(Ui.roundRect(selected ? Ui.PRIMARY_LIGHT : Color.TRANSPARENT,
                    12, Color.TRANSPARENT, this));
            navigation[i].setTextColor(selected ? Ui.PRIMARY_DARK : Ui.MUTED);
        }
        pageContainer.removeAllViews();
        LinearLayout page = Ui.vertical(this);
        page.setPadding(Ui.dp(this, wide ? 22 : 14), Ui.dp(this, 14),
                Ui.dp(this, wide ? 22 : 14), Ui.dp(this, 20));
        if (index == 1 || index == 2 || index == 3) {
            pageContainer.addView(page, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            ScrollView scroll = new ScrollView(this);
            scroll.setFillViewport(true);
            scroll.addView(page, Ui.matchWrap());
            pageContainer.addView(scroll);
        }
        if (index == 0) showOverview(page);
        else if (index == 1) showRecords(page, true);
        else if (index == 2) showRecords(page, false);
        else if (index == 3) showInventory(page);
        else showSettings(page);
    }

    private void showOverview(LinearLayout page) {
        pageHeader(page, "经营概览", "实时掌握店铺经营数据，助力高效决策", true);
        LinearLayout analysisEntry = Ui.horizontal(this);
        analysisEntry.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        analysisEntry.setBackground(Ui.roundRect(Ui.PRIMARY_LIGHT, 14, Ui.PRIMARY_LIGHT, this));
        analysisEntry.addView(Ui.text(this, "快销、滞销与补货建议", 15, Ui.TEXT, true),
                Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        Button analysisButton = Ui.button(this, "近期分析", true);
        analysisButton.setOnClickListener(v -> openAnalysis());
        analysisEntry.addView(analysisButton);
        Ui.add(page, analysisEntry, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 10);
        DateRanges.Range range = DateRanges.containing(periodMode, periodAnchor);
        DateRanges.Range previousRange = DateRanges.containing(periodMode,
                DateRanges.shift(periodMode, periodAnchor, -1));
        PeriodStats stats = database.getPeriodStats(range.start, range.end);
        PeriodStats previous = database.getPeriodStats(previousRange.start, previousRange.end);
        InventorySummary inventory = database.getSummary();
        LinearLayout metrics = Ui.vertical(this);
        String[] labels = {"销售额", "毛利", "进货额", "销售单数", "低库存商品", "滞销商品"};
        String[] values = {Formatters.money(stats.salesCents), Formatters.money(stats.grossProfitCents),
                Formatters.money(stats.purchaseCents), String.valueOf(stats.salesCount),
                inventory.lowStockCount + " 款", inventory.stagnantProductCount + " 款"};
        String[] notes = {comparison(stats.salesCents, previous.salesCents),
                comparison(stats.grossProfitCents, previous.grossProfitCents),
                comparison(stats.purchaseCents, previous.purchaseCents),
                comparison(stats.salesCount, previous.salesCount), "实时库存", "超过30天无销售"};
        int[] backgrounds = {0xFFFFEDF1, 0xFFFFF5E8, 0xFFEDF5FF, 0xFFF4EFFF, 0xFFFFF3E6, 0xFFFFEDF1};
        addMetricGrid(metrics, labels, values, notes, backgrounds);
        Ui.add(page, metrics, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 14);

        LinearLayout charts = wide ? Ui.horizontal(this) : Ui.vertical(this);
        LinearLayout trend = section("销售趋势", "玫红销售额 · 橙色进货额");
        SalesTrendChartView trendChart = new SalesTrendChartView(this);
        trendChart.setData(database.getTrend(DateRanges.trend(periodMode, periodAnchor)));
        Ui.add(trend, trendChart, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 250), 8);
        addColumn(charts, trend, wide, 2);
        LinearLayout season = section("季节销售分析", "按销售金额占比");
        SeasonDonutView donut = new SeasonDonutView(this);
        donut.setData(database.getCategoryStats(true, range.start, range.end));
        Ui.add(season, donut, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 250), 8);
        addColumn(charts, season, wide, 1);
        Ui.add(page, charts, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);

        LinearLayout ranks = wide ? Ui.horizontal(this) : Ui.vertical(this);
        LinearLayout hot = section("热销商品 Top5", "按本期销售件数排序");
        List<ProductRank> hotItems = database.getHotProducts(range.start, range.end, 5);
        if (hotItems.isEmpty()) hot.addView(emptyText("本期还没有销售记录"));
        int rank = 1;
        for (ProductRank item : hotItems) hot.addView(hotRow(rank++, item));
        addColumn(ranks, hot, wide, 1);
        LinearLayout overstock = section("易压货商品", "结合库存与最近销售时间");
        List<ProductRank> stockItems = database.getOverstockProducts(5);
        if (stockItems.isEmpty()) overstock.addView(emptyText("当前没有库存"));
        for (ProductRank item : stockItems) overstock.addView(overstockRow(item));
        addColumn(ranks, overstock, wide, 1);
        Ui.add(page, ranks, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);

        LinearLayout style = section("风格销售与库存", "销量/销售额取本期，库存/成本取当前");
        List<CategoryStats> styles = database.getCategoryStats(false, range.start, range.end);
        if (styles.isEmpty()) style.addView(emptyText("暂无风格数据"));
        for (CategoryStats item : styles) {
            style.addView(rankRow(item.name, "售 " + item.salesQuantity + "件 / " + Formatters.money(item.salesCents),
                    "存 " + item.stockQuantity + "件 / " + Formatters.money(item.stockCostCents)));
        }
        addCard(page, style);
    }

    private void showRecords(LinearLayout page, boolean purchase) {
        pageHeader(page, purchase ? "进货管理" : "销售", purchase ? "补货记录、供应商信息与采购入库操作" :
                "记录销售订单，快速开单，实时掌握销售动态", true);
        DateRanges.Range range = DateRanges.containing(periodMode, periodAnchor);
        DateRanges.Range previousRange = DateRanges.containing(periodMode,
                DateRanges.shift(periodMode, periodAnchor, -1));
        PeriodStats stats = database.getPeriodStats(range.start, range.end);
        PeriodStats previous = database.getPeriodStats(previousRange.start, previousRange.end);
        LinearLayout metrics = Ui.vertical(this);
        if (purchase) {
            addMetricGrid(metrics, new String[]{"本期进货额", "本期进货单数", "本期进货量"},
                    new String[]{Formatters.money(stats.purchaseCents), String.valueOf(stats.purchaseCount),
                            stats.purchaseQuantity + " 件"},
                    new String[]{comparison(stats.purchaseCents, previous.purchaseCents),
                            comparison(stats.purchaseCount, previous.purchaseCount),
                            comparison(stats.purchaseQuantity, previous.purchaseQuantity)},
                    new int[]{0xFFFFEDF1, 0xFFEDF5FF, 0xFFFFF3E6});
        } else {
            addMetricGrid(metrics, new String[]{"本期销售额", "本期毛利", "本期销售单数"},
                    new String[]{Formatters.money(stats.salesCents), Formatters.money(stats.grossProfitCents),
                            String.valueOf(stats.salesCount)},
                    new String[]{comparison(stats.salesCents, previous.salesCents),
                            comparison(stats.grossProfitCents, previous.grossProfitCents),
                            comparison(stats.salesCount, previous.salesCount)},
                    new int[]{0xFFFFEDF1, 0xFFFFF5E8, 0xFFF4EFFF});
        }
        Ui.add(page, metrics, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 14);

        LinearLayout columns = wide ? Ui.horizontal(this) : Ui.vertical(this);
        columns.setGravity(android.view.Gravity.TOP);
        LinearLayout ledger = section(purchase ? "进货记录" : "销售记录", "当前周期 · 删除前确认并同步库存");
        EditText search = Ui.input(this, purchase ? "搜索商品或供应商" : "搜索商品或客户", false);
        Ui.add(ledger, search, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46), 8);
        LinearLayout list = Ui.vertical(this);
        ScrollView listScroll = new ScrollView(this);
        listScroll.addView(list, Ui.matchWrap());
        ledger.addView(listScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        List<BusinessRecord> records = database.getRecords(purchase, 500);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                fillRecordList(list, records, purchase, range, s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        fillRecordList(list, records, purchase, range, "");
        if (!wide) {
            Button add = Ui.button(this, purchase ? "记进货" : "快速销售开单", true);
            add.setOnClickListener(v -> openRecord(purchase, -1));
            Ui.add(ledger, add, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48), 10);
            columns.addView(ledger, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        } else {
            LinearLayout.LayoutParams ledgerParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 2);
            ledgerParams.setMarginEnd(Ui.dp(this, 12));
            columns.addView(ledger, ledgerParams);
            RecordFormView form = new RecordFormView(this, purchase, -1, () -> selectTab(currentTab));
            ScrollView formScroll = new ScrollView(this);
            formScroll.addView(form.getView(), Ui.matchWrap());
            columns.addView(formScroll, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        LinearLayout.LayoutParams columnsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        columnsParams.topMargin = Ui.dp(this, 12);
        page.addView(columns, columnsParams);
    }

    private void fillRecordList(LinearLayout list, List<BusinessRecord> records, boolean purchase,
                                DateRanges.Range range, String query) {
        list.removeAllViews();
        String keyword = query.trim().toLowerCase(Locale.CHINA);
        int shown = 0;
        for (BusinessRecord record : records) {
            if (record.businessDate < range.start || record.businessDate >= range.end) continue;
            String text = (record.productName + record.productCode + record.partner).toLowerCase(Locale.CHINA);
            if (!keyword.isEmpty() && !text.contains(keyword)) continue;
            list.addView(recordRow(record));
            shown++;
        }
        if (shown == 0) list.addView(emptyText("当前条件下没有记录"));
    }

    private void showInventory(LinearLayout page) {
        pageHeader(page, "库存管理", "实时查看商品库存、滞销与低库存状态", false);
        List<Product> all = database.searchProducts("");
        int low = 0;
        int stagnant = 0;
        int overstock = 0;
        long threshold = System.currentTimeMillis() - 30L * 86400000L;
        for (Product product : all) {
            if (product.stock <= 5) low++;
            if (product.stock >= 50) overstock++;
            if (isStagnant(product, threshold)) stagnant++;
        }
        LinearLayout filters = Ui.horizontal(this);
        String[] labels = {"全部 (" + all.size() + ")", "低库存 (" + low + ")",
                "滞销 (" + stagnant + ")", "压货 (" + overstock + ")"};
        for (int i = 0; i < labels.length; i++) {
            int filter = i;
            Button button = Ui.button(this, labels[i], inventoryFilter == i);
            button.setTextSize(12);
            button.setOnClickListener(v -> {
                inventoryFilter = filter;
                selectedProductId = -1;
                selectTab(currentTab);
            });
            LinearLayout.LayoutParams params = Ui.weightParams(Ui.dp(this, 44));
            params.setMarginEnd(i == 3 ? 0 : Ui.dp(this, 6));
            filters.addView(button, params);
        }
        Ui.add(page, filters, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        LinearLayout tools = Ui.horizontal(this);
        EditText search = Ui.input(this, "搜索商品名称、货号或风格", false);
        search.setText(inventorySearch);
        tools.addView(search, Ui.weightParams(Ui.dp(this, 48)));
        Button searchButton = Ui.button(this, "搜索", true);
        searchButton.setOnClickListener(v -> selectTab(currentTab));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 48));
        searchParams.setMarginStart(Ui.dp(this, 8));
        tools.addView(searchButton, searchParams);
        Spinner sort = new Spinner(this);
        sort.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"默认排序", "库存升序", "库存降序"}));
        sort.setSelection(inventorySort);
        LinearLayout.LayoutParams sortParams = new LinearLayout.LayoutParams(Ui.dp(this, 140), Ui.dp(this, 48));
        sortParams.setMarginStart(Ui.dp(this, 8));
        tools.addView(sort, sortParams);
        Ui.add(page, tools, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 10);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                inventorySearch = s.toString();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        sort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (inventorySort != position) {
                    inventorySort = position;
                    selectTab(currentTab);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        Button add = Ui.button(this, "新增商品", true);
        add.setOnClickListener(v -> startActivity(new Intent(this, ProductAddActivity.class)));
        Ui.add(page, add, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48), 8);

        List<Product> products = new ArrayList<>();
        for (Product product : database.searchProducts(inventorySearch)) {
            if (inventoryFilter == 1 && product.stock > 5) continue;
            if (inventoryFilter == 2 && !isStagnant(product, threshold)) continue;
            if (inventoryFilter == 3 && product.stock < 50) continue;
            products.add(product);
        }
        if (inventorySort == 1) products.sort((a, b) -> Integer.compare(a.stock, b.stock));
        if (inventorySort == 2) products.sort((a, b) -> Integer.compare(b.stock, a.stock));
        if (products.isEmpty()) {
            addCard(page, emptyText("当前筛选条件下没有商品"));
            return;
        }
        if (selectedProductId < 0 || !containsProduct(products, selectedProductId)) {
            selectedProductId = products.get(0).id;
        }
        LinearLayout columns = wide ? Ui.horizontal(this) : Ui.vertical(this);
        columns.setGravity(android.view.Gravity.TOP);
        LinearLayout listCard = section("商品信息", "季节 / 风格 / 当前库存 / 进货价 / 销售价");
        LinearLayout productList = Ui.vertical(this);
        for (Product product : products) productList.addView(inventoryRow(product));
        ScrollView listScroll = new ScrollView(this);
        listScroll.addView(productList, Ui.matchWrap());
        listCard.addView(listScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        if (wide) {
            LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1);
            listParams.setMarginEnd(Ui.dp(this, 12));
            columns.addView(listCard, listParams);
            Product selected = database.getProduct(selectedProductId);
            ScrollView detailScroll = new ScrollView(this);
            detailScroll.addView(productDetail(selected), Ui.matchWrap());
            columns.addView(detailScroll, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
        } else {
            columns.addView(listCard, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        }
        LinearLayout.LayoutParams columnsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        columnsParams.topMargin = Ui.dp(this, 12);
        page.addView(columns, columnsParams);
    }

    private View inventoryRow(Product product) {
        LinearLayout row = Ui.horizontal(this);
        row.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
        row.setBackground(Ui.roundRect(product.id == selectedProductId && wide ? Ui.PRIMARY_LIGHT : Ui.SURFACE,
                12, product.id == selectedProductId && wide ? Ui.PRIMARY : Ui.BORDER, this));
        row.addView(productImage(product.photoPath, 64), new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 72)));
        LinearLayout info = Ui.vertical(this);
        info.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        info.addView(Ui.text(this, product.name, 15, Ui.TEXT, true));
        info.addView(Ui.text(this, product.code + " · " + product.seasonLabel() +
                (product.style.isEmpty() ? "" : " / " + product.style), 11, Ui.MUTED, false));
        info.addView(Ui.text(this, "库存 " + product.stock + " 件", 16,
                product.stock <= 5 ? Ui.PRIMARY_DARK : Ui.TEXT, true));
        info.addView(Ui.text(this, "进 " + Formatters.money(product.purchasePriceCents) +
                " / 售 " + Formatters.money(product.salePriceCents), 11, Ui.MUTED, false));
        row.addView(info, Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOnClickListener(v -> {
            if (wide) {
                selectedProductId = product.id;
                selectTab(currentTab);
            } else openProduct(product.id);
        });
        return row;
    }

    private View productDetail(Product product) {
        LinearLayout detail = Ui.vertical(this);
        LinearLayout card = section(product.name, product.code + " · " + product.seasonLabel()
                + (product.style.isEmpty() ? "" : " / " + product.style)
                + (product.category.isEmpty() ? "" : " / " + product.category));
        LinearLayout top = Ui.horizontal(this);
        top.addView(productImage(product.photoPath, 150), new LinearLayout.LayoutParams(Ui.dp(this, 150), Ui.dp(this, 170)));
        LinearLayout numbers = Ui.vertical(this);
        numbers.setPadding(Ui.dp(this, 16), 0, 0, 0);
        numbers.addView(Ui.text(this, "当前库存", 12, Ui.MUTED, false));
        numbers.addView(Ui.text(this, product.stock + " 件", 28, Ui.PRIMARY_DARK, true));
        numbers.addView(Ui.text(this, "最近进货价  " + Formatters.money(product.purchasePriceCents), 16, Ui.TEXT, true));
        numbers.addView(Ui.text(this, "建议销售价  " + Formatters.money(product.salePriceCents), 16, Ui.TEXT, true));
        top.addView(numbers, Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(top);
        if (product.photoPaths.size() > 1) {
            LinearLayout thumbnails = Ui.horizontal(this);
            for (String path : product.photoPaths) {
                ImageView thumbnail = productImage(path, 54);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 60));
                params.setMarginEnd(Ui.dp(this, 6));
                thumbnails.addView(thumbnail, params);
            }
            android.widget.HorizontalScrollView photoScroll = new android.widget.HorizontalScrollView(this);
            photoScroll.addView(thumbnails, Ui.matchWrap());
            Ui.add(card, photoScroll, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 64), 8);
        }
        LinearLayout actions = Ui.horizontal(this);
        addAction(actions, "记进货", false, () -> openRecord(true, product.id));
        addAction(actions, "记销售", true, () -> openRecord(false, product.id));
        addAction(actions, "编辑商品", false, () -> {
            Intent intent = new Intent(this, ProductEditActivity.class);
            intent.putExtra(ProductEditActivity.EXTRA_PRODUCT_ID, product.id);
            startActivity(intent);
        });
        Ui.add(card, actions, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        detail.addView(card, Ui.matchWrap());
        LinearLayout trend = section("库存趋势", "近7天 · 根据真实库存流水反推");
        StockTrendChartView chart = new StockTrendChartView(this);
        chart.setData(database.getStockTrend(product.id));
        Ui.add(trend, chart, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 200), 8);
        Ui.add(detail, trend, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        LinearLayout movements = section("库存变动记录", "最近8条");
        List<StockMovement> items = database.getMovements(product.id, 8);
        if (items.isEmpty()) movements.addView(emptyText("暂无库存流水"));
        for (StockMovement item : items) {
            String type = item.type.equals("IN") ? "进货入库" : item.type.equals("OUT") ? "销售出库" : "初始库存";
            movements.addView(rankRow(type + "  " + (item.type.equals("OUT") ? "-" : "+") + item.quantity,
                    "变动后 " + item.stockAfter + " 件", Formatters.dateTime(item.createdAt) +
                            (item.note.isEmpty() ? "" : " · " + item.note)));
        }
        Ui.add(detail, movements, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        return detail;
    }

    private void showSettings(LinearLayout page) {
        pageHeader(page, "系统设置", "DeepSeek 配置、数据导入导出与备份恢复", false);
        settingsPanel = new SettingsPanel(this);
        page.addView(settingsPanel.getView(), Ui.matchWrap());
    }

    private void pageHeader(LinearLayout page, String title, String subtitle, boolean period) {
        LinearLayout header = wide ? Ui.horizontal(this) : Ui.vertical(this);
        LinearLayout heading = Ui.vertical(this);
        heading.addView(Ui.text(this, title, 28, Ui.TEXT, true));
        heading.addView(Ui.text(this, subtitle, 13, Ui.MUTED, false));
        if (wide) header.addView(heading, Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        else header.addView(heading, Ui.matchWrap());
        if (period) {
            LinearLayout controls = Ui.horizontal(this);
            Button previous = Ui.button(this, "‹", false);
            previous.setOnClickListener(v -> {
                periodAnchor = DateRanges.shift(periodMode, periodAnchor, -1);
                selectTab(currentTab);
            });
            controls.addView(previous);
            Button date = Ui.button(this, rangeLabel(DateRanges.containing(periodMode, periodAnchor)), false);
            date.setOnClickListener(v -> chooseDate());
            LinearLayout.LayoutParams dateParams = Ui.weightParams(Ui.dp(this, 44));
            dateParams.setMarginStart(Ui.dp(this, 5));
            dateParams.setMarginEnd(Ui.dp(this, 5));
            controls.addView(date, dateParams);
            Button next = Ui.button(this, "›", false);
            next.setOnClickListener(v -> {
                periodAnchor = DateRanges.shift(periodMode, periodAnchor, 1);
                selectTab(currentTab);
            });
            controls.addView(next);
            String[] names = {"日", "月", "年"};
            int[] modes = {DateRanges.DAY, DateRanges.MONTH, DateRanges.YEAR};
            for (int i = 0; i < 3; i++) {
                int mode = modes[i];
                Button button = Ui.button(this, names[i], periodMode == mode);
                button.setOnClickListener(v -> {
                    periodMode = mode;
                    selectTab(currentTab);
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 44));
                params.setMarginStart(Ui.dp(this, 5));
                controls.addView(button, params);
            }
            if (wide) header.addView(controls);
            else Ui.add(header, controls, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 10);
        }
        page.addView(header, Ui.matchWrap());
    }

    private void addMetricGrid(LinearLayout parent, String[] labels, String[] values, String[] notes, int[] colors) {
        int columns = wide && labels.length > 3 ? 3 : Math.min(3, labels.length);
        for (int start = 0; start < labels.length; start += columns) {
            LinearLayout row = Ui.horizontal(this);
            for (int i = start; i < start + columns && i < labels.length; i++) {
                LinearLayout card = Ui.vertical(this);
                card.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
                card.setBackground(Ui.roundRect(colors[i], 15, colors[i], this));
                card.addView(Ui.text(this, labels[i], 13, Ui.MUTED, false));
                card.addView(Ui.text(this, values[i], 23, Ui.TEXT, true));
                card.addView(Ui.text(this, notes[i], 11, notes[i].contains("↑") ? Ui.PRIMARY_DARK : Ui.MUTED, false));
                LinearLayout.LayoutParams params = Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMarginEnd(Ui.dp(this, 8));
                row.addView(card, params);
            }
            if (start + columns >= labels.length && labels.length % columns != 0) {
                row.addView(Ui.space(this, 1), Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            Ui.add(parent, row, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, start == 0 ? 0 : 8);
        }
    }

    private View hotRow(int rank, ProductRank item) {
        LinearLayout row = Ui.horizontal(this);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        row.addView(Ui.pill(this, String.valueOf(rank), rank <= 3 ? Ui.SURFACE : Ui.MUTED,
                rank == 1 ? Ui.ORANGE : rank <= 3 ? Ui.MUTED : Ui.BORDER));
        row.addView(productImage(item.photoPath, 48), new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 54)));
        LinearLayout info = Ui.vertical(this);
        info.setPadding(Ui.dp(this, 9), 0, Ui.dp(this, 9), 0);
        info.addView(Ui.text(this, item.name, 14, Ui.TEXT, true));
        info.addView(Ui.text(this, item.code + " · 当前库存 " + item.stock, 11, Ui.MUTED, false));
        row.addView(info, Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(Ui.text(this, item.quantity + "件\n" + Formatters.money(item.amountCents), 13, Ui.PRIMARY_DARK, true));
        row.setOnClickListener(v -> openProduct(item.productId));
        return row;
    }

    private View overstockRow(ProductRank item) {
        LinearLayout row = Ui.horizontal(this);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        row.addView(productImage(item.photoPath, 48), new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 54)));
        LinearLayout info = Ui.vertical(this);
        info.setPadding(Ui.dp(this, 9), 0, Ui.dp(this, 9), 0);
        info.addView(Ui.text(this, item.name, 14, Ui.TEXT, true));
        info.addView(Ui.text(this, item.code + " · " + item.stockDays + "天", 11,
                item.stockDays >= 30 ? Ui.PRIMARY_DARK : Ui.MUTED, false));
        row.addView(info, Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(Ui.text(this, item.stock + "件", 15, Ui.TEXT, true));
        Button view = Ui.button(this, "查看", false);
        view.setOnClickListener(v -> openProduct(item.productId));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                Ui.dp(this, 40));
        params.setMarginStart(Ui.dp(this, 8));
        row.addView(view, params);
        return row;
    }

    private View recordRow(BusinessRecord record) {
        LinearLayout row = Ui.vertical(this);
        row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        LinearLayout top = Ui.horizontal(this);
        Product product = database.getProduct(record.productId);
        top.addView(productImage(product == null ? "" : product.photoPath, 48),
                new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 54)));
        LinearLayout info = Ui.vertical(this);
        info.setPadding(Ui.dp(this, 9), 0, Ui.dp(this, 9), 0);
        info.addView(Ui.text(this, record.productName, 14, Ui.TEXT, true));
        info.addView(Ui.text(this, date(record.businessDate) + " · " + record.partner, 11, Ui.MUTED, false));
        info.addView(Ui.text(this, record.quantity + "件 × " + Formatters.money(record.unitPriceCents),
                12, Ui.MUTED, false));
        top.addView(info, Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        top.addView(Ui.text(this, Formatters.money(record.totalCents()) +
                (record.purchase ? "" : "\n毛利 " + Formatters.money(record.profitCents())), 13, Ui.PRIMARY_DARK, true));
        row.addView(top);
        Button delete = Ui.button(this, "删除", false);
        delete.setTextSize(11);
        delete.setMinHeight(0);
        delete.setMinimumHeight(0);
        delete.setPadding(0, 0, 0, 0);
        delete.setOnClickListener(v -> confirmDelete(record));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 30));
        deleteParams.gravity = android.view.Gravity.END;
        deleteParams.topMargin = Ui.dp(this, 4);
        row.addView(delete, deleteParams);
        return row;
    }

    private void confirmDelete(BusinessRecord record) {
        new AlertDialog.Builder(this).setTitle("删除" + (record.purchase ? "进货" : "销售") + "记录？")
                .setMessage(record.productName + " · " + record.quantity + "件\n" +
                        (record.purchase ? "将扣回库存；导致负库存时会拒绝删除。" : "将把销售数量补回库存。"))
                .setNegativeButton("取消", null).setPositiveButton("确认删除", (d, w) -> {
                    try {
                        if (record.purchase) database.deletePurchase(record.id); else database.deleteSale(record.id);
                        selectTab(currentTab);
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private ImageView productImage(String path, int size) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(Ui.roundRect(Ui.BACKGROUND, 9, Ui.BORDER, this));
        Bitmap bitmap = PhotoManager.thumbnail(path, Ui.dp(this, size));
        image.setImageBitmap(bitmap);
        image.setContentDescription(bitmap == null ? "暂无商品图片" : "商品图片");
        return image;
    }

    private LinearLayout section(String title, String subtitle) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, title, 18, Ui.TEXT, true));
        if (!subtitle.isEmpty()) card.addView(Ui.text(this, subtitle, 12, Ui.MUTED, false));
        card.addView(Ui.space(this, 8));
        return card;
    }

    private void addColumn(LinearLayout row, View view, boolean horizontal, int weight) {
        if (horizontal) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, weight);
            if (row.getChildCount() > 0) params.setMarginStart(Ui.dp(this, 12));
            row.addView(view, params);
        } else {
            Ui.add(row, view, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, row.getChildCount() == 0 ? 0 : 12);
        }
    }

    private View rankRow(String title, String value, String note) {
        LinearLayout row = Ui.vertical(this);
        row.setPadding(0, Ui.dp(this, 7), 0, Ui.dp(this, 7));
        LinearLayout top = Ui.horizontal(this);
        top.addView(Ui.text(this, title, 14, Ui.TEXT, true), Ui.weightParams(ViewGroup.LayoutParams.WRAP_CONTENT));
        top.addView(Ui.text(this, value, 13, Ui.PRIMARY_DARK, true));
        row.addView(top);
        row.addView(Ui.text(this, note, 12, Ui.MUTED, false));
        return row;
    }

    private TextView emptyText(String text) {
        TextView view = Ui.text(this, text, 14, Ui.MUTED, false);
        view.setGravity(android.view.Gravity.CENTER);
        view.setPadding(0, Ui.dp(this, 24), 0, Ui.dp(this, 24));
        return view;
    }

    private void addCard(LinearLayout page, View card) {
        Ui.add(page, card, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
    }

    private void addAction(LinearLayout parent, String label, boolean primary, Runnable action) {
        Button button = Ui.button(this, label, primary);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = Ui.weightParams(Ui.dp(this, 44));
        params.setMarginEnd(Ui.dp(this, 6));
        parent.addView(button, params);
    }

    private String comparison(long current, long previous) {
        if (previous == 0) return current == 0 ? "较上期 0%" : "较上期 新增";
        double change = (current - previous) * 100.0 / Math.abs(previous);
        return String.format(Locale.CHINA, "较上期 %s%.1f%%", change >= 0 ? "↑" : "↓", Math.abs(change));
    }

    private String rangeLabel(DateRanges.Range range) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        if (periodMode == DateRanges.DAY) return format.format(new Date(range.start));
        return format.format(new Date(range.start)) + " - " + format.format(new Date(range.end - 86400000L));
    }

    private void chooseDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(periodAnchor);
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 12, 0, 0);
            periodAnchor = selected.getTimeInMillis();
            selectTab(currentTab);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean isStagnant(Product product, long threshold) {
        return database.isProductStagnant(product.id, threshold);
    }

    private boolean containsProduct(List<Product> products, long id) {
        for (Product product : products) if (product.id == id) return true;
        return false;
    }

    private void openProduct(long id) {
        if (wide) {
            selectedProductId = id;
            inventoryFilter = 0;
            inventorySort = 0;
            inventorySearch = "";
            selectTab(3);
        } else {
            Intent intent = new Intent(this, ProductEditActivity.class);
            intent.putExtra(ProductEditActivity.EXTRA_PRODUCT_ID, id);
            startActivity(intent);
        }
    }

    private void openAnalysis() {
        if (!new AppSettings(this).hasDeepSeekApiKey()) {
            new AlertDialog.Builder(this).setTitle("需要 DeepSeek 配置")
                    .setMessage("近期分析会把聚合后的商品经营指标发送给 DeepSeek。请先在设置中配置 API Key。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("去设置", (dialog, which) -> selectTab(4)).show();
            return;
        }
        startActivity(new Intent(this, AnalysisActivity.class));
    }

    private void openRecord(boolean purchase, long productId) {
        Intent intent = new Intent(this, RecordActivity.class);
        intent.putExtra(RecordActivity.EXTRA_IS_PURCHASE, purchase);
        intent.putExtra(RecordActivity.EXTRA_PRODUCT_ID, productId);
        startActivity(intent);
    }

    private String date(long timestamp) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(timestamp));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (settingsPanel != null) settingsPanel.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        if (settingsPanel != null) settingsPanel.destroy();
        super.onDestroy();
    }
}
