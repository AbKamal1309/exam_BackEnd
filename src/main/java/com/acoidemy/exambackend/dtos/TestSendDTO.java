package com.acoidemy.exambackend.dtos;

import lombok.Data;

import java.util.List;
@Data
public class TestSendDTO {

    private Long userId;
    private String codeExam;

    private List<QuestionDTO> questionDTOS;
}
