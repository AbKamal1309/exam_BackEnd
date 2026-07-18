package com.acoidemy.exambackend.dtos;

import com.acoidemy.exambackend.enums.ExamStatus;
import com.acoidemy.exambackend.enums.ExamVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data @NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamDTO {
    private String          codeExam;
    private Date            dateCreation;
    private int             numberOfQuestions;
    private ExamStatus      status;
    private ExamVisibility visibility;
    private String          description;
    private Long            userId;
    private String          originalExamId;
    private List<QuestionDTO> questionDTOList;
    private Integer durationMinutes;



}
