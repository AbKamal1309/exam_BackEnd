package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.exceptions.AnswerNotFoundException;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.TestNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface DashBoardService {


    ResponseTestScoreDTO getScoreTest(RequestTestScoreDTO requestTestScore) throws TestNotFoundException, AnswerNotFoundException;

    // authentication : nécessaire pour vérifier que seul le créateur (ou un admin)
    // de l'examen peut consulter les résultats de TOUS les candidats.
    ResponseAllTestExam getAllTestExam(RequestAllTestExam requestAllTestExam, Authentication authentication) throws ExamNotFoundException;

    ResponseAllTestUser getAllTestUser(Long userId) throws UserNotFoundException;

    List<String> getMostPopularExam();

    BestScoreDTO getBestScoreForExam(String examId) throws ExamNotFoundException;

    String getBestUserScored(String examId) throws ExamNotFoundException;
}
