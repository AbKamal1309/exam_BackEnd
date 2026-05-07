package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.entities.Exam;
import com.acoidemy.exambackend.entities.TestExam;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.exceptions.AnswerNotFoundException;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.TestNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.repositories.ExamRepository;
import com.acoidemy.exambackend.repositories.TestExamRepository;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class DashBoardServiceImpl implements DashBoardService{

    private TestExamRepository testExamRepository;

    private ExamRepository examRepository;

    private AppUserRepository appUserRepository;


    private TestService testService;

    @Override
    public ResponseTestScoreDTO getScoreTest(RequestTestScoreDTO requestTestScore) throws TestNotFoundException, AnswerNotFoundException {
        TestExam testExam = testExamRepository.findById(requestTestScore.getTestId())
                .orElseThrow(() -> new TestNotFoundException("Test Not Found"));

        ResponseTestScoreDTO responseTestScoreDTO=new ResponseTestScoreDTO();

        responseTestScoreDTO.setTestId(requestTestScore.getTestId());
        responseTestScoreDTO.setUserName(testExam.getAppUser().getName());
        responseTestScoreDTO.setExamSetName(testExam.getExam().getAppUser().getName());
        responseTestScoreDTO.setScore(testExam.getScore());
        responseTestScoreDTO.setNumberOfQuestions(testExam.getExam().getNumberOfQuestions());
        responseTestScoreDTO.setNumberOfFailedQuestions(testService.getScore(testExam.getTestExamId())
                .getFailedQuestions().size());
        responseTestScoreDTO.setNumberOfSucceededQuestions(testExam.getExam().getNumberOfQuestions() - testService.getScore(testExam.getTestExamId()).getFailedQuestions().size());
        responseTestScoreDTO.setFailedQuestions(testService.getScore(testExam.getTestExamId()).getFailedQuestions());



        return responseTestScoreDTO;
    }

    @Override
    public ResponseAllTestExam getAllTestExam(RequestAllTestExam requestAllTestExam) throws ExamNotFoundException {
        ResponseAllTestExam responseAllTestExam=new ResponseAllTestExam();
        List<TestResultDTO> testResultDTOList=new ArrayList<>();

        Exam exam = examRepository.findById(requestAllTestExam.getExamId())
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));

        List<TestExam> testExamList = exam.getTestExam();
        for (int i=0;i<testExamList.size();i++){
            TestResultDTO testResultDTO=new TestResultDTO();

            testResultDTO.setTestId(testExamList.get(i).getTestExamId());
            testResultDTO.setExamId(exam.getCodeExam());
            testResultDTO.setUserNameTest(testExamList.get(i).getAppUser().getName());
            testResultDTO.setUserNameExamSetter(exam.getAppUser().getName());
            testResultDTO.setScore(testExamList.get(i).getScore());
            testResultDTOList.add(testResultDTO);
        }

        responseAllTestExam.setTestExamDTOList(testResultDTOList);

        return responseAllTestExam;
    }

    @Override
    public ResponseAllTestUser getAllTestUser(Long userId) throws UserNotFoundException {

     ResponseAllTestUser responseAllTestUser=new ResponseAllTestUser();
        List<TestResultDTO> testResultDTOList=new ArrayList<>();


        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("AppUser Not Found"));

        List<TestExam> testExams = appUser.getTestExams();

        for (int i=0;i<testExams.size();i++){
            TestResultDTO testResultDTO=new TestResultDTO();

            testResultDTO.setTestId(testExams.get(i).getTestExamId());
            testResultDTO.setExamId(testExams.get(i).getExam().getCodeExam());
            testResultDTO.setUserNameTest(appUser.getName());
            testResultDTO.setUserNameExamSetter(testExams.get(i).getExam().getAppUser().getName());
            testResultDTO.setScore(testExams.get(i).getScore());
            testResultDTOList.add(testResultDTO);
        }

        responseAllTestUser.setTestResultDTOList(testResultDTOList);

        return responseAllTestUser;
    }

    @Override
    public List<String> getMostPopularExam() {

        //List<ExamDTO> examDTOList=new ArrayList<>();
        List<String> codeMostPopularExamsList=new ArrayList<>();

        List<TestExam> testExams = testExamRepository.findAll();
        List<String> codeExamsList=new ArrayList<>();
        for (int i=0;i<testExams.size();i++){
            codeExamsList.add(testExams.get(i).getExam().getCodeExam());
        }
        int mostFrequency = Collections.frequency(codeExamsList, codeExamsList.get(0));
        codeMostPopularExamsList.add(codeExamsList.get(0));

        for (int i=1;i<codeExamsList.size();i++){
            int frequency = Collections.frequency(codeExamsList, codeExamsList.get(i));
            if (frequency==mostFrequency){
                codeMostPopularExamsList.add(codeExamsList.get(i));
            }else if (frequency > mostFrequency){
                Collections.replaceAll(codeMostPopularExamsList,codeMostPopularExamsList.get(0),codeExamsList.get(i));
                mostFrequency=frequency;
            }
        }



        return codeMostPopularExamsList.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public BestScoreDTO getBestScoreForExam(String examId) throws ExamNotFoundException {
        BestScoreDTO bestScoreDTO=new BestScoreDTO();

        Exam exam = examRepository.findById(examId).orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));
        List<TestExam> testExams = exam.getTestExam();

        Collections.sort(testExams, Comparator.comparing(TestExam::getScore)
                .thenComparing(TestExam::getTestStartingDate));
        Collections.reverse(testExams);
        bestScoreDTO.setBestScore(testExams.get(0).getScore());
        bestScoreDTO.setTestId(testExams.get(0).getTestExamId());
        bestScoreDTO.setName(testExams.get(0).getAppUser().getName());

        bestScoreDTO.setExamId(exam.getCodeExam());


        return bestScoreDTO;
    }

    @Override
    public String getBestUserScored(String examId) throws ExamNotFoundException {
        return this.getBestScoreForExam(examId).getName();
    }
}
