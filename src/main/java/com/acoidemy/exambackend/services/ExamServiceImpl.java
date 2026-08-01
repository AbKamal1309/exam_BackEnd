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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import com.acoidemy.exambackend.utils.IdGenerator;

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
    private com.acoidemy.exambackend.security.SecurityUtils securityUtils;

    @Override
    public ExamDTO saveExam(ExamDTO examDTO) throws UserNotFoundException, ExamNotFoundException {
        // saveExam() dupliquait la même logique que saveExamAllQuestionsAndAnswers()
        // avec sa propre implémentation manuelle (boucles + sauvegardes
        // individuelles, sans jamais synchroniser les collections bidirectionnelles
        // en mémoire — la vraie source des erreurs "orphan-removal" qu'on vient de
        // traquer). On délègue désormais à la version déjà correcte, testée et
        // cascade-safe, pour n'avoir plus qu'UN SEUL chemin de création d'examen à
        // maintenir, plutôt que deux implémentations parallèles qui divergent.
        return saveExamAllQuestionsAndAnswers(examDTO);
    }
    @Override
    public ExamDTO createExam(Long userId, ExamDTO dto) {
        // Même logique dupliquée que saveExam/saveExamAllQuestionsAndAnswers, avec
        // le même bug (question.setAnswers(...) sur une entité déjà gérée par la
        // transaction, incompatible avec cascade=ALL,orphanRemoval=true). On
        // délègue à l'implémentation déjà correcte plutôt que de maintenir une
        // troisième copie divergente.
        dto.setUserId(userId);
        try {
            return saveExamAllQuestionsAndAnswers(dto);
        } catch (UserNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    @Override
    public List<ExamDTO> listExams() {
        List<Exam> exams = examRepository.findAll();
        List<ExamDTO> examDTOS = exams.stream().map(exam -> dtoMapper.fromExam(exam))
                .collect(Collectors.toList());
        return examDTOS;
    }

    @Override
    public ExamDTO getExam(String codeExam, org.springframework.security.core.Authentication authentication) throws ExamNotFoundException {
        ExamDTO examDTO = getExamInternal(codeExam);

        // ── AJOUT : un examen complet (questions + réponses, answerStatus compris)
        // ne doit être visible que pour son créateur ou un admin. Sans ce contrôle,
        // n'importe quel utilisateur pouvait consulter tout l'énoncé — et même le
        // corrigé, answerStatus n'étant pas masqué ici comme il l'est sur /test —
        // avant même de commencer le test, ou sans jamais le passer.
        boolean allowed = false;
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                var currentUser = securityUtils.getCurrentUser(authentication);
                allowed = (examDTO.getUserId() != null && examDTO.getUserId().equals(currentUser.getId()))
                        || securityUtils.isAdmin(authentication);
            }
        } catch (Exception e) {
            // Authentification absente/invalide : on retombe sur le comportement le
            // plus restrictif (pas de questions), jamais une erreur 500 côté client.
            allowed = false;
        }

        if (!allowed) {
            examDTO.setQuestionDTOList(null);
        }

        return examDTO;
    }

    /**
     * Version interne, non filtrée : réservée aux traitements serveur qui ont
     * légitimement besoin du contenu complet (ex: vérifier le propriétaire d'un
     * examen avant d'y ajouter une question). Ne JAMAIS exposer son résultat
     * directement à un client sans passer par getExam(...) ci-dessus.
     */
    private ExamDTO getExamInternal(String codeExam) throws ExamNotFoundException {
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
                question.setCodeQuestion(IdGenerator.generate());
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
                    answer.setCodeAnswer(IdGenerator.generate());
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

        // Recharger pour la réponse — SANS jamais réassigner answers/questions
        // sur une entité encore gérée par la transaction (setAnswers()/
        // setQuestions() sur une relation cascade=ALL,orphanRemoval=true fait
        // perdre à Hibernate le suivi de la collection déjà "possédée",
        // provoquant "no longer referenced by the owning entity instance" au
        // flush). On construit directement les DTOs de réponse à la place.
        Exam reloaded = examRepository.findByCodeExam(savedExam.getCodeExam());
        List<Question> finalQuestions = questionRepository.findByExamCodeExam(reloaded.getCodeExam());

        List<QuestionDTO> finalQuestionDTOs = new ArrayList<>();
        for (Question q : finalQuestions) {
            QuestionDTO qDto = dtoMapper.fromQuestion(q);
            List<AnswerDTO> aDtos = new ArrayList<>();
            for (Answer a : answerRepository.findByQuestion(q)) {
                aDtos.add(dtoMapper.fromAnswer(a));
            }
            qDto.setAnswers(aDtos);
            finalQuestionDTOs.add(qDto);
        }

        ExamDTO resultDTO = dtoMapper.fromExam(reloaded);
        resultDTO.setQuestionDTOList(finalQuestionDTOs);
        return resultDTO;
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
       question.setCodeQuestion(IdGenerator.generate());
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
            answer.setCodeAnswer(IdGenerator.generate());
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
        ExamDTO examDTO = this.getExamInternal(questionDTO.getExamId());
        if (examDTO.getUserId() == null || !examDTO.getUserId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez ajouter des questions qu'à vos propres examens.");
        }
        Question question=dtoMapper.fromQuestionDTOWithAnswers(questionDTO);
        question.setExam(dtoMapper.fromExamDTO(examDTO));
        Question savedQuestion = questionRepository.save(question);
        List<Answer> answers=new ArrayList<>();
        for (int i=0;i<questionDTO.getAnswers().size();i++){
            Answer answer=new Answer();
            answer.setCodeAnswer(IdGenerator.generate());
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
        answer.setCodeAnswer(IdGenerator.generate());
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
        Exam exam = dtoMapper.fromExamAllQuestionsAndAnswersDTO(examDTO);
        exam.setCodeExam(IdGenerator.generate());
        exam.setDateCreation(new Date());
        exam.setNumberOfQuestions(examDTO.getQuestionDTOList().size());
        exam.setAppUser(dtoMapper.fromUserDTO(userService.getUser(examDTO.getUserId())));
        exam.setStatus(ExamStatus.CREATED);

        // La cascade (Exam.questions -> Question.answers, toutes deux en
        // CascadeType.ALL) persiste déjà l'examen ET ses questions ET leurs
        // réponses en un seul appel — leurs codeQuestion/codeAnswer sont déjà
        // renseignés par le mapper juste au-dessus.
        // ⚠️ NE JAMAIS refaire de save manuel après ça, et surtout ne JAMAIS
        // changer le codeQuestion/codeAnswer d'une entité déjà gérée par cette
        // transaction (Hibernate perd le fil de son identité, ce qui provoquait
        // des "Unable to find ... with id ..." aléatoires lors du flush).
        Exam savedExam = examRepository.save(exam);

        List<QuestionDTO> questionDTOS = new ArrayList<>();
        for (Question savedQuestion : savedExam.getQuestions()) {
            List<AnswerDTO> answers = new ArrayList<>();
            for (Answer savedAnswer : savedQuestion.getAnswers()) {
                answers.add(dtoMapper.fromAnswer(savedAnswer));
            }
            QuestionDTO questionDTO = dtoMapper.fromQuestion(savedQuestion);
            questionDTO.setAnswers(answers);
            questionDTOS.add(questionDTO);
        }

        ExamDTO examDTO1 = new ExamDTO();
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
    public List<GroupSharedExamDTO> getExamsSharedWithUserGroups(Long userId) {
        List<Group> myGroups = groupRepository.findGroupsByMemberId(userId);
        List<GroupSharedExamDTO> result = new ArrayList<>();
        for (Group g : myGroups) {
            try {
                for (Exam e : g.getSharedExams()) {
                    result.add(GroupSharedExamDTO.builder()
                            .examDTO(toDTO(e))
                            .groupId(g.getId())
                            .groupName(g.getGroupName())
                            .build());
                }
            } catch (RuntimeException ex) {
                // Une référence cassée dans group_shared_exams (examen supprimé,
                // ou jamais persisté avec succès malgré une tentative de partage)
                // ne doit jamais faire planter tout le chargement du Dashboard —
                // on journalise et on passe simplement ce groupe.
                log.warn("Impossible de charger les examens partagés du groupe {} ({}) : {}",
                        g.getId(), g.getGroupName(), ex.getMessage());
            }
        }
        return result;
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
                .numberOfTestsPassed(exam.getTestExams() != null ? exam.getTestExams().size() : 0)
                .build();
    }

    private AppUser findUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));

    }


}
