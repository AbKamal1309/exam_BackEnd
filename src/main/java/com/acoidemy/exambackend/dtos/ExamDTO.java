package com.acoidemy.exambackend.dtos;

import com.acoidemy.exambackend.enums.ExamStatus;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ExamDTO {
    private String codeExam;
    private Date dateCreation;
    private int numberOfQuestions;

    private ExamStatus status;
    private String description;

    private Long userId;
    private List<QuestionDTO> questionDTOList;



}
