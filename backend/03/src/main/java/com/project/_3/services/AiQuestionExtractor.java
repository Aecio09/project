package com.project._3.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project._3.dto.QuestionCreateRequest;
import com.project._3.entities.Question;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiQuestionExtractor {

    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    public AiQuestionExtractor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.apiKey = loadApiKey();
    }

    public List<QuestionCreateRequest> extractQuestions(String text) {
        String rawJson = askAi(text);
        return parseQuestions(rawJson);
    }

    private String askAi(String text) {
        Map<String, Object> payload = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", """
                                Você deve retornar SOMENTE JSON válido, sem markdown, sem texto extra e sem blocos de código.
                                Estrutura obrigatória:
                                {"questions":[{"questionBody":"...","type":"...","difficulty":"...","requiredUsage":"...","topic":"..."}]}
                                Regras críticas:
                                - Use apenas 1 valor por campo de enum.
                                - NÃO use placeholders com barra vertical como "A|B|C".
                                - NÃO use descrições em português nos campos de enum.
                                Valores permitidos:
                                - type: MULTIPLE_CHOICE ou PRACTICAL
                                - difficulty: EASY, MEDIUM, HARD
                                - topic: OPERADORES_TIPOS_E_VARIAVEIS, EXECUCAO_CONDICIONAL, OPERADORES_LOGICOS, LACOS, SUBPROGRAMAS, VETORES, ARRAYS, TIPOS_CRIADOS_PELO_PROGRAMADOR
                                - requiredUsage: um valor exato do enum de RequiredUsage ou null
                                Exemplo válido:
                                {"questionBody":"...","type":"PRACTICAL","difficulty":"MEDIUM","requiredUsage":"FOR","topic":"LACOS"}
                                """),
                        Map.of("role", "user", "content", "Extraia as questões do texto abaixo e devolva apenas o JSON.\nTexto:\n" + text)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<?, ?> response = restTemplate.postForObject(URL, new HttpEntity<>(payload, headers), Map.class);
        if (response == null) {
            throw new IllegalStateException("Resposta vazia da IA");
        }

        List<?> choices = (List<?>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("Resposta da IA sem choices");
        }

        Map<?, ?> firstChoice = (Map<?, ?>) choices.getFirst();
        Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
        return String.valueOf(message.get("content"));
    }

    private List<QuestionCreateRequest> parseQuestions(String rawJson) {
        try {
            String cleaned = cleanJson(rawJson);
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode questionsNode = root.isArray() ? root : root.path("questions");

            if (!questionsNode.isArray() || questionsNode.isEmpty()) {
                throw new IllegalStateException("JSON da IA sem perguntas");
            }

            List<QuestionCreateRequest> questions = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                questions.add(new QuestionCreateRequest(
                        requiredText(node, "questionBody"),
                        parseQuestionType(node),
                        parseDifficulty(node),
                        parseOptionalRequiredUsage(node),
                        parseTopic(node)
                ));
            }
            return questions;
        } catch (IOException e) {
            throw new IllegalStateException("JSON inválido retornado pela IA", e);
        }
    }

    private String cleanJson(String rawJson) {
        String cleaned = rawJson == null ? "" : rawJson.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        int startObject = cleaned.indexOf('{');
        int startArray = cleaned.indexOf('[');
        int start = startObject < 0 ? startArray : (startArray < 0 ? startObject : Math.min(startObject, startArray));

        int endObject = cleaned.lastIndexOf('}');
        int endArray = cleaned.lastIndexOf(']');
        int end = Math.max(endObject, endArray);

        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalStateException("Resposta da IA não contém JSON válido");
        }
        return cleaned.substring(start, end + 1).trim();
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Campo obrigatório ausente no JSON da IA: " + field);
        }
        return value;
    }

    private Question.QuestionType parseQuestionType(JsonNode node) {
        String value = requiredText(node, "type");
        return parseRequiredEnum(value, "type", Question.QuestionType.class, Map.of(
                "MULTIPLA_ESCOLHA", "MULTIPLE_CHOICE",
                "MULTIPLA ESCOLHA", "MULTIPLE_CHOICE",
                "MULTIPLE CHOICE", "MULTIPLE_CHOICE",
                "PRATICA", "PRACTICAL",
                "PRACTICA", "PRACTICAL"
        ));
    }

    private Question.DifficultyLevel parseDifficulty(JsonNode node) {
        String value = requiredText(node, "difficulty");
        return parseRequiredEnum(value, "difficulty", Question.DifficultyLevel.class, Map.of(
                "FACIL", "EASY",
                "MEDIO", "MEDIUM",
                "MEDIA", "MEDIUM",
                "DIFICIL", "HARD"
        ));
    }

    private Question.Topics parseTopic(JsonNode node) {
        String value = requiredText(node, "topic");
        return parseRequiredEnum(value, "topic", Question.Topics.class, Map.of(
                "OPERADORES TIPOS E VARIAVEIS", "OPERADORES_TIPOS_E_VARIAVEIS",
                "OPERADORES_LOGICOS", "OPERADORES_LOGICOS",
                "EXECUCAO CONDICIONAL", "EXECUCAO_CONDICIONAL",
                "TIPOS_CRIADOS_PELO_PROGRAMADOR", "TIPOS_CRIADOS_PELO_PROGRAMADOR",
                "TIPOS CRIADOS PELO PROGRAMADOR", "TIPOS_CRIADOS_PELO_PROGRAMADOR"
        ));
    }

    private Question.RequiredUsage parseOptionalRequiredUsage(JsonNode node) {
        String value = node.path("requiredUsage").asText(null);
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return parseRequiredEnum(value, "requiredUsage", Question.RequiredUsage.class, Map.of());
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private <E extends Enum<E>> E parseRequiredEnum(String rawValue, String field, Class<E> enumClass, Map<String, String> aliases) {
        String normalized = normalizeEnumValue(rawValue);

        E exact = tryParseEnum(resolveAlias(normalized, aliases), enumClass);
        if (exact != null) {
            return exact;
        }

        for (String part : normalized.split("[|/,;]")) {
            String token = resolveAlias(normalizeEnumValue(part), aliases);
            E candidate = tryParseEnum(token, enumClass);
            if (candidate != null) {
                return candidate;
            }
        }

        for (E enumValue : enumClass.getEnumConstants()) {
            if (normalized.contains(enumValue.name())) {
                return enumValue;
            }
        }

        throw new IllegalStateException("Valor inválido para campo " + field + ": " + rawValue);
    }

    private <E extends Enum<E>> E tryParseEnum(String value, Class<E> enumClass) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String resolveAlias(String value, Map<String, String> aliases) {
        return aliases.getOrDefault(value, value);
    }

    private String normalizeEnumValue(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents
                .trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
    }

    private String loadApiKey() {
        try {
            return new String(new ClassPathResource("APIKEY").getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("API key not found in resources/APIKEY", e);
        }
    }
}
