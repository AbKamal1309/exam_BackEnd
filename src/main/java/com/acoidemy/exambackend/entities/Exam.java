package com.acoidemy.exambackend.entities;

import com.acoidemy.exambackend.enums.ExamStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data @AllArgsConstructor @NoArgsConstructor
public class Exam {
    @Id
    private String codeExam;
    private Date dateCreation;
    private int numberOfQuestions;
    @Enumerated(EnumType.STRING)
    private ExamStatus status;
    private String description;
    @ManyToOne
    private AppUser appUser;

    @OneToMany(mappedBy = "exam")
    private List<Question> questions;

    @OneToMany(mappedBy = "exam")
    private List<TestExam> testExam;
}
