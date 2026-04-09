package com.project._3.exceptions;

public class AnswerRejectedByAiException extends RuntimeException {
    private final String aiMessage;

    public AnswerRejectedByAiException(String aiMessage) {
        super("Answer rejected by AI verification: " + aiMessage);
        this.aiMessage = aiMessage;
    }

    public String getAiMessage() {
        return aiMessage;
    }
}

