package com.project._3.services;

import com.project._3.dto.QuestionCreateRequest;
import com.project._3.dto.QuestionSeedImportResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class QuestionSeedImportService {

    private static final String RESOURCE_FOLDER = "questionsSeedAndETL";
    private static final Path UPLOAD_FOLDER = Path.of("uploads", RESOURCE_FOLDER);

    private final QuestionResourceTextExtractor textExtractor;
    private final AiQuestionExtractor aiQuestionExtractor;
    private final QuestionSeedStore questionSeedStore;
    private final QuestionService questionService;

    public QuestionSeedImportService(QuestionResourceTextExtractor textExtractor,
                                     AiQuestionExtractor aiQuestionExtractor,
                                     QuestionSeedStore questionSeedStore,
                                     QuestionService questionService) {
        this.textExtractor = textExtractor;
        this.aiQuestionExtractor = aiQuestionExtractor;
        this.questionSeedStore = questionSeedStore;
        this.questionService = questionService;
    }

    public QuestionSeedImportResponse importFromResource(String fileName) {
        String text = textExtractor.extractFromResource(RESOURCE_FOLDER, fileName);
        List<QuestionCreateRequest> extractedQuestions = aiQuestionExtractor.extractQuestions(text);

        QuestionSeedStore.SeedWriteResult seedResult = questionSeedStore.mergeAndPersist(fileName, extractedQuestions);
        int insertedQuestions = questionService.importSeedQuestionsIfMissing(extractedQuestions);

        return new QuestionSeedImportResponse(
                fileName,
                extractedQuestions.size(),
                insertedQuestions,
                seedResult.totalQuestions()
        );
    }

    public QuestionSeedImportResponse importFromUploadedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo inválido para importação");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("Arquivo sem nome");
        }

        String normalizedName = originalName.trim();
        String lowerName = normalizedName.toLowerCase();
        if (!lowerName.endsWith(".docx") && !lowerName.endsWith(".pdf")) {
            throw new IllegalArgumentException("Formato inválido. Use .docx ou .pdf");
        }

        try {
            Files.createDirectories(UPLOAD_FOLDER);
            Path target = UPLOAD_FOLDER.resolve(normalizedName);
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar arquivo enviado", e);
        }

        return importFromResource(normalizedName);
    }
}
