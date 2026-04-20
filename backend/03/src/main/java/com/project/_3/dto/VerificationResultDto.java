package com.project._3.dto;

public record VerificationResultDto(
        boolean valid,
        String message,
        String requiredUsage,
        boolean found
) {}

