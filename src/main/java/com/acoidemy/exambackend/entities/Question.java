package com.acoidemy.exambackend.entities;

import com.acoidemy.exambackend.enums.AttachmentType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
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

    // Initialisée à une liste vide (jamais null) : un champ null sur une entité
    // avec cascade=ALL,orphanRemoval=true oblige Hibernate à gérer la transition
    // null -> collection au moment du flush, un terrain connu pour déclencher
    // "A collection with cascade=all-delete-orphan was no longer referenced by
    // the owning entity instance" — notamment pour une Question fraîchement
    // créée (new Question()) dont les réponses sont ajoutées séparément via
    // answerRepository plutôt que via cette collection.
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", length = 20)
    private AttachmentType attachmentType;

    @Column(name = "attachment_name")
    private String attachmentName;

}