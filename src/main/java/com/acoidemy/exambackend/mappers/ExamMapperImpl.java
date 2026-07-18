package com.acoidemy.exambackend.mappers;

import com.acoidemy.exambackend.dtos.AnswerDTO;
import com.acoidemy.exambackend.dtos.ExamDTO;
import com.acoidemy.exambackend.dtos.QuestionDTO;
import com.acoidemy.exambackend.dtos.UserDTO;
import com.acoidemy.exambackend.entities.Answer;
import com.acoidemy.exambackend.entities.AppRole;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.entities.Exam;
import com.acoidemy.exambackend.entities.Question;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamMapperImpl {

    // ==================== USER MAPPING ====================

    public UserDTO fromUser(AppUser appUser) {
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(appUser, userDTO);
        if (appUser.getAppRoles() != null) {
            userDTO.setRoles(appUser.getAppRoles().stream()
                    .map(AppRole::getRoleName)
                    .collect(Collectors.toList()));
        }
        return userDTO;
    }

    public AppUser fromUserDTO(UserDTO userDTO) {
        AppUser appUser = new AppUser();
        BeanUtils.copyProperties(userDTO, appUser);
        return appUser;
    }

    // ==================== ANSWER MAPPING ====================

    /**
     * Entity Answer -> AnswerDTO
     * Mapping basé sur le codeAnswer
     */
    public AnswerDTO fromAnswer(Answer answer) {
        AnswerDTO answerDTO = new AnswerDTO();
        answerDTO.setCodeAnswer(answer.getCodeAnswer());
        answerDTO.setAnswerContent(answer.getAnswerContent());
        answerDTO.setDescription(answer.getDescription());
        answerDTO.setAnswerStatus(answer.getAnswerStatus());
        answerDTO.setQuestionId(answer.getQuestion() != null ? answer.getQuestion().getCodeQuestion() : null);
        return answerDTO;
    }

    /**
     * AnswerDTO -> Entity Answer
     * Mapping basé sur le codeAnswer
     */
    public Answer fromAnswerDTO(AnswerDTO answerDTO) {
        Answer answer = new Answer();
        answer.setCodeAnswer(answerDTO.getCodeAnswer());
        answer.setAnswerContent(answerDTO.getAnswerContent());
        answer.setDescription(answerDTO.getDescription());
        answer.setAnswerStatus(answerDTO.getAnswerStatus());
        return answer;
    }

    /**
     * Liste AnswerDTO -> Liste Entity Answer
     */
    public List<Answer> fromAnswerDTOList(List<AnswerDTO> answerDTOList) {
        if (answerDTOList == null) return new ArrayList<>();
        return answerDTOList.stream()
                .map(this::fromAnswerDTO)
                .collect(Collectors.toList());
    }

    /**
     * Liste Entity Answer -> Liste AnswerDTO
     */
    public List<AnswerDTO> fromAnswerList(List<Answer> answerList) {
        if (answerList == null) return new ArrayList<>();
        return answerList.stream()
                .map(this::fromAnswer)
                .collect(Collectors.toList());
    }

    // ==================== QUESTION MAPPING ====================

    /**
     * Entity Question -> QuestionDTO
     * Mapping basé sur codeQuestion, et pour les réponses sur codeAnswer
     */
    public QuestionDTO fromQuestion(Question question) {
        if (question == null) return null;

        QuestionDTO questionDTO = new QuestionDTO();
        questionDTO.setCodeQuestion(question.getCodeQuestion());
        questionDTO.setQuestionContent(question.getQuestionContent());
        questionDTO.setDescription(question.getDescription());
        // ── AJOUT : pièce jointe ──
        questionDTO.setAttachmentUrl(question.getAttachmentUrl());
        questionDTO.setAttachmentType(question.getAttachmentType());
        questionDTO.setAttachmentName(question.getAttachmentName());


        if (question.getExam() != null) {
            questionDTO.setExamId(question.getExam().getCodeExam());
        }

        // Mapper les réponses une par une avec leur codeAnswer
        if (question.getAnswers() != null) {
            List<AnswerDTO> answers = new ArrayList<>();
            for (Answer answer : question.getAnswers()) {
                answers.add(fromAnswer(answer));
            }
            questionDTO.setAnswers(answers);
        }

        return questionDTO;
    }

    /**
     * QuestionDTO -> Entity Question (sans les réponses)
     * Mapping basé sur codeQuestion
     */
    public Question fromQuestionDTOWithoutAnswers(QuestionDTO questionDTO) {
        if (questionDTO == null) return null;

        Question question = new Question();
        question.setCodeQuestion(questionDTO.getCodeQuestion());
        question.setQuestionContent(questionDTO.getQuestionContent());
        question.setDescription(questionDTO.getDescription());
        // ── AJOUT : pièce jointe ──
        question.setAttachmentUrl(questionDTO.getAttachmentUrl());
        question.setAttachmentType(questionDTO.getAttachmentType());
        question.setAttachmentName(questionDTO.getAttachmentName());


        return question;
    }

    /**
     * QuestionDTO -> Entity Question (avec les réponses)
     * Mapping basé sur codeQuestion et codeAnswer
     */
    public Question fromQuestionDTOWithAnswers(QuestionDTO questionDTO) {
        if (questionDTO == null) return null;

        Question question = new Question();
        question.setCodeQuestion(questionDTO.getCodeQuestion() != null ?
                questionDTO.getCodeQuestion() : UUID.randomUUID().toString().substring(0, 8));
        question.setQuestionContent(questionDTO.getQuestionContent());
        question.setDescription(questionDTO.getDescription());
        // ── AJOUT : pièce jointe ──
        question.setAttachmentUrl(questionDTO.getAttachmentUrl());
        question.setAttachmentType(questionDTO.getAttachmentType());
        question.setAttachmentName(questionDTO.getAttachmentName());


        // Mapper les réponses
        if (questionDTO.getAnswers() != null) {
            List<Answer> answers = new ArrayList<>();
            for (AnswerDTO answerDTO : questionDTO.getAnswers()) {
                Answer answer = fromAnswerDTO(answerDTO);
                answer.setQuestion(question);
                answers.add(answer);
            }
            question.setAnswers(answers);
        }

        return question;
    }

    /**
     * QuestionDTO -> Entity Question (mapping complet pour création)
     */
    public Question fromQuestionDTO(QuestionDTO questionDTO) {
        return fromQuestionDTOWithAnswers(questionDTO);
    }

    /**
     * Liste Entity Question -> Liste QuestionDTO
     */
    public List<QuestionDTO> fromQuestionList(List<Question> questionList) {
        if (questionList == null) return new ArrayList<>();
        return questionList.stream()
                .map(this::fromQuestion)
                .collect(Collectors.toList());
    }

    /**
     * Liste QuestionDTO -> Liste Entity Question
     */
    public List<Question> fromQuestionDTOList(List<QuestionDTO> questionDTOList) {
        if (questionDTOList == null) return new ArrayList<>();
        return questionDTOList.stream()
                .map(this::fromQuestionDTO)
                .collect(Collectors.toList());
    }

    // ==================== EXAM MAPPING ====================

    /**
     * Entity Exam -> ExamDTO (mapping simple)
     * Mapping basé sur les codes
     */
    public ExamDTO fromExam(Exam exam) {
        if (exam == null) return null;

        ExamDTO examDTO = new ExamDTO();
        BeanUtils.copyProperties(exam, examDTO);

        if (exam.getAppUser() != null) {
            examDTO.setUserId(exam.getAppUser().getId());
        }

        // Mapper les questions avec leurs réponses
        if (exam.getQuestions() != null) {
            List<QuestionDTO> questionDTOList = new ArrayList<>();
            for (Question question : exam.getQuestions()) {
                questionDTOList.add(fromQuestion(question));
            }
            examDTO.setQuestionDTOList(questionDTOList);
            examDTO.setNumberOfQuestions(questionDTOList.size());
        }

        return examDTO;
    }

    /**
     * Entity Exam -> ExamDTO (mapping complet avec toutes les questions et réponses)
     * Mapping basé sur les codes
     */
    public ExamDTO fromExamAllQuestionsAndAnswers(Exam exam) {
        return fromExam(exam);
    }

    /**
     * ExamDTO -> Entity Exam (mapping simple)
     * Mapping basé sur les codes
     */
    public Exam fromExamDTO(ExamDTO examDTO) {
        if (examDTO == null) return null;

        Exam exam = new Exam();
        BeanUtils.copyProperties(examDTO, exam);

        if (examDTO.getQuestionDTOList() != null) {
            List<Question> questions = new ArrayList<>();
            for (QuestionDTO questionDTO : examDTO.getQuestionDTOList()) {
                Question question = fromQuestionDTOWithoutAnswers(questionDTO);
                question.setExam(exam);
                questions.add(question);
            }
            exam.setQuestions(questions);
        }

        return exam;
    }

    /**
     * ExamDTO -> Entity Exam (mapping complet avec toutes les questions et réponses)
     * Utilisé pour la création d'examen avec questions et réponses
     * Mapping basé sur les codes
     */
    public Exam fromExamAllQuestionsAndAnswersDTO(ExamDTO examDTO) {
        if (examDTO == null) return null;

        Exam exam = new Exam();
        exam.setDescription(examDTO.getDescription());
        exam.setCodeExam(examDTO.getCodeExam());
        exam.setDateCreation(examDTO.getDateCreation());
        exam.setVisibility(examDTO.getVisibility());
        exam.setStatus(examDTO.getStatus());
        exam.setDurationMinutes(examDTO.getDurationMinutes());


        List<QuestionDTO> questionDTOList = examDTO.getQuestionDTOList();
        if (questionDTOList != null) {
            List<Question> questionList = new ArrayList<>();

            for (QuestionDTO questionDTO : questionDTOList) {
                Question question = new Question();
                question.setCodeQuestion(UUID.randomUUID().toString().substring(0, 8));
                question.setQuestionContent(questionDTO.getQuestionContent());
                question.setDescription(questionDTO.getDescription());
                question.setExam(exam);
                // ── AJOUT : pièce jointe ──
                question.setAttachmentUrl(questionDTO.getAttachmentUrl());
                question.setAttachmentType(questionDTO.getAttachmentType());
                question.setAttachmentName(questionDTO.getAttachmentName());


                // Mapper les réponses une par une avec leur codeAnswer
                List<AnswerDTO> answersDTO = questionDTO.getAnswers();
                if (answersDTO != null) {
                    List<Answer> answerList = new ArrayList<>();
                    for (AnswerDTO answerDTO : answersDTO) {
                        Answer answer = new Answer();
                        answer.setCodeAnswer(UUID.randomUUID().toString().substring(0, 8));
                        answer.setAnswerContent(answerDTO.getAnswerContent());
                        answer.setAnswerStatus(answerDTO.getAnswerStatus());
                        answer.setDescription(answerDTO.getDescription());
                        answer.setQuestion(question);
                        answerList.add(answer);
                    }
                    question.setAnswers(answerList);
                }

                questionList.add(question);
            }

            exam.setQuestions(questionList);
        }

        return exam;
    }

    /**
     * ExamDTO -> Entity Exam pour la soumission de test
     * Mapping basé sur les codes existants (ne crée pas de nouveaux codes)
     */
    public Exam fromExamDTOForTest(ExamDTO examDTO) {
        if (examDTO == null) return null;

        Exam exam = new Exam();
        exam.setCodeExam(examDTO.getCodeExam());
        exam.setDescription(examDTO.getDescription());
        exam.setDurationMinutes(examDTO.getDurationMinutes());

        List<QuestionDTO> questionDTOList = examDTO.getQuestionDTOList();
        if (questionDTOList != null) {
            List<Question> questions = new ArrayList<>();

            for (QuestionDTO questionDTO : questionDTOList) {
                Question question = new Question();
                question.setCodeQuestion(questionDTO.getCodeQuestion());
                question.setQuestionContent(questionDTO.getQuestionContent());
                question.setDescription(questionDTO.getDescription());
                question.setExam(exam);
                // ── AJOUT : pièce jointe ──
                question.setAttachmentUrl(questionDTO.getAttachmentUrl());
                question.setAttachmentType(questionDTO.getAttachmentType());
                question.setAttachmentName(questionDTO.getAttachmentName());



                // Mapper les réponses en utilisant leur codeAnswer existant
                List<AnswerDTO> answerDTOList = questionDTO.getAnswers();
                if (answerDTOList != null) {
                    List<Answer> answers = new ArrayList<>();
                    for (AnswerDTO answerDTO : answerDTOList) {
                        Answer answer = new Answer();
                        answer.setCodeAnswer(answerDTO.getCodeAnswer());
                        answer.setAnswerContent(answerDTO.getAnswerContent());
                        answer.setAnswerStatus(answerDTO.getAnswerStatus());
                        answer.setDescription(answerDTO.getDescription());
                        answer.setQuestion(question);
                        answers.add(answer);
                    }
                    question.setAnswers(answers);
                }

                questions.add(question);
            }

            exam.setQuestions(questions);
        }

        return exam;
    }

    // ==================== METHODES UTILITAIRES ====================

    /**
     * Met à jour une question existante avec les données d'un DTO
     */
    public void updateQuestionFromDTO(Question existingQuestion, QuestionDTO questionDTO) {
        if (existingQuestion == null || questionDTO == null) return;

        existingQuestion.setQuestionContent(questionDTO.getQuestionContent());
        existingQuestion.setDescription(questionDTO.getDescription());
        // ── AJOUT : pièce jointe ──
        existingQuestion.setAttachmentUrl(questionDTO.getAttachmentUrl());
        existingQuestion.setAttachmentType(questionDTO.getAttachmentType());
        existingQuestion.setAttachmentName(questionDTO.getAttachmentName());


        // Mettre à jour les réponses en utilisant le codeAnswer comme identifiant
        if (questionDTO.getAnswers() != null && existingQuestion.getAnswers() != null) {
            // Créer une map des réponses DTO par codeAnswer
            Map<String, AnswerDTO> dtoAnswerMap = questionDTO.getAnswers().stream()
                    .filter(a -> a.getCodeAnswer() != null)
                    .collect(Collectors.toMap(AnswerDTO::getCodeAnswer, a -> a));

            // Parcourir les réponses existantes et les mettre à jour
            for (Answer existingAnswer : existingQuestion.getAnswers()) {
                AnswerDTO dtoAnswer = dtoAnswerMap.get(existingAnswer.getCodeAnswer());
                if (dtoAnswer != null) {
                    existingAnswer.setAnswerContent(dtoAnswer.getAnswerContent());
                    existingAnswer.setAnswerStatus(dtoAnswer.getAnswerStatus());
                    existingAnswer.setDescription(dtoAnswer.getDescription());
                }
            }
        }
    }
}