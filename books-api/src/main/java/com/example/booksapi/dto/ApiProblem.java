package com.example.booksapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

// RFC 9457 Problem Details for HTTP APIs
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiProblem {
    private String type;
    private String title;
    private Integer status;
    private String detail;
    private String instance;
}
