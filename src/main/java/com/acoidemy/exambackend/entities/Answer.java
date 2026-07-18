package com.acoidemy.exambackend.entities;

import com.acoidemy.exambackend.enums.AnswerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"question", "testAnswer"})
public class Answer {

    @Id
    @EqualsAndHashCode.Include
    private String codeAnswer;

    private String answerContent;
    private String description;

    @Enumerated(EnumType.STRING)
    private AnswerStatus answerStatus = AnswerStatus.WRONG;

    @ManyToOne
    private Question question;

    @OneToMany(mappedBy = "chosenAnswer")
    private List<TestAnswer> testAnswer;

}