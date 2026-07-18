package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.enums.ExamVisibility;
import com.acoidemy.exambackend.exceptions.AnswerNotFoundException;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.QuestionNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import jakarta.transaction.Transactional;

import java.util.List;

public interface ExamService {

    ExamDTO saveExam(ExamDTO examDTO) throws UserNotFoundException, ExamNotFoundException;

    List<ExamDTO> listExams();

    ExamDTO getExam(String codeExam) throws ExamNotFoundException;

    ExamDTO updateExam(ExamDTO examDTO, Long userId) throws UserNotFoundException, ExamNotFoundException;

    void deleteExam(String codeExam, Long userId) throws ExamNotFoundException;

    // Suppression par un administrateur, sans vérification de propriétaire
    void adminDeleteExam(String codeExam) throws ExamNotFoundException;

    QuestionDTO saveQuestion(QuestionDTO questionDTO, Long userId) throws ExamNotFoundException;

    QuestionDTO saveQuestionWithAnswers(QuestionDTO questionDTO, Long userId) throws ExamNotFoundException;

    List<QuestionDTO> listAllQuestions();//list all questions in the DB
    List<QuestionDTO> listQuestions(String codeExam) throws ExamNotFoundException;//list questions for exam
    QuestionDTO getQuestion(String codeQuestion) throws QuestionNotFoundException;
    QuestionDTO updateQuestion(QuestionDTO questionDTO, Long userId) throws QuestionNotFoundException;
    void deleteQuestion(String codeQuestion, Long userId);

    AnswerDTO saveAnswer(AnswerDTO answerDTO, Long userId) throws QuestionNotFoundException;
    List<AnswerDTO> listAllAnswers();//list all answers in the DB
    List<AnswerDTO> listAnswers(String codeQuestion) throws QuestionNotFoundException;
    AnswerDTO getAnswer(String codeAnswer) throws AnswerNotFoundException;
    AnswerDTO updateAnswer(AnswerDTO answerDTO, Long userId) throws QuestionNotFoundException, AnswerNotFoundException;
    void deleteAnswer(String codeAnswer, Long userId);


    QuestionDTO updateQuestionWithAnswers(QuestionDTO questionDTO, Long userId) throws QuestionNotFoundException;


    List<ExamDTO> listExamsByUser(Long userId) throws UserNotFoundException;

    ExamDTO saveExamAllQuestionsAndAnswers(ExamDTO examDTO) throws UserNotFoundException;


    ////New Update

    // ── 1. Créer un examen (avec visibilité) ──────────────────────
    public ExamDTO createExam(Long userId, ExamDTO dto);

    // ── 2. Modifier la visibilité d'un examen ────────────────────
    public ExamDTO updateExamVisibility(String codeExam, ExamVisibility visibility, Long userId);

    // ── 3. Copier un examen PUBLIC et l'ajouter à ses examens ────
    // Un utilisateur peut copier un examen PUBLIC et le modifier
    public ExamDTO copyPublicExam(CopyExamDTO dto);

    // ── 4. Partager un examen avec un groupe ──────────────────────
    // Seul un admin du groupe peut partager :
    //   - un examen PUBLIC (n'importe lequel)
    //   - OU un examen qu'il a créé lui-même
    public GroupResponseDTO shareExamWithGroup(ShareExamWithGroupDTO dto);

    // ── 5. Retirer un examen partagé d'un groupe ─────────────────
    public GroupResponseDTO unshareExamFromGroup(ShareExamWithGroupDTO dto);


    // ── 6. Lister les examens partagés avec un groupe ─────────────
    @Transactional()
    public List<ExamDTO> getSharedExamsForGroup(Long groupId, Long userId);

    // ── 7. Lister tous les examens publics ────────────────────────
    @Transactional()
    public List<ExamDTO> getPublicExams();

    // ── 8. Historique des tests d'un utilisateur pour un examen ──
    @Transactional()
    public List<TestResultDTO> getUserTestsForExam(Long userId, String codeExam);




}
