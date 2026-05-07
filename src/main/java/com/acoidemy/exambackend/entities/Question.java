package com.acoidemy.exambackend.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data   @AllArgsConstructor @NoArgsConstructor
public class Question {
    @Id
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


}
