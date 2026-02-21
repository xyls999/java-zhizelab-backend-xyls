package com.atguigu.service.impl;

import com.atguigu.result.Result;
import com.atguigu.result.ResultCodeEnum;
import com.atguigu.service.ILlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class LlmServiceImpl implements ILlmService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Value("${llm.base-url:}")
    private String baseUrl;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.temperature:0.7}")
    private Double temperature;

    @Override
    public Result status() {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", StringUtils.hasText(baseUrl) && StringUtils.hasText(apiKey));
        data.put("baseUrl", baseUrl);
        data.put("model", model);
        data.put("apiKeyMasked", maskApiKey(apiKey));
        return Result.ok(data);
    }

    @Override
    public Result chat(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey)) {
            return Result.build(null, ResultCodeEnum.LLM_NOT_CONFIGURED);
        }

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", model);
            payload.put("temperature", temperature);

            ArrayNode messages = payload.putArray("messages");
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return llmError("upstream status=" + response.statusCode());
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());
            JsonNode contentNode = jsonNode.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode()) {
                return llmError("upstream response format invalid");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("answer", contentNode.asText());
            data.put("model", model);
            return Result.ok(data);
        } catch (Exception e) {
            return llmError(e.getMessage());
        }
    }

    private Result llmError(String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("reason", reason);
        return Result.build(data, ResultCodeEnum.LLM_ERROR);
    }

    private String maskApiKey(String key) {
        if (!StringUtils.hasText(key)) {
            return "";
        }
        String trimmed = key.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }
}
