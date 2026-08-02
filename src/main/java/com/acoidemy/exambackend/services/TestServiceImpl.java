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
import com.acoidemy.exambackend.utils.IdGenerator;

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
    private final com.acoidemy.exambackend.security.SecurityUtils securityUtils;

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
        int maxAttempts = exam.getMaxAttempts() != null ? exam.getMaxAttempts() : 3;
        long attempts = testExamRepository.countByAppUserIdAndExamCodeExam(
                testRequestDTO.getUserId(), testRequestDTO.getCodeExam());
        if (attempts >= maxAttempts) {
            throw new TooManyAttemptsException(
                    "Vous avez déjà utilisé vos " + maxAttempts + " tentatives pour cet examen.");
        }

        String examSetterName = exam.getAppUser() != null ? exam.getAppUser().getName() : "Unknown";

        TestExamDTO testExamDTO = new TestExamDTO();
        testExamDTO.setUserRequestName(appUser.getName());
        testExamDTO.setUserNameExamSetter(examSetterName);
        testExamDTO.setCodeExam(exam.getCodeExam());
        testExamDTO.setExamDTO(dtoMapper.fromExam(exam));

        // ── AJOUT : masquage des bonnes réponses pendant le test ──────────
        // multipleCorrectAllowed est calculé dans fromQuestion() à partir du vrai
        // answerStatus, AVANT ce masquage : l'info utile au mobile (cases à cocher
        // ou non) est donc préservée, mais on ne renvoie plus quelle réponse est
        // correcte. Le score est de toute façon recalculé côté serveur dans
        // sendTest() à partir des données en base, jamais à partir de ce qui est
        // soumis tel quel.
        if (testExamDTO.getExamDTO() != null && testExamDTO.getExamDTO().getQuestionDTOList() != null) {
            testExamDTO.getExamDTO().getQuestionDTOList().forEach(q -> {
                if (q.getAnswers() != null) {
                    q.getAnswers().forEach(a -> a.setAnswerStatus(null));
                }
            });
        }

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

        int maxAttempts = exam.getMaxAttempts() != null ? exam.getMaxAttempts() : 3;
        long attempts = testExamRepository.countByAppUserIdAndExamCodeExam(
                testSendDTO.getUserId(), testSendDTO.getCodeExam());
        if (attempts >= maxAttempts) {
            throw new TooManyAttemptsException("Limite de tentatives atteinte (" + maxAttempts + " maximum).");
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

        // Lookup rapide des entités Question par code, pour persister ensuite quelles
        // réponses précises l'utilisateur a choisies (nécessaire pour la correction,
        // voir plus bas — jusqu'ici sendTest() ne gardait aucune trace des réponses
        // soumises une fois le score calculé).
        Map<String, Question> questionEntityMap = new HashMap<>();
        for (Question q : exam.getQuestions()) {
            questionEntityMap.put(q.getCodeQuestion(), q);
        }
        List<TestAnswer> testAnswersToSave = new ArrayList<>();

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

            // Persister les réponses choisies par l'utilisateur pour cette question
            // (indépendamment du résultat) : nécessaire pour la page de correction.
            Question questionEntity = questionEntityMap.get(submittedQ.getCodeQuestion());
            if (questionEntity != null && questionEntity.getAnswers() != null && submittedAnswers != null) {
                for (AnswerDTO submittedAnswer : submittedAnswers) {
                    if (submittedAnswer.getAnswerStatus() != AnswerStatus.CORRECT) continue; // pas sélectionnée
                    questionEntity.getAnswers().stream()
                            .filter(a -> a.getCodeAnswer().equals(submittedAnswer.getCodeAnswer()))
                            .findFirst()
                            .ifPresent(chosen -> {
                                TestAnswer ta = new TestAnswer();
                                ta.setQuestion(questionEntity);
                                ta.setChosenAnswer(chosen);
                                ta.setCorrect(chosen.getAnswerStatus() == AnswerStatus.CORRECT);
                                testAnswersToSave.add(ta);
                            });
                }
            }
        }

        log.info("=== FINAL: {}/{} correct", correctCount, totalQuestions);

        // 6. Création et sauvegarde du test
        TestExam testExam = new TestExam();
        testExam.setCodeTest(IdGenerator.generate());
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

        // Lier et sauvegarder les réponses choisies, maintenant que le test a un id.
        for (TestAnswer ta : testAnswersToSave) {
            ta.setTestExam(savedTest);
        }
        testAnswerRepository.saveAll(testAnswersToSave);

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

    @Override
    public ExamCorrectionDTO getCorrection(String codeExam, org.springframework.security.core.Authentication authentication)
            throws ExamNotFoundException, UserNotFoundException {

        // userId résolu depuis le JWT, jamais depuis un paramètre client.
        Long userId = securityUtils.getCurrentUserId(authentication);

        Exam exam = examRepository.findById(codeExam)
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));

        int maxAttempts = exam.getMaxAttempts() != null ? exam.getMaxAttempts() : 3;
        long attempts = testExamRepository.countByAppUserIdAndExamCodeExam(userId, codeExam);

        // ── Cœur de la fonctionnalité demandée : la correction ne se dévoile
        // qu'une fois toutes les tentatives autorisées épuisées ──
        if (attempts < maxAttempts) {
            throw new RuntimeException(
                    "La correction ne sera disponible qu'après avoir épuisé vos " + maxAttempts +
                            " tentatives (" + attempts + "/" + maxAttempts + " utilisées).");
        }

        // On corrige sur la DERNIÈRE tentative : c'est la plus pertinente à revoir.
        List<TestExam> userTests = testExamRepository.findByAppUserIdAndExamCodeExam(userId, codeExam);
        TestExam lastAttempt = userTests.stream()
                .max(Comparator.comparing(TestExam::getDatePassed))
                .orElseThrow(() -> new RuntimeException("Aucune tentative trouvée pour cet examen."));

        // Réponses choisies lors de cette dernière tentative, groupées par question.
        Map<String, List<TestAnswer>> chosenByQuestion = new HashMap<>();
        for (TestAnswer ta : lastAttempt.getTestAnswers()) {
            if (ta.getQuestion() == null) continue;
            chosenByQuestion
                    .computeIfAbsent(ta.getQuestion().getCodeQuestion(), k -> new ArrayList<>())
                    .add(ta);
        }

        List<QuestionCorrectionDTO> questionCorrections = new ArrayList<>();
        for (Question q : exam.getQuestions()) {
            List<TestAnswer> chosen = chosenByQuestion.getOrDefault(q.getCodeQuestion(), Collections.emptyList());
            java.util.Set<String> chosenCodeAnswers = new java.util.HashSet<>();
            for (TestAnswer ta : chosen) {
                if (ta.getChosenAnswer() != null) chosenCodeAnswers.add(ta.getChosenAnswer().getCodeAnswer());
            }

            List<AnswerCorrectionDTO> answerCorrections = new ArrayList<>();
            if (q.getAnswers() != null) {
                for (Answer a : q.getAnswers()) {
                    answerCorrections.add(AnswerCorrectionDTO.builder()
                            .answerContent(a.getAnswerContent())
                            .actuallyCorrect(a.getAnswerStatus() == AnswerStatus.CORRECT)
                            .userSelected(chosenCodeAnswers.contains(a.getCodeAnswer()))
                            .build());
                }
            }

            boolean fullyCorrect = answerCorrections.stream()
                    .allMatch(ac -> ac.isActuallyCorrect() == ac.isUserSelected());

            questionCorrections.add(QuestionCorrectionDTO.builder()
                    .questionContent(q.getQuestionContent())
                    .description(q.getDescription())
                    .attachmentUrl(q.getAttachmentUrl())
                    .attachmentType(q.getAttachmentType() != null ? q.getAttachmentType().name() : null)
                    .attachmentName(q.getAttachmentName())
                    .answers(answerCorrections)
                    .fullyCorrect(fullyCorrect)
                    .build());
        }

        return ExamCorrectionDTO.builder()
                .codeExam(exam.getCodeExam())
                .examDescription(exam.getDescription())
                .attemptsUsed((int) attempts)
                .maxAttempts(maxAttempts)
                .questions(questionCorrections)
                .build();
    }
}