package com.acoidemy.exambackend.entities;

import com.acoidemy.exambackend.enums.AttachmentType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"exam", "test", "answers"})
public class Question {

    @Id
    @EqualsAndHashCode.Include
    private String codeQuestion;

    private String questionContent;

    private Date dateCreation;
    private String description;
    private int appreciatedPoint;

    @ManyToOne
    private Exam exam;

    @ManyToOne
    private TestExam test;

    @OneToMany(mappedBy = "question")
    private List<Answer> answers;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", length = 20)
    private AttachmentType attachmentType;

    @Column(name = "attachment_name")
    private String attachmentName;

}