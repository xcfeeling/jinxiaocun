package com.workbuddy.jxc;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DeepSeekParser {
    private static final String ENDPOINT = "https://api.deepseek.com/chat/completions";

    private DeepSeekParser() {
    }

    public static ProductDraft parse(String command, String apiKey) throws Exception {
        JSONObject request = new JSONObject();
        request.put("model", "deepseek-chat");
        request.put("stream", false);
        request.put("temperature", 0);
        request.put("response_format", new JSONObject().put("type", "json_object"));
        request.put("messages", new org.json.JSONArray()
                .put(new JSONObject().put("role", "system").put("content",
                        "你是服装批发进销存的数据录入助手。把用户指令解析成JSON对象，只能包含" +
                                "name（商品名称，字符串）、purchasePrice（进货价，数字）、salePrice（销售价，数字）、" +
                                "stock（库存，非负整数）四个字段。价格单位是元。忽略货号自增长等要求，货号由系统生成。" +
                                "不要解释，不要返回其他字段。缺少信息时在name中返回空字符串。"))
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
            if (status < 200 || status >= 300) {
                if (status == 401) {
                    throw new IOException("DeepSeek API Key 无效，请检查设置");
                }
                if (status == 429) {
                    throw new IOException("DeepSeek 请求过于频繁或余额不足，请稍后重试");
                }
                throw new IOException("DeepSeek 请求失败（HTTP " + status + "）");
            }
            String response = read(connection.getInputStream());
            String content = new JSONObject(response).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content").trim();
            if (content.startsWith("```")) {
                content = content.replaceFirst("^```(?:json)?\\s*", "")
                        .replaceFirst("\\s*```$", "");
            }
            return draftFromJson(new JSONObject(content));
        } finally {
            connection.disconnect();
        }
    }

    private static ProductDraft draftFromJson(JSONObject json) {
        String name = json.optString("name", "").trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("大模型未能识别完整商品信息，请补充商品名称和价格、库存");
        }
        long purchase = jsonMoney(json, "purchasePrice");
        long sale = jsonMoney(json, "salePrice");
        if (!json.has("stock")) {
            throw new IllegalArgumentException("没有识别到库存，请补充后重试");
        }
        int stock;
        try {
            stock = new BigDecimal(json.getString("stock")).intValueExact();
        } catch (Exception e) {
            throw new IllegalArgumentException("库存必须是非负整数");
        }
        return new ProductDraft(name, purchase, sale, stock);
    }

    private static long jsonMoney(JSONObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalArgumentException("没有识别到商品价格，请补充后重试");
        }
        try {
            return new BigDecimal(json.getString(key)).movePointRight(2).longValueExact();
        } catch (Exception e) {
            throw new IllegalArgumentException("商品价格格式不正确");
        }
    }

    private static String read(InputStream input) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
        }
        return text.toString();
    }
}
