package com.acoidemy.exambackend.dtos;


import com.acoidemy.exambackend.enums.AnswerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class AnswerDTO {

    private String codeAnswer;
    private String answerContent;

    private AnswerStatus answerStatus;// CORRECT | WRONG
    private String description;

    private String questionId;


}
