package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.entities.Exam;
import com.acoidemy.exambackend.entities.TestExam;
import com.acoidemy.exambackend.exceptions.*;
import org.springframework.security.core.Authentication;

public interface TestService {

    TestExamDTO getTestExam(TestRequestDTO testRequestDTO) throws UserNotFoundException, ExamNotFoundException;

    TestResultDTO sendTest(TestSendDTO testSendDTO) throws
            QuestionNotFoundException, UserNotFoundException, ExamNotFoundException, AnswerNotFoundException, TestNotFoundException;

    ScoreDTO getScore(TestSendDTO test, Exam exam) throws TestNotFoundException, ExamNotFoundException;

    // N'est renvoyé que si l'utilisateur (résolu via le JWT, jamais un id client) a
    // épuisé toutes ses tentatives autorisées pour cet examen — sinon RuntimeException.
    ExamCorrectionDTO getCorrection(String codeExam, Authentication authentication)
            throws ExamNotFoundException, UserNotFoundException;
}
