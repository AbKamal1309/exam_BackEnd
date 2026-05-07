package com.acoidemy.exambackend.dtos;

import com.acoidemy.exambackend.entities.Question;
import com.acoidemy.exambackend.enums.AnswerStatus;
import lombok.Data;

@Data

public class AnswerDTO {

    private String codeAnswer;
    private String answerContent;

    private AnswerStatus answerStatus;
    private String description;

    private String questionId;


}
