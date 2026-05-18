package com.project._3.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class QuestionResourceTextExtractor {

    public String extractFromResource(String folder, String fileName) {
        Resource sourceFile = resolveResource(folder, fileName);

        String name = fileName.toLowerCase();
        try {
            if (name.endsWith(".docx")) {
                return extractDocx(sourceFile);
            }
            if (name.endsWith(".pdf")) {
                return extractPdf(sourceFile);
            }
            throw new IllegalArgumentException("Formato inválido. Use .docx ou .pdf");
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler arquivo: " + fileName, e);
        }
    }

    private Resource resolveResource(String folder, String fileName) {
        Resource classPath = new ClassPathResource(folder + "/" + fileName);
        if (classPath.exists()) {
            return classPath;
        }

        Resource uploads = new FileSystemResource(Path.of("uploads", folder, fileName));
        if (uploads.exists()) {
            return uploads;
        }

        throw new IllegalArgumentException("Arquivo não encontrado em classpath/" + folder + " nem em uploads/" + folder + ": " + fileName);
    }

    private String extractDocx(Resource sourceFile) throws IOException {
        try (var in = sourceFile.getInputStream();
             var document = new XWPFDocument(in);
             var extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractPdf(Resource sourceFile) throws IOException {
        byte[] bytes = sourceFile.getInputStream().readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
