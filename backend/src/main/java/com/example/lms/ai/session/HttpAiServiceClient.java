package com.example.lms.ai.session;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.example.lms.shared.json.JsonReader;

/**
 * Gọi {@code POST /v1/generate}. Lỗi mạng/5xx -> UNAVAILABLE (tự retry); 422 hoặc JSON hỏng -> INVALID_OUTPUT.
 * Không đưa thân response lỗi vào thông điệp ngoại lệ để tránh lộ chi tiết nội bộ.
 */
@Component
public class HttpAiServiceClient implements AiServiceClient {

    private final RestClient http;

    public HttpAiServiceClient(@Value("${ai.service.base-url:http://localhost:8000}") String baseUrl,
            @Value("${ai.service.token:}") String token,
            @Value("${ai.service.connect-timeout:PT3S}") Duration connectTimeout,
            @Value("${ai.service.read-timeout:PT60S}") Duration readTimeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        factory.setReadTimeout(readTimeout);
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory);
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("X-Internal-Token", token);
        }
        this.http = builder.build();
    }

    @Override
    public GenerateOutcome generate(GenerateCall call) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", call.query());
        body.put("allowed_course_ids", call.allowedCourseIds());
        body.put("allowed_document_ids", call.allowedDocumentIds());
        body.put("target_skill", call.targetSkill());
        body.put("target_difficulty", call.targetDifficulty());
        body.put("mode", "practice");
        body.put("language", call.language());
        body.put("allow_new_task_generation", true);
        body.put("learner_profile", call.learnerProfile());
        String response;
        try {
            response = http.post().uri("/v1/generate").contentType(MediaType.APPLICATION_JSON)
                    .body(JsonSupport.toJson(body)).retrieve().body(String.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 422) {
                throw new AiServiceException(AiServiceException.Kind.INVALID_OUTPUT, "AI service rejected the output");
            }
            throw new AiServiceException(AiServiceException.Kind.UNAVAILABLE,
                    "AI service returned HTTP " + e.getStatusCode().value());
        } catch (RuntimeException e) {
            throw new AiServiceException(AiServiceException.Kind.UNAVAILABLE, "AI service is unreachable", e);
        }
        return parse(response);
    }

    static GenerateOutcome parse(String response) {
        try {
            Map<String, Object> root = JsonReader.parseObject(response);
            List<String> retrieved = strings(root.get("retrieved_doc_ids"));
            String backend = String.valueOf(root.getOrDefault("llm_backend", "unknown"));
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) root.get("result");
            if (result == null) {
                throw new IllegalArgumentException("missing result");
            }
            if ("insufficient_context".equals(result.get("status"))) {
                return GenerateOutcome.insufficient(retrieved, backend);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> task = (Map<String, Object>) result.get("task");
            if (!"ok".equals(result.get("status")) || task == null) {
                throw new IllegalArgumentException("unexpected status");
            }
            return new GenerateOutcome(true, (String) task.get("prompt"), task.get("answer_key"),
                    (String) task.get("response_type"), strings(task.get("hints")),
                    (String) task.get("personalization_reason"), strings(task.get("supporting_doc_ids")), retrieved,
                    backend);
        } catch (RuntimeException e) {
            throw new AiServiceException(AiServiceException.Kind.INVALID_OUTPUT, "AI service response is malformed", e);
        }
    }

    private static List<String> strings(Object value) {
        if (value == null) {
            return List.of();
        }
        return ((List<?>) value).stream().map(String::valueOf).toList();
    }
}
