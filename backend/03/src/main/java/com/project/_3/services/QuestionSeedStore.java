package com.project._3.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project._3.dto.QuestionCreateRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionSeedStore {

    private static final Path SEED_PATH = Path.of("uploads/questionsSeedAndETL/questions-seed.json");
    private static final String CLASSPATH_SEED = "questionsSeedAndETL/questions-seed.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;

    public QuestionSeedStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SeedWriteResult mergeAndPersist(String sourceFile, List<QuestionCreateRequest> incoming) {
        List<QuestionCreateRequest> current = readCurrentSeed();
        List<QuestionCreateRequest> merged = mergeByQuestionBody(current, incoming);
        String payload = writeSeed(merged);
        saveSeedInDatabase(sourceFile, payload);
        return new SeedWriteResult(payload, merged.size());
    }

    public List<QuestionCreateRequest> readSeedQuestions() {
        if (Files.exists(SEED_PATH)) {
            return readFromFilePath(SEED_PATH);
        }
        return readFromClasspath();
    }

    private List<QuestionCreateRequest> readCurrentSeed() {
        if (Files.exists(SEED_PATH)) {
            return readFromFilePath(SEED_PATH);
        }
        return readFromClasspath();
    }

    private List<QuestionCreateRequest> readFromFilePath(Path path) {
        try {
            QuestionWrapper wrapper = objectMapper.readValue(Files.readString(path), QuestionWrapper.class);
            if (wrapper == null || wrapper.questions() == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(wrapper.questions());
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler questions-seed.json", e);
        }
    }

    private List<QuestionCreateRequest> readFromClasspath() {
        ClassPathResource resource = new ClassPathResource(CLASSPATH_SEED);
        if (!resource.exists()) {
            return new ArrayList<>();
        }
        try (var in = resource.getInputStream()) {
            QuestionWrapper wrapper = objectMapper.readValue(in, QuestionWrapper.class);
            if (wrapper == null || wrapper.questions() == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(wrapper.questions());
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler questions-seed.json do classpath", e);
        }
    }

    private List<QuestionCreateRequest> mergeByQuestionBody(List<QuestionCreateRequest> current, List<QuestionCreateRequest> incoming) {
        Map<String, QuestionCreateRequest> merged = new LinkedHashMap<>();
        for (QuestionCreateRequest item : current) {
            merged.put(item.questionBody(), item);
        }
        for (QuestionCreateRequest item : incoming) {
            merged.put(item.questionBody(), item);
        }
        return new ArrayList<>(merged.values());
    }

    private String writeSeed(List<QuestionCreateRequest> merged) {
        try {
            Files.createDirectories(SEED_PATH.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new QuestionWrapper(merged));
            Files.writeString(SEED_PATH, json, StandardCharsets.UTF_8);
            return json;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao atualizar questions-seed.json", e);
        }
    }

    private void saveSeedInDatabase(String sourceFile, String payloadJson) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS question_seed_json (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    source_file VARCHAR(255) NOT NULL,
                    payload_json LONGTEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("INSERT INTO question_seed_json(source_file, payload_json) VALUES (?, ?)", sourceFile, payloadJson);
    }

    public record SeedWriteResult(String payloadJson, int totalQuestions) {
    }

    private record QuestionWrapper(List<QuestionCreateRequest> questions) {
    }
}
