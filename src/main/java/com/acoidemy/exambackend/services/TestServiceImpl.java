package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.entities.*;
import com.acoidemy.exambackend.enums.AnswerStatus;
import com.acoidemy.exambackend.exceptions.*;
import com.acoidemy.exambackend.mappers.ExamMapperImpl;
import com.acoidemy.exambackend.repositories.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class TestServiceImpl implements TestService {

    private final AppUserRepository appUserRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final TestExamRepository testExamRepository;
    private final TestAnswerRepository testAnswerRepository;
    private final AnswerRepository answerRepository;
    private final ExamMapperImpl dtoMapper;
    private final TestSessionRepository testSessionRepository; // ── AJOUT ──

    @Override
    public TestExamDTO getTestExam(TestRequestDTO testRequestDTO)
            throws UserNotFoundException, ExamNotFoundException {

        log.info("=== GET TEST EXAM ===");
        log.info("UserId: {}, CodeExam: {}", testRequestDTO.getUserId(), testRequestDTO.getCodeExam());

        AppUser appUser = appUserRepository.findById(testRequestDTO.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User Not Found"));

        Exam exam = examRepository.findById(testRequestDTO.getCodeExam())
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));

        // Vérifier le nombre de tentatives déjà effectuées
        long attempts = testExamRepository.countByAppUserIdAndExamCodeExam(
                testRequestDTO.getUserId(), testRequestDTO.getCodeExam());
        if (attempts >= 3) {
            throw new TooManyAttemptsException("Vous avez déjà utilisé vos 3 tentatives pour cet examen.");
        }

        String examSetterName = exam.getAppUser() != null ? exam.getAppUser().getName() : "Unknown";

        TestExamDTO testExamDTO = new TestExamDTO();
        testExamDTO.setUserRequestName(appUser.getName());
        testExamDTO.setUserNameExamSetter(examSetterName);
        testExamDTO.setCodeExam(exam.getCodeExam());
        testExamDTO.setExamDTO(dtoMapper.fromExam(exam));

        // ── AJOUT : minuteur basé sur l'heure serveur ──────────────────
        if (exam.getDurationMinutes() != null && exam.getDurationMinutes() > 0) {
            TestSession session = testSessionRepository
                    .findByAppUserIdAndExamCodeExamAndSubmittedAtIsNull(
                            testRequestDTO.getUserId(), testRequestDTO.getCodeExam())
                    .orElseGet(() -> {
                        TestSession newSession = new TestSession();
                        newSession.setAppUser(appUser);
                        newSession.setExam(exam);
                        return testSessionRepository.save(newSession);
                    });

            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            testExamDTO.setTestStartTime(isoFormat.format(session.getStartTime()));
            testExamDTO.setServerTime(isoFormat.format(new Date()));

            log.info("Timer session -> start: {}, now: {}",
                    testExamDTO.getTestStartTime(), testExamDTO.getServerTime());
        }

        log.info("Test exam prepared successfully");
        return testExamDTO;
    }

    @Override
    public TestResultDTO sendTest(TestSendDTO testSendDTO)
            throws UserNotFoundException, ExamNotFoundException {

        log.info("=== SEND TEST ===");
        log.info("UserId: {}, CodeExam: {}", testSendDTO.getUserId(), testSendDTO.getCodeExam());

        // 1. Récupération de l'utilisateur
        AppUser appUser = appUserRepository.findById(testSendDTO.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User Not Found"));

        // 2. Récupération de l'examen
        Exam exam = examRepository.findById(testSendDTO.getCodeExam())
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));

        long attempts = testExamRepository.countByAppUserIdAndExamCodeExam(
                testSendDTO.getUserId(), testSendDTO.getCodeExam());
        if (attempts >= 3) {
            throw new TooManyAttemptsException("Limite de tentatives atteinte (3 maximum).");
        }

        // 3. Vérification des questions soumises
        List<QuestionDTO> submittedQuestions = testSendDTO.getQuestionDTOS();
        if (submittedQuestions == null || submittedQuestions.isEmpty()) {
            throw new RuntimeException("No questions submitted");
        }

        // 4. Construire une map : codeQuestion -> Map<codeAnswer, AnswerStatus> (depuis la BD)
        Map<String, Map<String, AnswerStatus>> storedAnswersMap = new HashMap<>();

        for (Question question : exam.getQuestions()) {
            Map<String, AnswerStatus> answersMap = new HashMap<>();
            if (question.getAnswers() != null) {
                for (Answer answer : question.getAnswers()) {
                    answersMap.put(answer.getCodeAnswer(), answer.getAnswerStatus());
                    log.info("Stored -> Question: {}, Answer: {}, Status: {}",
                            question.getCodeQuestion(), answer.getCodeAnswer(), answer.getAnswerStatus());
                }
            }
            storedAnswersMap.put(question.getCodeQuestion(), answersMap);
        }

        // 5. Comparer chaque question soumise avec les données de la BD
        int correctCount = 0;
        int totalQuestions = exam.getQuestions().size();

        for (QuestionDTO submittedQ : submittedQuestions) {
            log.info("--- Processing question: {} ---", submittedQ.getCodeQuestion());

            Map<String, AnswerStatus> storedAnswers = storedAnswersMap.get(submittedQ.getCodeQuestion());

            if (storedAnswers == null) {
                log.warn("Question {} not found in exam", submittedQ.getCodeQuestion());
                continue;
            }

            List<AnswerDTO> submittedAnswers = submittedQ.getAnswers();

            // Vérifier que le nombre de réponses soumises correspond à celui de la BD
            boolean sameSize = submittedAnswers != null &&
                    submittedAnswers.size() == storedAnswers.size();

            if (!sameSize) {
                log.info("Question {} WRONG - mismatched answer count (submitted={}, stored={})",
                        submittedQ.getCodeQuestion(),
                        submittedAnswers != null ? submittedAnswers.size() : 0,
                        storedAnswers.size());
                continue;
            }

            // Vérifier que chaque réponse soumise a le même status que celui stocké en BD
            boolean allMatch = submittedAnswers.stream().allMatch(submittedAnswer -> {
                AnswerStatus storedStatus = storedAnswers.get(submittedAnswer.getCodeAnswer());

                if (storedStatus == null) {
                    log.warn("Answer {} not found in BD for question {}",
                            submittedAnswer.getCodeAnswer(), submittedQ.getCodeQuestion());
                    return false;
                }

                boolean match = storedStatus == submittedAnswer.getAnswerStatus();
                log.info("Answer: {} | Submitted: {} | Stored: {} | Match: {}",
                        submittedAnswer.getCodeAnswer(),
                        submittedAnswer.getAnswerStatus(),
                        storedStatus.name(),
                        match);
                return match;
            });

            if (allMatch) {
                correctCount++;
                log.info("Question {} -> CORRECT ({}/{})", submittedQ.getCodeQuestion(), correctCount, totalQuestions);
            } else {
                log.info("Question {} -> WRONG", submittedQ.getCodeQuestion());
            }
        }

        log.info("=== FINAL: {}/{} correct", correctCount, totalQuestions);

        // 6. Création et sauvegarde du test
        TestExam testExam = new TestExam();
        testExam.setCodeTest(UUID.randomUUID().toString().substring(0, 8));
        testExam.setDatePassed(new Date());
        testExam.setTotalQuestions(totalQuestions);
        testExam.setAppUser(appUser);
        testExam.setExam(exam);
        testExam.setCorrectAnswers(correctCount);
        testExam.setWrongAnswers(totalQuestions - correctCount);
        testExam.setScore(correctCount);

        double percentage = totalQuestions > 0 ? ((double) correctCount / totalQuestions) * 100 : 0;
        testExam.setScorePercentage(Math.round(percentage * 100.0) / 100.0);

        TestExam savedTest = testExamRepository.save(testExam);
        log.info("Test saved - Score: {}/{} ({}%)", correctCount, totalQuestions, testExam.getScorePercentage());

        // ── AJOUT : clôturer la session de minuteur si elle existe ──────
        testSessionRepository
                .findByAppUserIdAndExamCodeExamAndSubmittedAtIsNull(
                        testSendDTO.getUserId(), testSendDTO.getCodeExam())
                .ifPresent(session -> {
                    session.setSubmittedAt(new Date());
                    testSessionRepository.save(session);
                    log.info("Session de test clôturée pour userId={}, codeExam={}",
                            testSendDTO.getUserId(), testSendDTO.getCodeExam());
                });

        // 7. Construction du DTO de résultat
        TestResultDTO result = new TestResultDTO();
        result.setTestId(savedTest.getCodeTest());
        result.setExamId(exam.getCodeExam());
        result.setUserNameTest(appUser.getName());
        result.setUserNameExamSetter(exam.getAppUser() != null ? exam.getAppUser().getName() : "Unknown");
        result.setScore(savedTest.getScore());
        result.setScorePercentage(savedTest.getScorePercentage());
        result.setTotalQuestions(savedTest.getTotalQuestions());
        result.setCorrectAnswers(savedTest.getCorrectAnswers());
        result.setWrongAnswers(savedTest.getWrongAnswers());
        result.setDatePassed(savedTest.getDatePassed());

        return result;
    }

    @Override
    public ScoreDTO getScore(TestSendDTO testSendDTO, Exam exam)
            throws TestNotFoundException, ExamNotFoundException {

        log.info("=== GET SCORE ===");
        log.info("Exam code: {}", exam.getCodeExam());

        List<QuestionDTO> submittedQuestions = testSendDTO.getQuestionDTOS();

        ScoreDTO scoreDTO = new ScoreDTO();
        scoreDTO.setScore(0);
        scoreDTO.setCorrectAnswers(0);
        scoreDTO.setWrongAnswers(0);
        scoreDTO.setScorePercentage(0.0);
        scoreDTO.setFailedQuestions(new ArrayList<>());

        if (submittedQuestions == null || submittedQuestions.isEmpty()) {
            log.warn("No submitted questions found");
            return scoreDTO;
        }

        // Construire la map des réponses correctes attendues
        Map<String, String> expectedCorrectAnswerMap = new HashMap<>();
        for (Question question : exam.getQuestions()) {
            if (question.getAnswers() != null) {
                for (Answer answer : question.getAnswers()) {
                    if (answer.getAnswerStatus() == AnswerStatus.CORRECT) {
                        expectedCorrectAnswerMap.put(question.getCodeQuestion(), answer.getCodeAnswer());
                        break;
                    }
                }
            }
        }

        int totalQuestions = exam.getQuestions().size();
        int correctCount = 0;
        List<QuestionDTO> failedQuestions = new ArrayList<>();

        for (QuestionDTO submittedQuestion : submittedQuestions) {
            String expectedCorrectCode = expectedCorrectAnswerMap.get(submittedQuestion.getCodeQuestion());

            // Trouver la réponse choisie par l'utilisateur
            String selectedCode = null;
            if (submittedQuestion.getAnswers() != null) {
                for (AnswerDTO answer : submittedQuestion.getAnswers()) {
                    if ("CORRECT".equals(answer.getAnswerStatus())) {
                        selectedCode = answer.getCodeAnswer();
                        break;
                    }
                }
            }

            boolean isCorrect = (selectedCode != null && selectedCode.equals(expectedCorrectCode));

            if (isCorrect) {
                correctCount++;
            } else {
                Question originalQuestion = questionRepository.findById(submittedQuestion.getCodeQuestion())
                        .orElse(null);
                if (originalQuestion != null) {
                    failedQuestions.add(dtoMapper.fromQuestion(originalQuestion));
                }
            }
        }

        double percentage = totalQuestions > 0 ? ((double) correctCount / totalQuestions) * 100 : 0;

        scoreDTO.setScore(correctCount);
        scoreDTO.setCorrectAnswers(correctCount);
        scoreDTO.setWrongAnswers(totalQuestions - correctCount);
        scoreDTO.setScorePercentage(Math.round(percentage * 100.0) / 100.0);
        scoreDTO.setFailedQuestions(failedQuestions);

        log.info("Score calculated: {}/{} ({}%)", correctCount, totalQuestions, scoreDTO.getScorePercentage());

        return scoreDTO;
    }
}