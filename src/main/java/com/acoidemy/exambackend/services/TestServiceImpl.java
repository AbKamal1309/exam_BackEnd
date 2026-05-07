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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
@Slf4j

public class TestServiceImpl implements TestService {
    private AppUserRepository appUserRepository;

    private QuestionRepository questionRepository;

    private ExamRepository examRepository;

    private TestExamRepository testExamRepository;

    private TestAnswerRepository testAnswerRepository;

    private AnswerRepository answerRepository;


    private ExamMapperImpl dtoMapper;


    @Override
    public TestExamDTO getTestExam(TestRequestDTO testRequestDTO)
            throws UserNotFoundException, ExamNotFoundException {
        TestExamDTO testExamDTO = new TestExamDTO();

        AppUser appUser = appUserRepository.findById(testRequestDTO.getUserId())
                .orElseThrow(() -> new UserNotFoundException("AppUser Not Found"));
        Exam exam = examRepository.findById(testRequestDTO.getCodeExam())
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));
        AppUser appUserExamSetter = appUserRepository.findById(exam.getAppUser().getId())
                .orElseThrow(() -> new UserNotFoundException("Can Not Found AppUser Exam Setter"));
        List<Question> questions = exam.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            for (int j = 0; j < questions.get(i).getAnswers().size(); j++) {
                questions.get(i).getAnswers().get(j).setAnswerStatus(AnswerStatus.WRONG);
            }
        }

        testExamDTO.setUserNameExamSetter(appUserExamSetter.getName());
        testExamDTO.setUserRequestName(appUser.getName());
        testExamDTO.setCodeExam(exam.getCodeExam());
        testExamDTO.setExamDTO(dtoMapper.fromExam(exam));


        return testExamDTO;
    }

    @Override
    public TestResultDTO sendTest(TestSendDTO testSendDTO)
            throws QuestionNotFoundException, UserNotFoundException, ExamNotFoundException, AnswerNotFoundException, TestNotFoundException {
        List<QuestionDTO> questionDTOS = testSendDTO.getQuestionDTOS();

        AppUser appUser = appUserRepository.findById(testSendDTO.getUserId())
                .orElseThrow(() -> new UserNotFoundException("AppUser Not Found"));
        Exam exam = examRepository.findById(testSendDTO.getCodeExam())
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));
        List<Question> questionList = questionDTOS.stream().map(questionDTO -> dtoMapper.fromQuestionDTO(questionDTO))
                .collect(Collectors.toList());
        List<TestAnswer> testAnswerList = new ArrayList<>();


        TestExam testExam = new TestExam(UUID.randomUUID().toString(), new Date(), 0
                , questionList, exam, appUser, testAnswerList);


        TestExam savedTest = testExamRepository.save(testExam);

        for (int i=0;i<questionDTOS.size();i++){
            for (int j=0;j<questionDTOS.get(i).getAnswers().size();j++){
                TestAnswer testAnswer=new TestAnswer();
                    testAnswer.setTestExam(savedTest);
                    testAnswer.setAnswerTestStatus(questionDTOS.get(i).getAnswers().get(j).getAnswerStatus());
                    testAnswer.setAnswer(dtoMapper.fromAnswerDTO(questionDTOS.get(i).getAnswers().get(j)));
                    testAnswer.setQuestion(dtoMapper.fromQuestionDTO(questionDTOS.get(i)));
                TestAnswer testAnswerSaved = testAnswerRepository.save(testAnswer);
                testAnswerList.add(testAnswerSaved);
            }
        }
        savedTest.setTestAnswer(testAnswerList);
        savedTest.setScore(this.getScore(savedTest.getTestExamId()).getScore());
        TestExam testSaved = testExamRepository.save(savedTest);
        log.info("test saving ");

        TestResultDTO testResultDTO = new TestResultDTO();
        testResultDTO.setTestId(testSaved.getTestExamId());
        testResultDTO.setExamId(exam.getCodeExam());
        testResultDTO.setUserNameTest(appUser.getName());
        testResultDTO.setUserNameExamSetter(exam.getAppUser().getName());
         testResultDTO.setScore(testSaved.getScore());

        return testResultDTO;
    }

    @Override
    public ScoreDTO getScore(String testId) throws TestNotFoundException, AnswerNotFoundException {
            ScoreDTO scoreDTO=new ScoreDTO();

        TestExam testExam=testExamRepository.findById(testId)
                .orElseThrow(()->new TestNotFoundException("Test Not Found"));

        List<Question> questions = testExam.getExam().getQuestions();
        List<TestAnswer> testAnswers = testExam.getTestAnswer();


        List<Question> listFailedQuestions=new ArrayList<>();
        int score=0;
        int totalScore=0;

        for (int i=0;i<testAnswers.size();i++){
            Answer answer = answerRepository.findById(testAnswers.get(i).getAnswer().getCodeAnswer())
                    .orElseThrow(() -> new AnswerNotFoundException("Answer Not Found"));
            if (testAnswers.get(i).getAnswerTestStatus()==answer.getAnswerStatus()){
                score++;
            }else {
                for (int j=0;j<questions.size();j++){
                    if (questions.get(j).getCodeQuestion()==answer.getQuestion().getCodeQuestion()){
                        questions.get(j).setAppreciatedPoint(0);
                        listFailedQuestions.add(questions.get(j));
                    }
                }
            }

        }
        for (int i=0;i<questions.size();i++){
            totalScore=totalScore+questions.get(i).getAppreciatedPoint();
        }

        for (int i=0;i<questions.size();i++){
           questions.get(i).setAppreciatedPoint(2);
        }

        scoreDTO.setTestId(testExam.getTestExamId());
        scoreDTO.setScore(totalScore);

        List<QuestionDTO> listFailedQuestionsDTO=new ArrayList<>();

        for (int k=0;k<listFailedQuestions.size();k++){
            listFailedQuestionsDTO.add(dtoMapper.fromQuestion(listFailedQuestions.get(k)));
        }
        scoreDTO.setFailedQuestions(listFailedQuestionsDTO);



        return scoreDTO;
    }


}
