package com.project._3.services;


import com.project._3.entities.Question;
import com.project._3.dto.QuestionCreateRequest;
import com.project._3.repositories.QuestionRepository;
import org.springframework.stereotype.Service;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Question createQuestion(QuestionCreateRequest request) {
        Question question = new Question();
        question.setQuestionBody(request.questionBody());
        question.setType(request.type());
        question.setDifficulty(request.difficulty());
        question.setRequiredUsage(request.requiredUsage());
        question.setTopic(request.topic());
        return questionRepository.save(question);
    }
}
