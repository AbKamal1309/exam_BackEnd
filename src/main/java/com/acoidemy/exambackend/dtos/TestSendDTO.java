package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSendDTO {

    private Long userId;
    private String codeExam;

    private List<QuestionDTO> questionDTOS;
}
