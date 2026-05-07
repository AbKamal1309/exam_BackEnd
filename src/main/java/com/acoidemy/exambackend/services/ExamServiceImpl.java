package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.AnswerDTO;
import com.acoidemy.exambackend.dtos.ExamDTO;
import com.acoidemy.exambackend.dtos.QuestionDTO;
import com.acoidemy.exambackend.entities.Answer;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.entities.Exam;
import com.acoidemy.exambackend.entities.Question;
import com.acoidemy.exambackend.enums.ExamStatus;
import com.acoidemy.exambackend.exceptions.AnswerNotFoundException;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.QuestionNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.mappers.ExamMapperImpl;
import com.acoidemy.exambackend.repositories.AnswerRepository;
import com.acoidemy.exambackend.repositories.ExamRepository;
import com.acoidemy.exambackend.repositories.QuestionRepository;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public ExamDTO saveExam(ExamDTO examDTO) throws UserNotFoundException,ExamNotFoundException {
        log.info("Saving new Exam");

       Optional<AppUser> appUser= appUserRepository.findById(examDTO.getUserId());
       log.info(appUser.get().getName());
     //   examDTO.setDateCreation(new Date());
      //  Exam exam = dtoMapper.fromExamDTO(examDTO);
        Exam exam=new Exam();
     //   exam.setAppUser(dtoMapper.fromUserDTO(userService.getUser(examDTO.getUserId())));
        exam.setCodeExam(UUID.randomUUID().toString());
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
            question.setCodeQuestion(UUID.randomUUID().toString());
            question.setQuestionContent(dtoMapper.fromQuestionDTO(examDTO.getQuestionDTOList().get(i)).getQuestionContent());
        log.info(dtoMapper.fromQuestionDTO(examDTO.getQuestionDTOList().get(i)).getQuestionContent());
            question.setExam(savedExam);
            Question savedQuestion=questionRepository.save(question);
        log.info(savedQuestion.getQuestionContent()+" "+savedQuestion.getExam().getDescription());
            for (int j=0;j<4;j++){
                Answer answer=new Answer();
                answer.setCodeAnswer(UUID.randomUUID().toString());
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
    public ExamDTO updateExam(ExamDTO examDTO) throws ExamNotFoundException {
        log.info("Updating Exam");
        Exam exam = examRepository.findById(examDTO.getCodeExam())
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));
        exam.setDescription(examDTO.getDescription());
        exam.setNumberOfQuestions(exam.getNumberOfQuestions()+examDTO.getNumberOfQuestions());
        exam.setNumberOfQuestions(examDTO.getQuestionDTOList().size());
       // Exam updatedExam = examRepository.updateExamByCodeExam(exam.getCodeExam());
       Exam savedExam = examRepository.save(exam);


        return dtoMapper.fromExam(exam);
    }

    @Override
    public void deleteExam(String codeExam) {

    }

    @Override
    public QuestionDTO saveQuestion(QuestionDTO questionDTO) throws ExamNotFoundException {
        log.info("Saving new Question");

       Question question=dtoMapper.fromNewQuestionDTOAndAnswers(questionDTO);
       // Question question = dtoMapper.fromNewQuestionDTOWithoutAnswers(questionDTO);
       question.setCodeQuestion(UUID.randomUUID().toString());
        Exam exam = examRepository.findByCodeExam(questionDTO.getExamId());
        if (exam.getQuestions()==null){
            List<Question> questionList = new ArrayList<>();
            questionList.add(question);
            exam.setQuestions(questionList);
        }else {
            exam.getQuestions().add(question);
        }

        for (int i=0;i<questionDTO.getAnswers().size();i++){
            Answer answer=new Answer();
            answer.setCodeAnswer(UUID.randomUUID().toString());
            answer.setAnswerContent(questionDTO.getAnswers().get(i).getAnswerContent());
            answer.setAnswerStatus(questionDTO.getAnswers().get(i).getAnswerStatus());
            answer.setQuestion(question);
            answerRepository.save(answer);
        }


        question.setExam(exam);
        //question.setExam(dtoMapper.fromExamDTO(this.getExam(questionDTO.getExamId())));
        Question savedQuestion = questionRepository.save(question);
        return dtoMapper.fromNewQuestionAndAnswers(savedQuestion);
    }
    @Override
    public QuestionDTO saveQuestionWithAnswers(QuestionDTO questionDTO) throws ExamNotFoundException {
        log.info("Saving new Question And Answers  ");
        Question question=dtoMapper.fromNewQuestionDTOAndAnswers(questionDTO);
        question.setExam(dtoMapper.fromExamDTO(this.getExam(questionDTO.getExamId())));
        Question savedQuestion = questionRepository.save(question);
        List<Answer> answers=new ArrayList<>();
        for (int i=0;i<questionDTO.getAnswers().size();i++){
            Answer answer=new Answer();
            answer.setCodeAnswer(UUID.randomUUID().toString());
            answer.setAnswerContent(questionDTO.getAnswers().get(i).getAnswerContent());
            answer.setQuestion(savedQuestion);
            Answer savedAnswer = answerRepository.save(answer);
            answers.add(savedAnswer);
        }
        question.setAnswers(answers);

        return dtoMapper.fromNewQuestionAndAnswers(question);
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
    public QuestionDTO updateQuestion(QuestionDTO questionDTO) throws QuestionNotFoundException {
        log.info("Updating new Question");
        Question question = questionRepository.findById(questionDTO.getCodeQuestion())
                .orElseThrow(() -> new QuestionNotFoundException("Question Not Found"));
        question.setQuestionContent(questionDTO.getQuestionContent());
        question.setDescription(questionDTO.getDescription());


        Question savedQuestion = questionRepository.save(question);
        return dtoMapper.fromQuestion(savedQuestion);
    }

    @Override
    public void deleteQuestion(String codeQuestion) {

    }

    @Override
    public AnswerDTO saveAnswer(AnswerDTO answerDTO) throws QuestionNotFoundException {
        log.info("Saving new Answer");
        Answer answer = dtoMapper.fromAnswerDTO(answerDTO);
        answer.setCodeAnswer(UUID.randomUUID().toString());
        Question question = questionRepository.findById(answerDTO.getQuestionId())
                .orElseThrow(() -> new QuestionNotFoundException("question not found"));
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
    public AnswerDTO updateAnswer(AnswerDTO answerDTO) throws AnswerNotFoundException {
        log.info("Updating new Answer");

        Answer answer = answerRepository.findById(answerDTO.getCodeAnswer())
                .orElseThrow(() -> new AnswerNotFoundException("Answer Not Found"));
        answer.setAnswerContent(answerDTO.getAnswerContent());
        answer.setAnswerStatus(answerDTO.getAnswerStatus());
        answer.setDescription(answerDTO.getDescription());
        Answer savedAnswer = answerRepository.save(answer);
        return dtoMapper.fromAnswer(savedAnswer);
    }

    @Override
    public void deleteAnswer(String codeAnswer) {
        answerRepository.deleteById(codeAnswer);
    }

    @Override
    public QuestionDTO updateQuestionWithAnswers(QuestionDTO questionDTO) throws QuestionNotFoundException {
        log.info("Updating new Question And Answers");
        Question question = questionRepository.findById(questionDTO.getCodeQuestion())
                .orElseThrow(() -> new QuestionNotFoundException("Question Not Found"));
        question.setQuestionContent(questionDTO.getQuestionContent());
        for (int i=0;i<questionDTO.getAnswers().size();i++){
            question.getAnswers().get(i).setAnswerContent(questionDTO.getAnswers().get(i).getAnswerContent());
        }

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
        exam.setCodeExam(UUID.randomUUID().toString());
        exam.setDateCreation(new Date());
        exam.setNumberOfQuestions(examDTO.getQuestionDTOList().size());
        exam.setAppUser(dtoMapper.fromUserDTO(userService.getUser(examDTO.getUserId())));
        exam.setStatus(ExamStatus.CREATED);
        Exam savedExam = examRepository.save(exam);

        List<QuestionDTO>   questionDTOS=new ArrayList<>();
        for (int i=0;i<exam.getQuestions().size();i++){
            Question question=exam.getQuestions().get(i);
            question.setCodeQuestion(UUID.randomUUID().toString());
            question.setDateCreation(exam.getDateCreation());
            question.setExam(exam);
            Question savedQuestion = questionRepository.save(question);

            List<AnswerDTO> answers=new ArrayList<>();
            for (int j=0;j<4;j++){
                Answer answer=question.getAnswers().get(j);
                answer.setCodeAnswer(UUID.randomUUID().toString());
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


}
