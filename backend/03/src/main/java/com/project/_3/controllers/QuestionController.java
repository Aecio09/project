package com.project._3.controllers;

import com.project._3.dto.QuestionCreateRequest;
import com.project._3.entities.Question;
import com.project._3.services.QuestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<Question> createQuestion(@Valid @RequestBody QuestionCreateRequest request) {
        Question savedQuestion = questionService.createQuestion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedQuestion);
    }
}

