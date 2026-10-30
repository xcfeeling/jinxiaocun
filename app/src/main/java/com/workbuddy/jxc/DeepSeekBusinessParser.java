package com.workbuddy.jxc;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DeepSeekBusinessParser {
    private static final String ENDPOINT = "https://api.deepseek.com/chat/completions";

    private DeepSeekBusinessParser() {
    }

    public static BusinessDraft parse(String command, String apiKey, String preferredIntent) throws Exception {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        JSONObject request = new JSONObject();
        request.put("model", "deepseek-chat");
        request.put("stream", false);
        request.put("temperature", 0);
        request.put("response_format", new JSONObject().put("type", "json_object"));
        String system = "你是女装批发进销存语音助手。判断用户意图并只返回JSON对象，字段为：" +
                "intent（ADD_PRODUCT新增商品、PURCHASE进货、SALE销售）、productName、purchasePrice、salePrice、stock、" +
                "quantity、unitPrice、partner、businessDate。新增商品必须给purchasePrice、salePrice、stock；" +
                "进货销售必须给productName、quantity、unitPrice。partner是供应商或客户，可为空字符串。" +
                "businessDate使用yyyy-MM-dd，未提及时用" + today + "。忽略货号自增长，不要解释。" +
                (preferredIntent == null ? "" : "若语义无法判断意图，可参考当前页面意图" + preferredIntent + "。");
        request.put("messages", new org.json.JSONArray()
                .put(new JSONObject().put("role", "system").put("content", system))
                .put(new JSONObject().put("role", "user").put("content", command)));
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(45000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(request.toString().getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            if (status == 401) throw new IOException("DeepSeek API Key 无效，请检查设置");
            if (status == 429) throw new IOException("DeepSeek 请求频繁或余额不足，请稍后重试");
            if (status < 200 || status >= 300) throw new IOException("DeepSeek 请求失败（HTTP " + status + "）");
            String response = read(connection.getInputStream());
            String content = new JSONObject(response).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content").trim();
            if (content.startsWith("```")) {
                content = content.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
            }
            return fromJson(new JSONObject(content));
        } finally {
            connection.disconnect();
        }
    }

    private static BusinessDraft fromJson(JSONObject json) {
        String intent = json.optString("intent");
        String name = json.optString("productName").trim();
        if (BusinessDraft.ADD_PRODUCT.equals(intent)) {
            return new BusinessDraft(new ProductDraft(name, money(json, "purchasePrice"),
                    money(json, "salePrice"), integer(json, "stock")));
        }
        long date = System.currentTimeMillis();
        String dateText = json.optString("businessDate");
        if (!dateText.isEmpty()) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
                format.setLenient(false);
                Date parsed = format.parse(dateText);
                if (parsed != null) date = parsed.getTime() + 12L * 60 * 60 * 1000;
            } catch (Exception e) {
                throw new IllegalArgumentException("业务日期格式不正确");
            }
        }
        return new BusinessDraft(intent, name, integer(json, "quantity"), money(json, "unitPrice"),
                json.optString("partner"), date);
    }

    private static long money(JSONObject json, String key) {
        try {
            return Formatters.parseMoney(json.getString(key));
        } catch (Exception e) {
            throw new IllegalArgumentException("缺少或无法识别价格字段：" + key);
        }
    }

    private static int integer(JSONObject json, String key) {
        try {
            return new java.math.BigDecimal(json.getString(key)).intValueExact();
        } catch (Exception e) {
            throw new IllegalArgumentException("缺少或无法识别整数字段：" + key);
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
