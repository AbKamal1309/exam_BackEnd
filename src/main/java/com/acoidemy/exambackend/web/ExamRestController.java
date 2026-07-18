package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.enums.ExamVisibility;
import com.acoidemy.exambackend.exceptions.AnswerNotFoundException;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.QuestionNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.security.SecurityUtils;
import com.acoidemy.exambackend.services.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ExamRestController {

    private final ExamService examService;
    private final SecurityUtils securityUtils;

    @GetMapping("/exams")
    public List<ExamDTO> exams(){
        return examService.listExams();
    }

    @GetMapping("/exams/{id}")
    public ExamDTO getExam(@PathVariable(name = "id") String codeExam) throws ExamNotFoundException{
        return examService.getExam(codeExam);
    }
    @GetMapping("/examsOfUser/{userId}")
    public List<ExamDTO> getExamsForUser(@PathVariable Long userId) throws UserNotFoundException {
        return examService.listExamsByUser(userId);
    }

    @PostMapping("/exams")
    @PreAuthorize("isAuthenticated()")
    public ExamDTO saveExam(@RequestBody ExamDTO examDTO, Authentication authentication) throws UserNotFoundException, ExamNotFoundException {
        Long userId = securityUtils.getCurrentUserId(authentication);
        return examService.createExam(userId, examDTO);
    }

    @PostMapping("/examAllQuestionsAndAnswers/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ExamDTO saveExamAllQuestionsAndAnswers(@PathVariable Long userId
            ,@RequestBody ExamDTO examDTO, Authentication authentication)
            throws UserNotFoundException {
        examDTO.setUserId(securityUtils.getCurrentUserId(authentication));
        return examService.saveExamAllQuestionsAndAnswers(examDTO);
    }

    @PutMapping("/exams/{codeExam}")
    @PreAuthorize("isAuthenticated()")
    public ExamDTO updateExam(@PathVariable String codeExam,@RequestBody ExamDTO examDTO, Authentication authentication)
            throws UserNotFoundException, ExamNotFoundException {
        examDTO.setCodeExam(codeExam);
        Long userId = securityUtils.getCurrentUserId(authentication);
        return examService.updateExam(examDTO, userId);
    }

    @DeleteMapping("/exams/{codeExam}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteExam(
            @PathVariable String codeExam,
            Authentication authentication
    ) throws ExamNotFoundException {
        Long userId = securityUtils.getCurrentUserId(authentication);
        examService.deleteExam(codeExam, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/questions")
    public List<QuestionDTO> getAllQuestions(){

        return examService.listAllQuestions();
    }

    @GetMapping("/exams/{codeExam}/questions")
    public List<QuestionDTO> getQuestions(@PathVariable String codeExam) throws ExamNotFoundException {
        return examService.listQuestions(codeExam);
    }
    @GetMapping("/questions/{codeQuestion}")
    public QuestionDTO getQuestion(@PathVariable String codeQuestion) throws QuestionNotFoundException {
        return examService.getQuestion(codeQuestion);
    }
    @PostMapping("/question")
    @PreAuthorize("isAuthenticated()")
    public QuestionDTO saveQuestion(@RequestBody QuestionDTO questionDTO, Authentication authentication) throws ExamNotFoundException {
        Long userId = securityUtils.getCurrentUserId(authentication);
        return examService.saveQuestionWithAnswers(questionDTO, userId);
    }
    @PostMapping("/questionAndAnswers")
    @PreAuthorize("isAuthenticated()")
    public QuestionDTO saveQuestionAndAnswers(@RequestBody QuestionDTO questionDTO, Authentication authentication) throws ExamNotFoundException {
        Long userId = securityUtils.getCurrentUserId(authentication);
        return examService.saveQuestionWithAnswers(questionDTO, userId);
    }

    @GetMapping("/answers")
    public List<AnswerDTO> getAllAnswers(){
        return examService.listAllAnswers();
    }

    @GetMapping("/answers/{codeQuestion}")
    public List<AnswerDTO> getAnswers(@PathVariable String codeQuestion) throws QuestionNotFoundException {
        return examService.listAnswers(codeQuestion);
    }

    @GetMapping("/{codeAnswer}")
    public AnswerDTO getAnswer(@PathVariable String codeAnswer) throws AnswerNotFoundException {
        return examService.getAnswer(codeAnswer);
    }

    @PostMapping("/answer")
    @PreAuthorize("isAuthenticated()")
    public AnswerDTO saveAnswer(@RequestBody AnswerDTO answerDTO, Authentication authentication) throws QuestionNotFoundException {
        Long userId = securityUtils.getCurrentUserId(authentication);
        return examService.saveAnswer(answerDTO, userId);
    }
    @PutMapping("/question/{codeQuestion}")
    @PreAuthorize("isAuthenticated()")
    public QuestionDTO updateQuestion(@PathVariable String codeQuestion,@RequestBody QuestionDTO questionDTO, Authentication authentication)
            throws QuestionNotFoundException {
        questionDTO.setCodeQuestion(codeQuestion);
        Long userId = securityUtils.getCurrentUserId(authentication);
        return examService.updateQuestion(questionDTO, userId);
    }
    @PutMapping("/questionAndAnswers/{codeQuestion}")
    @PreAuthorize("isAuthenticated()")
    public QuestionDTO updateQuestionWithAnswers(@PathVariable String codeQuestion,@RequestBody QuestionDTO questionDTO, Authentication authentication)
            throws QuestionNotFoundException {
        questionDTO.setCodeQuestion(codeQuestion);
        Long userId = securityUtils.getCurrentUserId(authentication);
        return examService.updateQuestionWithAnswers(questionDTO, userId);
    }
    @PutMapping("/answer/{codeAnswer}")
    @PreAuthorize("isAuthenticated()")
    public AnswerDTO updateAnswer(@PathVariable String codeAnswer,@RequestBody AnswerDTO answerDTO, Authentication authentication)
            throws AnswerNotFoundException, QuestionNotFoundException {

        answerDTO.setCodeAnswer(codeAnswer);
        Long userId = securityUtils.getCurrentUserId(authentication);
        return examService.updateAnswer(answerDTO, userId);
    }


    //New Update

    // ── Examens publics ───────────────────────────────────────────
    // GET /exams/public
    @GetMapping("/exams/public")
    public ResponseEntity<List<ExamDTO>> getPublicExams() {
        return ResponseEntity.ok(examService.getPublicExams());
    }


    // ── Changer la visibilité d'un examen ─────────────────────────
    // PATCH /exams/{codeExam}/visibility?visibility=PUBLIC
    // userId n'est plus lu depuis la requête : c'est l'utilisateur authentifié.
    @PatchMapping("/exams/{codeExam}/visibility")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExamDTO> updateVisibility(
            @PathVariable String codeExam,
            @RequestParam ExamVisibility visibility,
            Authentication authentication
    ) {
        Long userId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(examService.updateExamVisibility(codeExam, visibility, userId));
    }


    // ── Copier un examen public ───────────────────────────────────
    // POST /exams/copy
    @PostMapping("/exams/copy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExamDTO> copyPublicExam(@RequestBody CopyExamDTO dto, Authentication authentication) {
        dto.setUserId(securityUtils.getCurrentUserId(authentication));
        return ResponseEntity.ok(examService.copyPublicExam(dto));
    }

    // ── Partager un examen avec un groupe ─────────────────────────
    // POST /groups/share-exam — adminId dérivé du JWT, pas du body envoyé par le client.
    @PostMapping("/groups/share-exam")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupResponseDTO> shareExam(@RequestBody ShareExamWithGroupDTO dto, Authentication authentication) {
        dto.setAdminId(securityUtils.getCurrentUserId(authentication));
        return ResponseEntity.ok(examService.shareExamWithGroup(dto));
    }


    // ── Retirer un examen partagé d'un groupe ─────────────────────
    // DELETE /groups/share-exam
    @DeleteMapping("/groups/share-exam")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupResponseDTO> unshareExam(@RequestBody ShareExamWithGroupDTO dto, Authentication authentication) {
        dto.setAdminId(securityUtils.getCurrentUserId(authentication));
        return ResponseEntity.ok(examService.unshareExamFromGroup(dto));
    }


    // ── Examens partagés avec un groupe ───────────────────────────
    // GET /groups/{groupId}/exams
    @GetMapping("/groups/{groupId}/exams")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExamDTO>> getGroupSharedExams(
            @PathVariable Long groupId,
            Authentication authentication
    ) {
        Long userId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(examService.getSharedExamsForGroup(groupId, userId));
    }


    // ── Historique des tests d'un utilisateur pour un examen ──────
    // GET /exams/{codeExam}/my-tests
    @GetMapping("/exams/{codeExam}/my-tests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TestResultDTO>> getMyTestsForExam(
            @PathVariable String codeExam,
            Authentication authentication
    ) {
        Long userId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(examService.getUserTestsForExam(userId, codeExam));
    }

}
