package com.vishnu.ticketsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishnu.ticketsystem.dto.AiClassificationResult;
import com.vishnu.ticketsystem.model.Ticket;
import com.vishnu.ticketsystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiClassificationService {

    private final TicketRepository ticketRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.6-flash}")
    private String model;

    private static final String CACHE_PREFIX = "ticket:classification:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta")
            .build();

    public void classifyAndUpdate(Ticket ticket) throws Exception {

        String cacheKey = CACHE_PREFIX + sha256(ticket.getDescription().trim().toLowerCase());

        // 1. Check Redis cache first
        AiClassificationResult cached = (AiClassificationResult) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Cache HIT for ticket id={}", ticket.getId());
            applyResult(ticket, cached, true);
            return;
        }

        log.info("Cache MISS for ticket id={} - calling AI API", ticket.getId());

        // 2. Call the AI API
        AiClassificationResult result = callGemini(ticket.getTitle(), ticket.getDescription());

        // 3. Store in cache for next time
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL);

        applyResult(ticket, result, false);
    }

    private AiClassificationResult callGemini(String title, String description) throws Exception {

        String prompt = """
                You are a support ticket triage assistant.
                Classify the ticket below and respond with ONLY valid JSON, no markdown, no extra text.

                JSON schema:
                {
                  "category": "BILLING | TECHNICAL | ACCOUNT | OTHER",
                  "priority": "LOW | MEDIUM | HIGH",
                  "suggestedResponse": "a 1-2 sentence draft reply an agent could send"
                }

                Ticket Title: %s
                Ticket Description: %s
                """.formatted(title, description);

        // Gemini request format: contents -> parts -> text
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // Extract text from Gemini's response shape:
        // { "candidates": [ { "content": { "parts": [ { "text": "..." } ] } } ] }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        String text = (String) parts.get(0).get("text");

        // Strip any accidental markdown fences before parsing
        String clean = text.replaceAll("```json|```", "").trim();

        return objectMapper.readValue(clean, AiClassificationResult.class);
    }

    private void applyResult(Ticket ticket, AiClassificationResult result, boolean fromCache) {
        ticket.setCategory(result.getCategory());
        ticket.setPriority(result.getPriority());
        ticket.setSuggestedResponse(result.getSuggestedResponse());
        ticket.setAiProcessed(true);
        ticket.setFromCache(fromCache);
        ticket.setStatus(Ticket.TicketStatus.CLASSIFIED);
        ticket.setClassifiedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
    }

    private String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
