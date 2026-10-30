# 女装批发进销存

面向女装批发实体店的原生 Android 进销存应用，覆盖商品、进货、销售、库存、经营分析、语音录入、DeepSeek 决策辅助及数据备份恢复。

- 当前版本：**3.3**
- 最低系统：**Android 8.0（API 26）**
- 目标系统：**Android 15（API 35）**

## 下载与演示

| 物料 | 说明 |
| --- | --- |
| [标准版 APK](dist/jxc-debug.apk) | 用于现有门店数据，包名 `com.workbuddy.jxc` |
| [独立演示版 APK](dist/jxc-demo.apk) | 内置 9 款模拟女装，包名 `com.workbuddy.jxc.demo`，不影响标准版数据 |
| [DeepSeek 模拟分析报告](dist/deepseek-demo-analysis.md) | 使用真实 DeepSeek 和模拟经营数据生成 |
| [分析报告原始 JSON](dist/deepseek-demo-analysis.json) | 快销、滞销、分类机会、采购建议、风险及市场说明 |
| [功能与业务逻辑说明](进销存功能与业务逻辑说明.md) | 完整功能、数据结构、联动规则和统计口径 |

> Debug APK 用于安装体验。正式上架或分发前，请配置正式签名并重新构建 Release 包。

## 核心功能

### 响应式经营工作台

- 宽屏/平板：左侧导航、双栏业务页面。
- 手机窄屏：底部导航、单栏内容。
- 概览、进货、销售、库存、设置五个导航页面。
- 台账、商品列表、右侧表单和详情独立滚动。
- 标题、指标与筛选区固定，删除操作采用小按钮和确认弹框，降低误触。

### 商品与库存

- 自动生成六位递增货号。
- 商品名称、进货价、销售价、初始库存、季节、风格、分类。
- 风格与分类支持自由输入，也提供常见女装内置建议。
- 最多 9 张商品图片，支持拍照、相册多选、封面和单张移除。
- 全部、低库存、滞销、压货筛选，支持搜索与库存排序。
- 商品主从详情、近 7 天库存趋势和库存变动流水。

### 进货、销售与联动

- 进货单包含商品、数量、单价、供应商、业务日期和备注。
- 销售单包含商品、数量、单价、成本快照、客户、业务日期和备注。
- 进货自动增加库存，销售自动扣减库存并防止超卖。
- 保存、删除均在 SQLite 事务中执行，库存流水关联具体业务单。
- 删除销售回补库存；删除进货扣回库存，会导致负库存时拒绝删除。
- 删除商品前明确提示将级联删除其业务记录。

### 经营概览

- 日、月、年统计及上一相同周期环比。
- 销售额、毛利、进货额、销售单数、低库存、滞销指标。
- 销售折线趋势、季节销售占比环图。
- 季节、风格、价格带分析。
- 热销 Top5、易压货商品、库存天数。
- 当前压货件数、库存成本、零售货值和 30 天滞销分析。

### 统一语音业务助手

支持新增商品、进货、销售三类意图：

```text
新增裙子，进货价60，销售价70，库存20
连衣裙进货20件，进价60，供应商广州美衣
裙子卖出3件，售价70，客户张姐
```

处理流程：

1. 识别 `ADD_PRODUCT`、`PURCHASE` 或 `SALE` 意图。
2. 优先使用 DeepSeek，失败时回退本地中文规则。
3. 提取商品、数量、价格、供应商/客户和业务日期。
4. 匹配现有商品并回填可编辑表单。
5. 用户最终确认后才保存，继续执行库存校验与事务联动。

本地规则支持常见中文数字、今天/昨天/前天及常见业务表达。

### DeepSeek 近期经营分析

首页“近期分析”支持近 30、90、180 天窗口：

- 聚合全部商品和业务记录，而非分析设计图或虚构示例。
- 计算近期销量、销售额、未售天数、日均销量、库存可售天数。
- 分析快销、滞销、季节、风格、分类和价格带。
- 本地确定性规则生成基础补货量，DeepSeek 提供经营总结与采购建议。
- 建议数量扣除当前库存，慢销和无销量商品原则上不补货。
- AI 返回的商品编码及建议数量会再次校验。
- 一键携带商品、建议数量和参考进价进入进货单。
- 支持报告分享；AI 失败时明确展示本地分析，不冒充 AI 结果。

发送给 DeepSeek 的是经营聚合指标，不发送客户/供应商姓名或商品图片。市场说明仅作为一般经验参考，不代表实时市场数据。

