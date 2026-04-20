package com.project._3.dto;

public record AiVerificationResponseDto(
        boolean approved,
        String feedback
) {
}
