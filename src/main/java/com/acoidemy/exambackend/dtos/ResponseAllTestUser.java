package com.acoidemy.exambackend.dtos;

import lombok.Data;

import java.util.List;
@Data
public class ResponseAllTestUser {

    private List<TestResultDTO> testResultDTOList;
}
