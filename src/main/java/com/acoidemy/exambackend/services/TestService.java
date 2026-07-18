package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.*;
import com.acoidemy.exambackend.entities.Exam;
import com.acoidemy.exambackend.entities.TestExam;
import com.acoidemy.exambackend.exceptions.*;

public interface TestService {

    TestExamDTO getTestExam(TestRequestDTO testRequestDTO) throws UserNotFoundException, ExamNotFoundException;

    TestResultDTO sendTest(TestSendDTO testSendDTO) throws
            QuestionNotFoundException, UserNotFoundException, ExamNotFoundException, AnswerNotFoundException, TestNotFoundException;

    ScoreDTO getScore(TestSendDTO test, Exam exam) throws TestNotFoundException, ExamNotFoundException;
}
