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
import com.acoidemy.exambackend.security.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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

    private SecurityUtils securityUtils;

    private TestService testService;

    @Override
    public ResponseTestScoreDTO getScoreTest(RequestTestScoreDTO requestTestScore) throws TestNotFoundException, AnswerNotFoundException {
        TestExam testExam = testExamRepository.findById(requestTestScore.getTestId())
                .orElseThrow(() -> new TestNotFoundException("Test Not Found"));

        ResponseTestScoreDTO responseTestScoreDTO=new ResponseTestScoreDTO();

        responseTestScoreDTO.setTestId(requestTestScore.getTestId());
        // ── AJOUT : oublié jusqu'ici, donc le mobile ne pouvait jamais savoir
        // vers quel examen naviguer pour la page de correction.
        responseTestScoreDTO.setExamId(testExam.getExam().getCodeExam());
        responseTestScoreDTO.setUserName(testExam.getAppUser().getName());
        responseTestScoreDTO.setExamSetName(testExam.getExam().getAppUser().getName());
        responseTestScoreDTO.setScore(testExam.getScore());
        responseTestScoreDTO.setNumberOfQuestions(testExam.getExam().getNumberOfQuestions());
        // ── AJOUT : ces deux compteurs restaient à 0 côté mobile (ResultScreen).
        // testService.getScore(codeTest) référencé ici avant n'existe pas avec cette
        // signature — la seule surcharge disponible attend le TestSendDTO original
        // (les réponses soumises), qui n'est jamais persisté après sendTest(). En
        // revanche TestExam stocke déjà ces deux agrégats au moment de la soumission :
        // pas besoin de recalcul, juste les exposer.
        responseTestScoreDTO.setNumberOfSucceededQuestions(testExam.getCorrectAnswers());
        responseTestScoreDTO.setNumberOfFailedQuestions(testExam.getWrongAnswers());
        // failedQuestions (le détail question par question) resterait vide : le
        // détail des réponses choisies n'est pas persisté par sendTest() (pas
        // d'entité TestAnswer créée). Nécessiterait de sauvegarder les réponses
        // soumises pour être vraiment exploitable — dis-moi si tu veux ce chantier.



        return responseTestScoreDTO;
    }

    @Override
    public ResponseAllTestExam getAllTestExam(RequestAllTestExam requestAllTestExam, Authentication authentication) throws ExamNotFoundException {
        ResponseAllTestExam responseAllTestExam=new ResponseAllTestExam();
        List<TestResultDTO> testResultDTOList=new ArrayList<>();

        Exam exam = examRepository.findById(requestAllTestExam.getExamId())
                .orElseThrow(() -> new ExamNotFoundException("Exam Not Found"));

        // ── AJOUT : seul le créateur de l'examen (ou un admin) peut voir qui a
        // passé le test et avec quel score. Sans ce contrôle, n'importe quel
        // utilisateur connaissant un examId pouvait consulter les résultats de
        // tous les autres candidats.
        AppUser currentUser = securityUtils.getCurrentUser(authentication);
        boolean isCreator = exam.getAppUser() != null && exam.getAppUser().getId().equals(currentUser.getId());
        if (!isCreator && !securityUtils.isAdmin(authentication)) {
            throw new RuntimeException("Vous ne pouvez consulter les résultats des tests que pour vos propres examens.");
        }

        List<TestExam> testExamList = exam.getTestExams();
        for (int i=0;i<testExamList.size();i++){
            TestResultDTO testResultDTO=new TestResultDTO();

            testResultDTO.setTestId(testExamList.get(i).getCodeTest());
            testResultDTO.setExamId(exam.getCodeExam());
            testResultDTO.setUserNameTest(testExamList.get(i).getAppUser().getName());
            testResultDTO.setUserNameExamSetter(exam.getAppUser().getName());
            testResultDTO.setScore(testExamList.get(i).getScore());
            testResultDTO.setScorePercentage(testExamList.get(i).getScorePercentage());
            testResultDTO.setTotalQuestions(testExamList.get(i).getTotalQuestions());
            testResultDTO.setWrongAnswers(testExamList.get(i).getWrongAnswers());
            testResultDTO.setCorrectAnswers(testExamList.get(i).getCorrectAnswers());
            testResultDTO.setDatePassed(testExamList.get(i).getDatePassed());
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

            testResultDTO.setTestId(testExams.get(i).getCodeTest());
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
        List<TestExam> testExams = exam.getTestExams();

        Collections.sort(testExams, Comparator.comparing(TestExam::getScore)
                .thenComparing(TestExam::getDatePassed));
        Collections.reverse(testExams);
        bestScoreDTO.setBestScore(testExams.get(0).getScore());
        bestScoreDTO.setTestId(testExams.get(0).getCodeTest());
        bestScoreDTO.setName(testExams.get(0).getAppUser().getName());

        bestScoreDTO.setExamId(exam.getCodeExam());


        return bestScoreDTO;
    }

    @Override
    public String getBestUserScored(String examId) throws ExamNotFoundException {
        return this.getBestScoreForExam(examId).getName();
    }
}
