package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionCorrectionDTO {
    private String questionContent;
    private String description;
    private String attachmentUrl;
    private String attachmentType;
    private String attachmentName;
    private List<AnswerCorrectionDTO> answers;
    private boolean fullyCorrect;
}