### 数据备份与导出

- 导出真实 Excel `.xlsx`，包含商品库存、进货、销售、库存流水四个工作表。
- 导出结构化 JSON 完整备份。
- 导入 JSON 恢复商品、进货、销售和库存流水。
- 导入前校验关联关系、金额、日期、毛利及库存流水。
- 恢复在单个事务中执行，失败回滚，不破坏现有数据。
- 图片不嵌入导出文件，仅恢复本机仍存在的同名图片。

## 技术架构

- **语言**：Java 8 兼容语法。
- **UI**：原生 Android View，无大型 UI 或图表依赖。
- **数据库**：SQLite / `SQLiteOpenHelper`。
- **图表**：原生 Canvas，趋势折线、环图、分类与库存图。
- **网络**：`HttpURLConnection`，DeepSeek `deepseek-chat`。
- **图片**：应用私有目录、系统相机、系统文档选择器及内部 `ContentProvider`。
- **导出**：Android `ZipOutputStream` 生成 Office Open XML，JSON 结构化备份。
- **金额**：以整数“分”存储，避免浮点误差。

主要目录：

```text
app/src/main/java/com/workbuddy/jxc/
├── MainActivity.java                 # 响应式导航与业务页面
├── InventoryDatabase.java            # 数据库、事务、统计与删除联动
├── ProductFormView.java              # 商品共享表单
├── RecordFormView.java               # 进销共享表单
├── VoiceBusinessActivity.java        # 统一语音业务助手
├── LocalBusinessParser.java          # 本地意图与字段解析
├── DeepSeekBusinessParser.java       # DeepSeek 语音意图解析
├── AnalysisActivity.java             # 近期经营报告
├── AnalysisReportGenerator.java      # 本地经营聚合与补货规则
├── DeepSeekAnalyzer.java             # DeepSeek 经营分析
├── DataExporter.java                 # Excel / JSON 导出
├── DataImporter.java                 # JSON 校验与事务恢复
└── DemoDataSeeder.java               # 独立演示版模拟数据
```

## 构建

### 环境要求

- JDK 17。
- Android SDK Platform 35。
- Android SDK Build Tools。
- 项目自带 Gradle Wrapper，使用 Gradle 8.5。

### 标准版

```bash
./gradlew clean assembleDebug
```

Windows：

```bat
gradlew.bat clean assembleDebug
```

输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 独立演示版

```bash
./gradlew assembleDemo
```

输出：

```text
app/build/outputs/apk/demo/app-demo.apk
```

演示版使用独立包名和独立数据库，首次启动自动生成模拟商品及近 90 天业务记录。

### 静态检查

```bash
./gradlew lintDebug
```

## DeepSeek 配置

1. 打开应用“设置”。
2. 在“DeepSeek 配置”中输入 API Key 并保存。
3. 返回首页点击“近期分析”，或在语音助手中使用自然语言录单。

API Key 保存在应用私有空间，不写入源码、APK、README 或分析报告。未配置 Key 时，语音功能使用本地规则；近期 AI 分析会引导先完成配置。

## 模拟分析结果

真实 DeepSeek 模拟报告使用 9 款女装数据，覆盖快销、滞销、从未销售、低库存、不同季节、风格、分类及价格带。

报告识别结果包括：

- 近 90 天销售 135 件，销售额 27,982 元。
- 毛利 13,670 元，毛利率约 48.9%。
- 快销商品 6 款，滞销商品 3 款。
- 滞销库存 265 件，约占总库存 90.8%。
- 逐款补货数量、库存风险、过季清仓与组合销售建议。

完整内容见 [DeepSeek 模拟分析报告](dist/deepseek-demo-analysis.md)。

## 验证情况

已完成：

- Debug 与 Demo 构建。
- Android Lint 检查。
- 新增、进货、销售及中文数字语音解析。
- 日/月/年边界和月末环比检查。
- 补货公式的季节、无销量和滞销场景检查。
- Excel 文件实际读取验证。
- 使用有效 Key 调用真实 DeepSeek 生成模拟经营报告。

真机视觉、相机、麦克风和备份恢复仍建议按实际设备继续验收。

## 数据与安全

- 业务数据保存在本机 SQLite。
- 商品图片保存在应用私有目录。
- 系统云备份与设备转移已关闭。
- `local.properties`、构建目录和本机缓存不纳入版本控制。
- 不要将真实 API Key 提交到仓库；若曾泄露，请及时轮换。

## License

本项目基于 [MIT License](LICENSE) 提供。
