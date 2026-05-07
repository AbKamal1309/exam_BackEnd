package com.acoidemy.exambackend.dtos;

import lombok.Data;

@Data
public class TestExamDTO {

    private String userRequestName;
    private String codeExam;
    private String userNameExamSetter;

    private ExamDTO examDTO;
}
