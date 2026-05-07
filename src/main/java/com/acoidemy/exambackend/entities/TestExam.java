package com.acoidemy.exambackend.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data @AllArgsConstructor @NoArgsConstructor
public class TestExam {
    @Id
    private String testExamId;
    private Date testStartingDate;

    private int score;
    @OneToMany(mappedBy = "test")
    private List<Question> questionList;

    @ManyToOne
    private Exam exam;

    @ManyToOne
    private AppUser appUser;
    @OneToMany(mappedBy = "testExam")
    private List<TestAnswer> testAnswer;

}
