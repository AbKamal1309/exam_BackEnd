package com.acoidemy.exambackend.dtos;

import lombok.Data;

import java.util.List;
@Data
public class ResponseAllTestExam {

    private List<TestResultDTO> testExamDTOList;
}
