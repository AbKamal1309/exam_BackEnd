package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.entities.*;
import com.acoidemy.exambackend.enums.ExamStatus;
import com.acoidemy.exambackend.enums.ExamVisibility;
import com.acoidemy.exambackend.exceptions.AnswerNotFoundException;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.QuestionNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.mappers.ExamMapperImpl;
import com.acoidemy.exambackend.repositories.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Console;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class ExamServiceImpl implements ExamService {

    private AppUserRepository appUserRepository;

    private ExamRepository examRepository;
    private QuestionRepository questionRepository;
    private AnswerRepository answerRepository;
    private ExamMapperImpl dtoMapper;
    @Autowired
    private AppUserService userService;
    private GroupRepository groupRepository;
    private TestExamRepository testExamRepository;

    @Override
    public ExamDTO saveExam(ExamDTO examDTO) throws UserNotFoundException,ExamNotFoundException {
        log.info("Saving new Exam");

       Optional<AppUser> appUser= appUserRepository.findById(examDTO.getUserId());
       log.info(appUser.get().getName());
     //   examDTO.setDateCreation(new Date());
      //  Exam exam = dtoMapper.fromExamDTO(examDTO);
        Exam exam=new Exam();
     //   exam.setAppUser(dtoMapper.fromUserDTO(userService.getUser(examDTO.getUserId())));
        exam.setCodeExam(UUID.randomUUID().toString().substring(0,8));
        exam.setStatus(ExamStatus.CREATED);
        exam.setDateCreation(new Date());
        exam.setNumberOfQuestions(examDTO.getQuestionDTOList().size());
        exam.setDescription(examDTO.getDescription());
        exam.setAppUser(appUser.get());


         Exam   savedExam = examRepository.save(exam);

        log.info("the code is : "+savedExam.getCodeExam());
        log.info("the number of questions is  : "+savedExam.getNumberOfQuestions());
        for (int i = 0; i< savedExam.getNumberOfQuestions(); i++) {
            Question question=new Question();
            question.setCodeQuestion(UUID.randomUUID().toString().substring(0,8));
            question.setQuestionContent(dtoMapper.fromQuestionDTO(examDTO.getQuestionDTOList().get(i)).getQuestionContent());
        log.info(dtoMapper.fromQuestionDTO(examDTO.getQuestionDTOList().get(i)).getQuestionContent());
            question.setExam(savedExam);
            Question savedQuestion=questionRepository.save(question);
        log.info(savedQuestion.getQuestionContent()+" "+savedQuestion.getExam().getDescription());
            for (int j=0;j<4;j++){
                Answer answer=new Answer();
                answer.setCodeAnswer(UUID.randomUUID().toString().substring(0,8));
                answer.setAnswerContent(examDTO.getQuestionDTOList().get(i).getAnswers().get(j).getAnswerContent());
             //   log.info(examDTO.getQuestionDTOList().get(i).getAnswers().get(j).getAnswerContent());
                answer.setAnswerStatus(examDTO.getQuestionDTOList().get(i).getAnswers().get(j).getAnswerStatus());
                answer.setQuestion(savedQuestion);
                Answer savedAnswer = answerRepository.save(answer);
                log.info(savedAnswer.getAnswerContent()+" from "+savedQuestion.getQuestionContent());
            }

        }

        log.info("Exam saved");
        Exam examRegistred = examRepository.findByCodeExam(savedExam.getCodeExam());
                //.orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));
        List<Question> questionList = questionRepository.findByExamCodeExam(examRegistred.getCodeExam());
        for (Question question : questionList){
            question.setAnswers(answerRepository.findByQuestion(question));
        }
        examRegistred.setQuestions(questionList);

        return dtoMapper.fromExam(examRegistred);
    }

    @Override
    public ExamDTO createExam(Long userId, ExamDTO dto) {
        AppUser user = findUser(userId);
        Exam exam = new Exam();
        exam.setCodeExam(UUID.randomUUID().toString().substring(0,8));
        exam.setDescription(dto.getDescription());
        exam.setDateCreation(new Date());
        exam.setStatus(dto.getStatus() != null ? dto.getStatus() : ExamStatus.CREATED);
        exam.setNumberOfQuestions(dto.getQuestionDTOList().size());
        exam.setVisibility(dto.getVisibility() != null ? dto.getVisibility() : ExamVisibility.PRIVATE);
        exam.setDurationMinutes(dto.getDurationMinutes());
        exam.setAppUser(user);
        Exam savedExam = examRepository.save(exam);

        for (int i = 0; i < savedExam.getNumberOfQuestions(); i++) {
            QuestionDTO questionDTO = dto.getQuestionDTOList().get(i);
            Question question = new Question();
            question.setCodeQuestion(UUID.randomUUID().toString().substring(0,8));
            question.setQuestionContent(questionDTO.getQuestionContent());
            question.setDescription(questionDTO.getDescription());
            question.setExam(savedExam);
            // ── AJOUT : pièce jointe ──
            question.setAttachmentUrl(questionDTO.getAttachmentUrl());
            question.setAttachmentType(questionDTO.getAttachmentType());
            question.setAttachmentName(questionDTO.getAttachmentName());

            Question savedQuestion = questionRepository.save(question);

            // ── CORRECTIF : boucle dynamique, plus de "j<4" codé en dur ──
            List<AnswerDTO> answersDTO = questionDTO.getAnswers();
            if (answersDTO != null) {
                for (AnswerDTO answerDTO : answersDTO) {
                    Answer answer = new Answer();
                    answer.setCodeAnswer(UUID.randomUUID().toString().substring(0,8));
                    answer.setAnswerContent(answerDTO.getAnswerContent());
                    answer.setAnswerStatus(answerDTO.getAnswerStatus());
                    answer.setDescription(answerDTO.getDescription());
                    answer.setQuestion(savedQuestion);
                    answerRepository.save(answer);
                }
            }
        }

        log.info("Exam saved");
        Exam examRegistred = examRepository.findByCodeExam(savedExam.getCodeExam());
        List<Question> questionList = questionRepository.findByExamCodeExam(examRegistred.getCodeExam());
        for (Question question : questionList) {
            question.setAnswers(answerRepository.findByQuestion(question));
        }
        examRegistred.setQuestions(questionList);

        return dtoMapper.fromExam(examRegistred);
    }
    @Override
    public List<ExamDTO> listExams() {
        List<Exam> exams = examRepository.findAll();
        List<ExamDTO> examDTOS = exams.stream().map(exam -> dtoMapper.fromExam(exam))
                .collect(Collectors.toList());
        return examDTOS;
    }

    @Override
    public ExamDTO getExam(String codeExam) throws ExamNotFoundException {
        Exam exam = examRepository.findById(codeExam)
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));

        return dtoMapper.fromExam(exam);
    }

    @Override
    public ExamDTO updateExam(ExamDTO examDTO, Long userId) throws ExamNotFoundException {
        log.info("Updating Exam with full questions/answers sync");
        Exam exam = examRepository.findById(examDTO.getCodeExam())
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));

        if (!exam.getAppUser().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres examens.");
        }

        exam.setDescription(examDTO.getDescription());
        if (examDTO.getStatus() != null) exam.setStatus(examDTO.getStatus());
        if (examDTO.getVisibility() != null) exam.setVisibility(examDTO.getVisibility());
        exam.setDurationMinutes(examDTO.getDurationMinutes());

        List<QuestionDTO> incomingQuestions = examDTO.getQuestionDTOList() != null
                ? examDTO.getQuestionDTOList() : new ArrayList<>();

        List<Question> existingQuestions = questionRepository.findByExamCodeExam(exam.getCodeExam());
        Map<String, Question> existingQuestionsByCode = existingQuestions.stream()
                .filter(q -> q.getCodeQuestion() != null)
                .collect(Collectors.toMap(Question::getCodeQuestion, q -> q));

        Set<String> incomingQuestionCodes = new HashSet<>();

        for (QuestionDTO qDto : incomingQuestions) {
            Question question;
            if (qDto.getCodeQuestion() != null && existingQuestionsByCode.containsKey(qDto.getCodeQuestion())) {
                // Question existante → mise à jour
                question = existingQuestionsByCode.get(qDto.getCodeQuestion());
                question.setQuestionContent(qDto.getQuestionContent());
                question.setDescription(qDto.getDescription());
                question.setAttachmentUrl(qDto.getAttachmentUrl());
                question.setAttachmentType(qDto.getAttachmentType());
                question.setAttachmentName(qDto.getAttachmentName());
            } else {
                // Nouvelle question ajoutée pendant l'édition
                question = new Question();
                question.setCodeQuestion(UUID.randomUUID().toString().substring(0, 8));
                question.setQuestionContent(qDto.getQuestionContent());
                question.setDescription(qDto.getDescription());
                question.setAttachmentUrl(qDto.getAttachmentUrl());
                question.setAttachmentType(qDto.getAttachmentType());
                question.setAttachmentName(qDto.getAttachmentName());
                question.setExam(exam);
            }

            incomingQuestionCodes.add(question.getCodeQuestion());
            Question savedQuestion = questionRepository.save(question);

            // ── Synchroniser les réponses de cette question ──
            List<AnswerDTO> incomingAnswers = qDto.getAnswers() != null ? qDto.getAnswers() : new ArrayList<>();
            List<Answer> existingAnswers = answerRepository.findByQuestion(savedQuestion);
            Map<String, Answer> existingAnswersByCode = existingAnswers.stream()
                    .filter(a -> a.getCodeAnswer() != null)
                    .collect(Collectors.toMap(Answer::getCodeAnswer, a -> a));

            Set<String> incomingAnswerCodes = new HashSet<>();

            for (AnswerDTO aDto : incomingAnswers) {
                Answer answer;
                if (aDto.getCodeAnswer() != null && existingAnswersByCode.containsKey(aDto.getCodeAnswer())) {
                    answer = existingAnswersByCode.get(aDto.getCodeAnswer());
                    answer.setAnswerContent(aDto.getAnswerContent());
                    answer.setAnswerStatus(aDto.getAnswerStatus());
                    answer.setDescription(aDto.getDescription());
                } else {
                    answer = new Answer();
                    answer.setCodeAnswer(UUID.randomUUID().toString().substring(0, 8));
                    answer.setAnswerContent(aDto.getAnswerContent());
                    answer.setAnswerStatus(aDto.getAnswerStatus());
                    answer.setDescription(aDto.getDescription());
                    answer.setQuestion(savedQuestion);
                }
                incomingAnswerCodes.add(answer.getCodeAnswer());
                answerRepository.save(answer);
            }

            // Supprimer les réponses retirées côté formulaire
            List<Answer> answersToDelete = existingAnswers.stream()
                    .filter(a -> !incomingAnswerCodes.contains(a.getCodeAnswer()))
                    .collect(Collectors.toList());
            answerRepository.deleteAll(answersToDelete);
        }

        // Supprimer les questions retirées côté formulaire (et leurs réponses)
        List<Question> questionsToDelete = existingQuestions.stream()
                .filter(q -> !incomingQuestionCodes.contains(q.getCodeQuestion()))
                .collect(Collectors.toList());
        for (Question q : questionsToDelete) {
            answerRepository.deleteAll(answerRepository.findByQuestion(q));
        }
        questionRepository.deleteAll(questionsToDelete);

        exam.setNumberOfQuestions(incomingQuestions.size());
        Exam savedExam = examRepository.save(exam);

        // Recharger complet pour la réponse
        Exam reloaded = examRepository.findByCodeExam(savedExam.getCodeExam());
        List<Question> finalQuestions = questionRepository.findByExamCodeExam(reloaded.getCodeExam());
        for (Question q : finalQuestions) {
            q.setAnswers(answerRepository.findByQuestion(q));
        }
        reloaded.setQuestions(finalQuestions);

        return dtoMapper.fromExam(reloaded);
    }
    @Override
    public void deleteExam(String codeExam, Long userId) throws ExamNotFoundException {
        Exam exam = examRepository.findById(codeExam)
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));

        if (!exam.getAppUser().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez supprimer que vos propres examens.");
        }

        log.info("Suppression de l'examen {}", codeExam);

        // 1. Supprimer les tests passés liés à cet examen
        List<TestExam> tests = testExamRepository.findByExamCodeExam(codeExam);
        testExamRepository.deleteAll(tests);

        // 2. Supprimer les questions et leurs réponses
        List<Question> questions = questionRepository.findByExamCodeExam(codeExam);
        for (Question question : questions) {
            List<Answer> answers = answerRepository.findByQuestion(question);
            answerRepository.deleteAll(answers);
        }
        questionRepository.deleteAll(questions);

        // 3. Supprimer l'examen lui-même
        examRepository.delete(exam);

        log.info("Examen {} supprimé avec succès", codeExam);
    }

    @Override
    public void adminDeleteExam(String codeExam) throws ExamNotFoundException {
        Exam exam = examRepository.findById(codeExam)
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));

        log.info("[ADMIN] Suppression de l'examen {}", codeExam);

        List<TestExam> tests = testExamRepository.findByExamCodeExam(codeExam);
        testExamRepository.deleteAll(tests);

        List<Question> questions = questionRepository.findByExamCodeExam(codeExam);
        for (Question question : questions) {
            List<Answer> answers = answerRepository.findByQuestion(question);
            answerRepository.deleteAll(answers);
        }
        questionRepository.deleteAll(questions);

        examRepository.delete(exam);

        log.info("[ADMIN] Examen {} supprimé avec succès", codeExam);
    }

    @Override
    public QuestionDTO saveQuestion(QuestionDTO questionDTO, Long userId) throws ExamNotFoundException {
        log.info("Saving new Question");

       Question question=dtoMapper.fromQuestionDTOWithAnswers(questionDTO);
       // Question question = dtoMapper.fromNewQuestionDTOWithoutAnswers(questionDTO);
       question.setCodeQuestion(UUID.randomUUID().toString().substring(0,8));
        Exam exam = examRepository.findByCodeExam(questionDTO.getExamId());
        if (exam == null) {
            throw new ExamNotFoundException("Exam Not Found");
        }
        if (!exam.getAppUser().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez ajouter des questions qu'à vos propres examens.");
        }
        if (exam.getQuestions()==null){
            List<Question> questionList = new ArrayList<>();
            questionList.add(question);
            exam.setQuestions(questionList);
        }else {
            exam.getQuestions().add(question);
        }

        for (int i=0;i<questionDTO.getAnswers().size();i++){
            Answer answer=new Answer();
            answer.setCodeAnswer(UUID.randomUUID().toString().substring(0,8));
            answer.setAnswerContent(questionDTO.getAnswers().get(i).getAnswerContent());
            answer.setAnswerStatus(questionDTO.getAnswers().get(i).getAnswerStatus());
            answer.setQuestion(question);
            answerRepository.save(answer);
        }


        question.setExam(exam);
        //question.setExam(dtoMapper.fromExamDTO(this.getExam(questionDTO.getExamId())));
        Question savedQuestion = questionRepository.save(question);
        return dtoMapper.fromQuestion (savedQuestion);
    }
    @Override
    public QuestionDTO saveQuestionWithAnswers(QuestionDTO questionDTO, Long userId) throws ExamNotFoundException {
        log.info("Saving new Question And Answers  ");
        ExamDTO examDTO = this.getExam(questionDTO.getExamId());
        if (examDTO.getUserId() == null || !examDTO.getUserId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez ajouter des questions qu'à vos propres examens.");
        }
        Question question=dtoMapper.fromQuestionDTOWithAnswers(questionDTO);
        question.setExam(dtoMapper.fromExamDTO(examDTO));
        Question savedQuestion = questionRepository.save(question);
        List<Answer> answers=new ArrayList<>();
        for (int i=0;i<questionDTO.getAnswers().size();i++){
            Answer answer=new Answer();
            answer.setCodeAnswer(UUID.randomUUID().toString().substring(0,8));
            answer.setAnswerContent(questionDTO.getAnswers().get(i).getAnswerContent());
            answer.setQuestion(savedQuestion);
            Answer savedAnswer = answerRepository.save(answer);
            answers.add(savedAnswer);
        }
        question.setAnswers(answers);

        return dtoMapper.fromQuestion (question);
    }

    @Override
    public List<QuestionDTO> listAllQuestions() {
        List<Question> questions = questionRepository.findAll();
        List<QuestionDTO> questionDTOS = questions.stream().map(question -> dtoMapper.fromQuestion(question))
                .collect(Collectors.toList());
        return questionDTOS;
    }

    @Override
    public List<QuestionDTO> listQuestions(String codeExam) throws ExamNotFoundException {
      Exam exam = examRepository.findById(codeExam)
               .orElseThrow(()->new ExamNotFoundException("Exam not Found"));
        List<Question> questions = exam.getQuestions();
        List<QuestionDTO> questionDTOS = questions.stream()
                .map(question -> dtoMapper.fromQuestion(question))
                .collect(Collectors.toList());


        return questionDTOS;
    }

    @Override
    public QuestionDTO getQuestion(String codeQuestion) throws QuestionNotFoundException {
        Question question = questionRepository.findById(codeQuestion)
                .orElseThrow(() -> new QuestionNotFoundException("Question Not Found"));
        return dtoMapper.fromQuestion(question);
    }

    @Override
    public QuestionDTO updateQuestion(QuestionDTO questionDTO, Long userId) throws QuestionNotFoundException {
        log.info("Updating new Question");
        Question question = questionRepository.findById(questionDTO.getCodeQuestion())
                .orElseThrow(() -> new QuestionNotFoundException("Question Not Found"));

        if (question.getExam() == null || !question.getExam().getAppUser().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez modifier que les questions de vos propres examens.");
        }

        dtoMapper.updateQuestionFromDTO(question, questionDTO); // ← copie aussi la pièce jointe


        Question savedQuestion = questionRepository.save(question);
        return dtoMapper.fromQuestion(savedQuestion);
    }

    @Override
    public void deleteQuestion(String codeQuestion, Long userId) {
        Question question = questionRepository.findById(codeQuestion)
                .orElseThrow(() -> new RuntimeException("Question introuvable : " + codeQuestion));

        if (question.getExam() == null || !question.getExam().getAppUser().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez supprimer que les questions de vos propres examens.");
        }

        answerRepository.deleteAll(answerRepository.findByQuestion(question));
        questionRepository.delete(question);
    }

    @Override
    public AnswerDTO saveAnswer(AnswerDTO answerDTO, Long userId) throws QuestionNotFoundException {
        log.info("Saving new Answer");
        Question question = questionRepository.findById(answerDTO.getQuestionId())
                .orElseThrow(() -> new QuestionNotFoundException("question not found"));

        if (question.getExam() == null || !question.getExam().getAppUser().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez ajouter des réponses qu'aux questions de vos propres examens.");
        }

        Answer answer = dtoMapper.fromAnswerDTO(answerDTO);
        answer.setCodeAnswer(UUID.randomUUID().toString().substring(0,8));
        answer.setQuestion(question);
        Answer savedAnswer = answerRepository.save(answer);
        return dtoMapper.fromAnswer(savedAnswer);
    }

    @Override
    public List<AnswerDTO> listAllAnswers() {
        List<Answer> answers = answerRepository.findAll();
        List<AnswerDTO> answerDTOS = answers.stream().map(answer -> dtoMapper.fromAnswer(answer))
                .collect(Collectors.toList());
        return answerDTOS;
    }

    @Override
    public List<AnswerDTO> listAnswers(String codeQuestion) throws QuestionNotFoundException {
        Question question = questionRepository.findById(codeQuestion)
                .orElseThrow(()->new QuestionNotFoundException("Question Not Found"));
        List<Answer> answers = question.getAnswers();
        List<AnswerDTO> answerDTOS = answers.stream().map(answer -> dtoMapper.fromAnswer(answer))
                .collect(Collectors.toList());
        return answerDTOS;
    }

    @Override
    public AnswerDTO getAnswer(String codeAnswer) throws AnswerNotFoundException {
        Answer answer = answerRepository.findById(codeAnswer)
                .orElseThrow(() -> new AnswerNotFoundException("Answer Not Found"));
        return dtoMapper.fromAnswer(answer);
    }

    @Override
    public AnswerDTO updateAnswer(AnswerDTO answerDTO, Long userId) throws AnswerNotFoundException {
        log.info("Updating new Answer");

        Answer answer = answerRepository.findById(answerDTO.getCodeAnswer())
                .orElseThrow(() -> new AnswerNotFoundException("Answer Not Found"));

        Question question = answer.getQuestion();
        if (question == null || question.getExam() == null || !question.getExam().getAppUser().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez modifier que les réponses de vos propres examens.");
        }

        answer.setAnswerContent(answerDTO.getAnswerContent());
        answer.setAnswerStatus(answerDTO.getAnswerStatus());
        answer.setDescription(answerDTO.getDescription());
        Answer savedAnswer = answerRepository.save(answer);
        return dtoMapper.fromAnswer(savedAnswer);
    }

    @Override
    public void deleteAnswer(String codeAnswer, Long userId) {
        Answer answer = answerRepository.findById(codeAnswer)
                .orElseThrow(() -> new RuntimeException("Réponse introuvable : " + codeAnswer));

        Question question = answer.getQuestion();
        if (question == null || question.getExam() == null || !question.getExam().getAppUser().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez supprimer que les réponses de vos propres examens.");
        }

        answerRepository.deleteById(codeAnswer);
    }

    @Override
    public QuestionDTO updateQuestionWithAnswers(QuestionDTO questionDTO, Long userId) throws QuestionNotFoundException {
        log.info("Updating new Question And Answers");
        Question question = questionRepository.findById(questionDTO.getCodeQuestion())
                .orElseThrow(() -> new QuestionNotFoundException("Question Not Found"));

        if (question.getExam() == null || !question.getExam().getAppUser().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez modifier que les questions de vos propres examens.");
        }

        dtoMapper.updateQuestionFromDTO(question, questionDTO); // ← contenu + description + pièce jointe + réponses


        Question savedQuestion = questionRepository.save(question);
        return dtoMapper.fromQuestion(savedQuestion);
    }

    @Override
    public List<ExamDTO> listExamsByUser(Long userId) throws UserNotFoundException {
        AppUser appUser = appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("AppUser Not Found"));
        List<Exam> exams = appUser.getExams();
        List<ExamDTO> examDTOS=new ArrayList<>();
        for (int i=0;i<exams.size();i++){
            examDTOS.add(dtoMapper.fromExam(exams.get(i)));

        }
        return examDTOS;
    }

    @Override
    public ExamDTO saveExamAllQuestionsAndAnswers(ExamDTO examDTO) throws UserNotFoundException {

        log.info("Saving New Exam With All Questions And Answers");
        Exam exam=dtoMapper.fromExamAllQuestionsAndAnswersDTO(examDTO);
        exam.setCodeExam(UUID.randomUUID().toString().substring(0,8));
        exam.setDateCreation(new Date());
        exam.setNumberOfQuestions(examDTO.getQuestionDTOList().size());
        exam.setAppUser(dtoMapper.fromUserDTO(userService.getUser(examDTO.getUserId())));
        exam.setStatus(ExamStatus.CREATED);
        Exam savedExam = examRepository.save(exam);

        List<QuestionDTO>   questionDTOS=new ArrayList<>();
        for (int i=0;i<exam.getQuestions().size();i++){
            Question question=exam.getQuestions().get(i);
            question.setCodeQuestion(UUID.randomUUID().toString().substring(0,8));
            question.setDateCreation(exam.getDateCreation());
            question.setExam(exam);
            Question savedQuestion = questionRepository.save(question);

            List<AnswerDTO> answers=new ArrayList<>();
            for (int j=0;j<question.getAnswers().size();j++){
                Answer answer=question.getAnswers().get(j);
                answer.setCodeAnswer(UUID.randomUUID().toString().substring(0,8));
                answer.setQuestion(savedQuestion);
                Answer savedAnswer = answerRepository.save(answer);
                answers.add(dtoMapper.fromAnswer(savedAnswer));
            }
            QuestionDTO questionDTO=dtoMapper.fromQuestion(savedQuestion);
            questionDTO.setAnswers(answers);
            questionDTOS.add(questionDTO);

        }


        ExamDTO examDTO1=new ExamDTO();
        examDTO1.setCodeExam(savedExam.getCodeExam());
        examDTO1.setDescription(savedExam.getDescription());
        examDTO1.setNumberOfQuestions(savedExam.getNumberOfQuestions());
        examDTO1.setUserId(savedExam.getAppUser().getId());
        examDTO1.setDateCreation(savedExam.getDateCreation());
        examDTO1.setStatus(savedExam.getStatus());
        examDTO1.setQuestionDTOList(questionDTOS);


       return examDTO1;
    }



    @Override
    public ExamDTO updateExamVisibility(String codeExam, ExamVisibility visibility, Long userId) {
        Exam exam = findExam(codeExam);
        if (!exam.getAppUser().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres examens.");
        }
        exam.setVisibility(visibility);
        return toDTO(examRepository.save(exam));

    }

    @Override
    public ExamDTO copyPublicExam(CopyExamDTO dto) {
        Exam original = findExam(dto.getExamCode());
        AppUser user  = findUser(dto.getUserId());

        if (!original.isPublic()) {
            throw new RuntimeException("Seuls les examens PUBLIC peuvent être copiés.");
        }

        // Créer une copie
        Exam copy = new Exam();
        copy.setCodeExam(UUID.randomUUID().toString());
        copy.setDescription(dto.getNewDescription() != null
                ? dto.getNewDescription()
                : "[Copie] " + original.getDescription());
        copy.setStatus(ExamStatus.CREATED);
        copy.setVisibility(ExamVisibility.PRIVATE); // La copie est privée par défaut
        copy.setAppUser(user);
        copy.setOriginalExam(original); // Référence vers l'original

        // Copier les questions et réponses
        if (original.getQuestions() != null) {
            List<Question> copiedQuestions = original.getQuestions().stream().map(q -> {
                Question newQ = new Question();
                newQ.setCodeQuestion(UUID.randomUUID().toString());
                newQ.setQuestionContent(q.getQuestionContent());
                newQ.setDescription(q.getDescription());
                newQ.setExam(copy);
                // Copier les réponses
                if (q.getAnswers() != null) {
                    List<Answer> copiedAnswers = q.getAnswers().stream().map(a -> {
                        Answer newA = new Answer();
                        newA.setCodeAnswer(UUID.randomUUID().toString());
                        newA.setAnswerContent(a.getAnswerContent());
                        newA.setAnswerStatus(a.getAnswerStatus());
                        newA.setDescription(a.getDescription());
                        newA.setQuestion(newQ);
                        return newA;
                    }).collect(Collectors.toList());
                    newQ.setAnswers(copiedAnswers);
                }
                return newQ;
            }).collect(Collectors.toList());
            copy.setQuestions(copiedQuestions);
        }

        return toDTO(examRepository.save(copy));
    }

    @Override
    public GroupResponseDTO shareExamWithGroup(ShareExamWithGroupDTO dto) {
        Group group = findGroup(dto.getGroupId());
        Exam    exam  = findExam(dto.getExamCode());
        AppUser admin = findUser(dto.getAdminId());

        group.shareExam(exam, admin); // Logique de validation dans Group.shareExam()
        groupRepository.save(group);

        return toGroupResponseDTO(group);

    }

    @Override
    public GroupResponseDTO unshareExamFromGroup(ShareExamWithGroupDTO dto) {
        Group   group = findGroup(dto.getGroupId());
        Exam    exam  = findExam(dto.getExamCode());
        AppUser admin = findUser(dto.getAdminId());

        group.unshareExam(exam, admin);
        groupRepository.save(group);
        return toGroupResponseDTO(group);

    }

    @Override
    public List<ExamDTO> getSharedExamsForGroup(Long groupId, Long userId) {
        Group   group = findGroup(groupId);
        AppUser user  = findUser(userId);

        if (!group.isMember(user)) {
            throw new RuntimeException("Vous n'êtes pas membre de ce groupe.");
        }
        return group.getSharedExams().stream()
                .map(this::toDTO).collect(Collectors.toList());

    }

    @Override
    public List<ExamDTO> getPublicExams() {
        return examRepository.findByVisibility(ExamVisibility.PUBLIC)
                .stream().map(this::toDTO).collect(Collectors.toList());

    }

    @Override
    public List<TestResultDTO> getUserTestsForExam(Long userId, String codeExam) {
        return testExamRepository.findByAppUserIdAndExamCodeExam(userId, codeExam)
                .stream().map(this::toTestResultDTO).collect(Collectors.toList());

    }

    private TestResultDTO toTestResultDTO(TestExam t) {
        return TestResultDTO.builder()
                .testId(t.getCodeTest())
                .examId(t.getExam() != null ? t.getExam().getCodeExam() : null)
                .userNameTest(t.getAppUser() != null ? t.getAppUser().getName() : null)
                .score(t.getScore())
                .scorePercentage(t.getScorePercentage())
                .totalQuestions(t.getTotalQuestions())
                .correctAnswers(t.getCorrectAnswers())
                .wrongAnswers(t.getWrongAnswers())
                .datePassed(t.getDatePassed())
                .build();
    }


    private GroupResponseDTO toGroupResponseDTO(Group g) {
        return GroupResponseDTO.builder()
                .id(g.getId())
                .name(g.getGroupName())
                .build();
    }

    private Group findGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Groupe introuvable : " + id));

    }


    private Exam findExam(String code) {
        return examRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("Examen introuvable : " + code));
    }

    private ExamDTO toDTO(Exam exam) {
        return ExamDTO.builder()
                .codeExam(exam.getCodeExam())
                .dateCreation(exam.getDateCreation())
                .numberOfQuestions(exam.getNumberOfQuestions())
                .status(exam.getStatus())
                .visibility(exam.getVisibility())
                .description(exam.getDescription())
                .durationMinutes(exam.getDurationMinutes())
                .userId(exam.getAppUser() != null ? exam.getAppUser().getId() : null)
                .originalExamId(exam.getOriginalExam() != null ? exam.getOriginalExam().getCodeExam() : null)
                .build();
    }

    private AppUser findUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));

    }


}
