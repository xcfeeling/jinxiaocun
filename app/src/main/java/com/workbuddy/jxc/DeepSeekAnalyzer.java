package com.workbuddy.jxc;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DeepSeekAnalyzer {
    private DeepSeekAnalyzer() {
    }

    public static AnalysisReport analyze(AnalysisSnapshot snapshot, String apiKey) throws Exception {
        JSONObject request = new JSONObject();
        request.put("model", "deepseek-chat");
        request.put("stream", false);
        request.put("temperature", 0.2);
        request.put("response_format", new JSONObject().put("type", "json_object"));
        String system = "你是女装批发经营分析师。根据用户提供的全部商品聚合经营数据给出可执行建议，" +
                "不要编造实时市场数据，不要把一般季节性经验说成确定事实。快销与滞销必须参考近期销量、库存、可售天数和未售天数；" +
                "补货量必须扣除当前库存，滞销或近期无销量商品原则上不建议补货。兼顾季节、风格、分类、价格带和毛利。" +
                "只返回JSON对象：summary字符串；fastMoving数组（code,name,reason）；slowMoving数组（code,name,reason）；"
                + "categoryInsights字符串数组；purchaseRecommendations数组（code,name,suggestedQuantity非负整数,reason）；"
                + "risks字符串数组；marketNotes字符串。商品code必须来自输入，建议数量0到9999。";
        request.put("messages", new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", system))
                .put(new JSONObject().put("role", "user").put("content", snapshot.toJson().toString())));
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "https://api.deepseek.com/chat/completions").openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(90000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(request.toString().getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            if (status == 401) throw new IOException("DeepSeek API Key 无效，请检查设置");
            if (status == 429) throw new IOException("DeepSeek 请求频繁或余额不足，请稍后重试");
            if (status < 200 || status >= 300) throw new IOException("DeepSeek 分析失败（HTTP " + status + "）");
            String response = read(connection.getInputStream());
            String content = new JSONObject(response).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content").trim();
            if (content.startsWith("```")) {
                content = content.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
            }
            return parse(new JSONObject(content));
        } finally {
            connection.disconnect();
        }
    }

    private static AnalysisReport parse(JSONObject json) {
        String summary = json.optString("summary").trim();
        if (summary.isEmpty()) throw new IllegalArgumentException("DeepSeek 返回的分析缺少总结");
        AnalysisReport report = new AnalysisReport(summary, json.optString("marketNotes"));
        readItems(json.optJSONArray("fastMoving"), report.fastMoving, false);
        readItems(json.optJSONArray("slowMoving"), report.slowMoving, false);
        readItems(json.optJSONArray("purchaseRecommendations"), report.purchases, true);
        readStrings(json.optJSONArray("categoryInsights"), report.categoryInsights);
        readStrings(json.optJSONArray("risks"), report.risks);
        return report;
    }

    private static void readItems(JSONArray array, java.util.List<AnalysisItem> target, boolean purchase) {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            String code = item.optString("code").trim();
            String name = item.optString("name").trim();
            String reason = item.optString("reason").trim();
            int quantity = purchase ? item.optInt("suggestedQuantity", -1) : 0;
            if (code.isEmpty() || reason.isEmpty() || (purchase && (quantity < 0 || quantity > 9999))) continue;
            target.add(new AnalysisItem(code, name, quantity, reason));
        }
    }

    private static void readStrings(JSONArray array, java.util.List<String> target) {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i).trim();
            if (!value.isEmpty()) target.add(value);
        }
    }

    private static String read(InputStream input) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line);
        }
        return text.toString();
    }
}
