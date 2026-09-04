package com.example.authapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ApiErrorResponse {
    private OffsetDateTime timestamp;
    private int status;
    private String message;
    private List<String> details;
}
