package com.acoidemy.exambackend.dtos;

import com.acoidemy.exambackend.entities.Answer;
import com.acoidemy.exambackend.entities.Exam;
import lombok.Data;

import java.util.List;
@Data
public class QuestionDTO {

    private String codeQuestion;
    private String questionContent;
    private String description;

    private String examId;

    private List<AnswerDTO> answers;


}
