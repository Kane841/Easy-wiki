package com.easywiki.agent;

import com.easywiki.config.AgentProperties;
import com.easywiki.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DeepSeekClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public DeepSeekClient(AgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(properties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(properties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
    }

    public String chat(String systemPrompt, List<ChatMessage> messages) {
        String url = properties.getApiBaseUrl().replaceAll("/$", "") + "/v1/chat/completions";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getModel());
        body.put("max_tokens", properties.getMaxTokens());

        ArrayNode msgArray = body.putArray("messages");
        ObjectNode system = msgArray.addObject();
        system.put("role", "system");
        system.put("content", systemPrompt);

        for (ChatMessage msg : messages) {
            ObjectNode node = msgArray.addObject();
            node.put("role", msg.role());
            node.put("content", msg.content());
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.warn("DeepSeek API error: status={}, body={}", response.code(), responseBody);
                throw new BusinessException(502, "AI 服务暂时不可用");
            }
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (IOException e) {
            log.warn("DeepSeek API call failed", e);
            throw new BusinessException(502, "AI 服务调用失败");
        }
    }
}
