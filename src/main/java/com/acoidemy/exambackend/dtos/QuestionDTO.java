package com.acoidemy.exambackend.dtos;


import com.acoidemy.exambackend.enums.AttachmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDTO {

    private String codeQuestion;
    private String questionContent;
    private String description;

    private String examId;

    private List<AnswerDTO> answers;

    private String attachmentUrl;
    private AttachmentType attachmentType;
    private String attachmentName;


}
